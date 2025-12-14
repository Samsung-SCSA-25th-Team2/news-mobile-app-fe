package com.example.mynewsmobileappfe.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map

/**
 * DataStore 초기화
 * - Context 확장 프로퍼티로 DataStore 인스턴스를 생성
 * - name = "auth_prefs" 파일에 저장됨
 *
 * by (위임) : "이 변수의 생성·관리·getter 로직을 대신 처리해줘”
 * - Singleton : Context당 하나
 * - Lazy init : 처음 접근 시 생성
 * - Thread-safe : 동시 접근 안전
 * - Lifecycle-safe : 메모리 누수 방지
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

/**
 * AccessToken/RefreshToken을 DataStore에 영구 저장하고 관리
 */
@Singleton
class TokenManager @Inject constructor( // 이 클래스는 Hilt가 생성해라
    @ApplicationContext private val context: Context
    // DataStore는 Context 기반, 앱 전역의 ApplicationContext를 사용해라
) {
    // 저장될 키 선언
    // stringPreferencesKey -> "access_token"이라는 String value가 올 key를 생성한다.
    private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
    private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")

    // 강제 로그아웃 이벤트 (앱 전역에서 구독)
    // Flow(스트림 - 다른 곳에서 온 데이터) vs StateFlow(가장 마지막 상태 - UI) vs SharedFlow(한번 이벤트 발생 - 로그아웃) ***
    private val _logoutEvent = MutableSharedFlow<Unit>()
    val logoutEvent: SharedFlow<Unit> = _logoutEvent.asSharedFlow()

    // 토큰 읽기 (Flow로 읽음) -> UI에서 collectAsState() 로 쉽게 사용 가능
    val accessToken: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[ACCESS_TOKEN_KEY]
    }

    val refreshToken: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[REFRESH_TOKEN_KEY]
    }

    //== 아래 코루틴 환경에서만 실행 가능 suspend fun ==//

    // 토큰 저장
    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY] = accessToken
            prefs[REFRESH_TOKEN_KEY] = refreshToken
        }
    }

    // 토큰 삭제 (로그아웃에서 사용할 예정)
    suspend fun clearTokens() {
        context.dataStore.edit { prefs ->
            prefs.remove(ACCESS_TOKEN_KEY)
            prefs.remove(REFRESH_TOKEN_KEY)
        }
        // 강제 로그아웃 이벤트 발행 (MainActivity가 구독하여 로그인 화면으로 이동)
        _logoutEvent.emit(Unit)
    }

}
