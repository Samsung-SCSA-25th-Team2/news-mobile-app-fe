package com.example.mynewsmobileappfe.core.network.interceptor

import android.util.Log
import com.example.mynewsmobileappfe.core.jwt.TokenManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Retrofit이 API 요청을 보낼 때 마다
 * DataStore에 저장된 Access Token을 읽어와 자동으로 Authorization 헤더에 추가
 *
 * 즉 로그인 후 모든 API 요청에 JWT를 붙이는 핵심 로직
 */
class AuthInterceptor(
    private val tokenManager: TokenManager
): Interceptor {

    companion object {
        private const val TAG = "AuthInterceptor"
        private const val HEADER_AUTH = "Authorization"
        private const val BEARER = "Bearer "
    }

    override fun intercept(chain: Interceptor.Chain): Response {

        val request = chain.request()
        val method = request.method

        /**
         * JWT가 필요 없는 요청들
         *
         * - 인증 관련 API (login, signup, reissue, logout)
         * - 뉴스 목록 조회 (비로그인 사용자도 접근 가능)
         * - 랜덤 기사 조회
         *
         * 👉 서버 정책과 1:1로 맞춰 관리하는 것이 중요
         */
        val path = request.url.encodedPath
        val isGet = request.method.equals("GET", ignoreCase = true)
        val isAuthEndpoint = path.startsWith("/api/v1/auth/") // 로그인/회원가입/재발급/로그아웃
        val isArticleList = isGet && path.endsWith("/articles")
        val isRandomArticle = isGet && path.contains("/articles/") && path.endsWith("/random")
        val shouldSkip = isAuthEndpoint || isArticleList || isRandomArticle

        // jwt 필요없는 대상 요청일 경우
        if (shouldSkip) {
            Log.d(
                TAG,
                "JWT 미첨부 요청 → method=$method, path=$path (공개 API 또는 인증 API)"
            )
            return chain.proceed(request)
        }

        /**
         * Access Token 읽기
         *
         * - DataStore는 Flow 기반이므로 runBlocking으로 동기 접근
         * - Interceptor는 네트워크 스레드에서 호출되므로
         *   여기서는 '짧고 단순한 작업'만 수행해야 함
         */
        val token = runBlocking {
            tokenManager.accessToken.first()
        }

        /**
         * Authorization 헤더 추가
         *
         * - 토큰이 존재하면: "Authorization: Bearer {token}"
         * - 토큰이 없으면: 그대로 요청 (→ 서버에서 401 반환)
         */
        val jwtRequest = if (!token.isNullOrEmpty()) {
            Log.d(TAG, "AccessToken 첨부 후 요청 전송 → path=$path")

            request.newBuilder()
                .addHeader(HEADER_AUTH, "$BEARER$token")
                .build()
        } else {
            Log.w(
                TAG,
                "AccessToken이 존재하지 않습니다. Authorization 헤더 없이 요청합니다. path=$path"
            )
            request
        }

        // 최종 요청을 서버로 전달
        return chain.proceed(jwtRequest)
    }

}