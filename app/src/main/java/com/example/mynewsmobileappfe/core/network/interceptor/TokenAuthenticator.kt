package com.example.mynewsmobileappfe.core.network.interceptor

import android.util.Log
import com.example.mynewsmobileappfe.core.jwt.TokenManager
import com.example.mynewsmobileappfe.feature.auth.data.remote.api.TokenRefreshApiService
import com.example.mynewsmobileappfe.feature.auth.data.remote.dto.TokenRefreshRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * OkHttp Authenticator
 *
 * ✅ 역할
 * - 서버가 401(Unauthorized)을 반환했을 때, Refresh Token 기반으로 토큰을 재발급(reissue) 시도합니다.
 * - 재발급에 성공하면 "원래 요청"을 새 Access Token으로 1회만 재시도합니다.
 *
 * ✅ 설계 포인트(실무 관점)
 * - 무한 루프 방지: 재시도 요청에 전용 헤더(HEADER_RETRIED)를 추가하여 401 → 재발급 → 401 → ... 루프를 차단
 * - 동시성 제어: 여러 요청이 동시에 401을 만나도 reissue API가 중복 호출되지 않도록 Mutex로 직렬화
 * - Refresh Token Rotation 지원: 재발급 성공 시 Access/Refresh 둘 다 교체 저장
 * - 강제 로그아웃 트리거: reissue 실패/예외 시 토큰 삭제 후 logoutEvent(SharedFlow)를 통해 앱 전역 로그아웃 유도
 *
 * ⚠️ 주의
 * - Authenticator는 OkHttp 네트워크 스레드에서 호출되므로 suspend를 직접 쓸 수 없어 runBlocking을 사용합니다.
 *   (대신 내부에서 Mutex로 lock 범위를 최소화해 부작용을 줄입니다.)
 */
