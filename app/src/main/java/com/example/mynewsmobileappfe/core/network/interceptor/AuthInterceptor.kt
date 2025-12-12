package com.example.mynewsmobileappfe.core.network.interceptor

import com.example.mynewsmobileappfe.core.datastore.TokenManager
import jakarta.inject.Inject
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
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
): Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {

        // 토큰 읽기 -> runBlocking 사용 (최초 한번만 실행 보장)
        val token = runBlocking { tokenManager.accessToken.first() }

        // Authorization 헤더 자동 추가
        val request = if (!token.isNullOrEmpty()) {
            chain.request().newBuilder()
                .addHeader("Authorization", token) // Bearer 없이 전송
                .build()
        } else {
            chain.request()
        }

        // OkHttp가 최종 request를 서버로 전송함.
        return chain.proceed(request)
    }
}