class TokenAuthenticator(
    private val tokenManager: TokenManager,
    private val tokenRefreshApiService: TokenRefreshApiService
) : Authenticator {

    /**
     * 동시 401 발생 시 reissue 요청이 중복으로 나가지 않도록 보호하는 락
     * - 예: 동시에 10개 API가 401이면, 10번 reissue 호출되는 문제 방지
     * - 1개만 재발급하고, 나머지는 저장된 최신 토큰을 사용해 재시도하도록 유도
     */
    private val mutex = Mutex()

    /**
     * 토큰 재발급을 "시도하지 않는" 경로 목록
     *
     * 이유:
     * - 로그인/회원가입/reissue 자체가 401을 반환할 수 있으며,
     *   여기서 다시 reissue를 시도하면 루프/불필요한 네트워크 호출이 발생할 수 있음
     * - 특히 /reissue는 실패 시 즉시 로그아웃 처리하는 것이 일반적
     */
    private val noRetryPrefixes = listOf(
        "/api/v1/auth/login",
        "/api/v1/auth/signup",
        "/api/v1/auth/reissue"
    )

    companion object {
        private const val TAG = "TokenAuthenticator"

        /** Authorization 헤더 키 */
        private const val HEADER_AUTH = "Authorization"

        /**
         * 재시도 여부를 표시하는 내부 헤더
         * - "이 요청은 토큰 재발급 후 이미 한 번 재시도했다"는 마킹
         * - 서버로 보내도 무해하지만, 원하면 Interceptor에서 제거해도 됨
         */
        private const val HEADER_RETRIED = "Token-Refreshed"

        /** Bearer 토큰 접두사 */
        private const val BEARER = "Bearer "
    }

    /**
     * 401 응답을 받았을 때 OkHttp가 호출하는 콜백
     *
     * 반환값:
     * - Request 를 반환하면: OkHttp가 그 Request로 "자동 재시도"
     * - null 을 반환하면: 재시도하지 않고 현재 401을 호출자에게 전달
     */
    override fun authenticate(route: Route?, response: Response): Request? {
        val path = response.request.url.encodedPath

        // 인증 관련 API는 재발급 시도하지 않음
        if (noRetryPrefixes.any { path.startsWith(it) }) {
            Log.d(TAG, "인증 관련 API는 토큰 재발급을 시도하지 않습니다. path=$path")
            return null
        }

        // 이미 재시도한 요청이면 중단 (무한 루프 방지)
        if (response.request.header(HEADER_RETRIED) != null) {
            Log.w(TAG, "이미 토큰 재발급 후 재시도한 요청입니다. 재시도를 중단합니다.")
            return null
        }

        /**
         * Authenticator는 suspend 지원이 없어서 runBlocking 사용
         * - 내부에서 DataStore(Flow) 읽기, reissue 호출이 필요
         * - 블로킹이므로 반드시 "필요 최소 영역"만 수행하도록 구성
         */
        return runBlocking {
            mutex.withLock {
                // 현재 저장된 토큰을 읽는다 (없으면 재시도 불가)
                val tokens = readTokens() ?: return@runBlocking null

                // 동시성 상황 최적화:
                //    응답을 받은 요청에 실린 토큰이 현재 저장된 토큰과 다르면
                //    "다른 요청이 이미 재발급 성공 후 저장해둔 상태"일 가능성이 큼.
                //    => 굳이 reissue를 또 치지 말고 최신 accessToken으로만 재시도
                if (wasTokenUpdatedBySomeoneElse(response, tokens.accessToken)) {
                    Log.d(TAG, "다른 요청에서 이미 토큰이 갱신되었습니다. 최신 토큰으로 재시도합니다.")
                    return@runBlocking buildRetriedRequest(response.request, tokens.accessToken)
                }

                // reissue 호출 후, 성공하면 저장 + 원 요청 재시도 Request 생성
                reissueAndRetry(response, tokens.accessToken, tokens.refreshToken)
            }
        }
    }

    /** DataStore에서 꺼낸 토큰을 다루기 위한 내부 모델 */
    private data class Tokens(
        val accessToken: String,
        val refreshToken: String
    )

    /**
     * DataStore에서 access/refresh token 읽기
     * - 비어 있으면 재발급 시도 불가능
     */
    private suspend fun readTokens(): Tokens? {
        val accessToken = tokenManager.accessToken.first().orEmpty()
        val refreshToken = tokenManager.refreshToken.first().orEmpty()

        if (accessToken.isBlank() || refreshToken.isBlank()) {
            // 실무에서는 여기서 "로그아웃 유도"까지 할지, 그냥 null로 401 넘길지는 정책 선택
            Log.w(
                TAG,
                "저장된 토큰이 없습니다. accessToken=${accessToken.isNotBlank()}, refreshToken=${refreshToken.isNotBlank()}"
            )
            return null
        }
        return Tokens(accessToken, refreshToken)
    }

    /**
     * "이미 다른 요청이 토큰을 갱신했는지" 체크
     *
     * 원리:
     * - 401을 만든 원 요청의 Authorization 토큰 ≠ 현재 DataStore의 accessToken
     *   => 누군가 refresh 성공 후 accessToken을 교체 저장했을 가능성이 큼
     */
    private fun wasTokenUpdatedBySomeoneElse(response: Response, currentAccess: String): Boolean {
        val requestAccess = response.request.header(HEADER_AUTH)?.removePrefix(BEARER)
        return requestAccess != null && requestAccess != currentAccess
    }

    /**
     * 원 요청을 새 accessToken으로 1회 재시도하기 위한 Request 생성
     * - HEADER_RETRIED를 추가하여 다음 401에서 authenticate가 다시 호출되더라도 종료되게 함
     */
    private fun buildRetriedRequest(original: Request, newAccess: String): Request {
        Log.d(TAG, "새로운 AccessToken으로 요청을 재시도합니다.")
        return original.newBuilder()
            .header(HEADER_AUTH, "$BEARER$newAccess")
            .header(HEADER_RETRIED, "true")
            .build()
    }

    /**
     * reissue API 호출 + 저장 + 재시도 Request 생성
     *
     * 실패 정책(실무 일반):
     * - 401(Refresh 만료/무효) 또는 기타 실패 => 토큰 삭제 + 전역 로그아웃 이벤트 발행
     * - 네트워크 예외 등도 동일 처리(보안/일관성 관점)
     *
     * 필요하면 정책을 분리할 수도 있음:
     * - 네트워크 예외는 로그아웃 대신 "그냥 401 전달" 등
     */
    private suspend fun reissueAndRetry(response: Response, access: String, refresh: String): Request? {
        return try {
            Log.d(TAG, "AccessToken 만료 감지. 토큰 재발급을 시도합니다.")
            Log.d(TAG, "AccessToken: ${access.take(20)}...")
            Log.d(TAG, "RefreshToken: ${refresh.take(20)}...")

            // accessToken + refreshToken을 JSON body로 전송
            val reissueResponse = tokenRefreshApiService.refreshToken(
                TokenRefreshRequest(accessToken = access, refreshToken = refresh)
            )

            // HTTP 레벨 실패 처리
            if (!reissueResponse.isSuccessful) {
                Log.w(
                    TAG,
                    "토큰 재발급 실패 (HTTP ${reissueResponse.code()}). 응답: ${reissueResponse.errorBody()?.string()}"
                )
                Log.w(TAG, "강제 로그아웃 처리합니다.")
                tokenManager.clearTokens() // => logoutEvent 발행
                return null
            }

            // 바디 null 방어 (서버/네트워크/직렬화 이슈 가능)
            val body = reissueResponse.body()
            if (body == null) {
                Log.w(TAG, "토큰 재발급 응답 본문이 비어 있습니다. 강제 로그아웃 처리합니다.")
                tokenManager.clearTokens()
                return null
            }

            // Refresh Token Rotation: 둘 다 교체 저장
            tokenManager.saveTokens(body.accessToken, body.refreshToken)

            Log.i(TAG, "토큰 재발급 성공. 새로운 토큰으로 요청을 재시도합니다.")
            buildRetriedRequest(response.request, body.accessToken)

        } catch (e: Exception) {
            // 예: 타임아웃, DNS 실패, JSON 파싱 에러 등
            Log.e(
                TAG,
                "토큰 재발급 중 예외 발생. 네트워크 오류 또는 서버 오류로 판단하여 로그아웃 처리합니다.",
                e
            )
            tokenManager.clearTokens()
            null
        }
    }
}
