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

    // jwt 필요없는 화이트리스트
    private val whiteList = listOf(
        // auth
        "/auth/signup",
        "/auth/login",
        "/auth/reissue", // 이미 expired 된 accessToken을 헤더가 아니라 body에 넣어서 보냄

        // article
        "/articles",
        "/articles/*/random"
    )


    override fun intercept(chain: Interceptor.Chain): Response {

        val request = chain.request()

        // url에서 화이트리스트 분리
        val path = request.url.encodedPath // QueryParameter 분리
        val shouldSkip = whiteList.any { path.contains(it) }

        // jwt 필요없는 대상 요청일 경우
        if (shouldSkip) {
            // OkHttp가 request를 서버로 전송함.
            return chain.proceed(request)
        }

        // 토큰 읽기 -> runBlocking 사용 (최초 한번만 실행 보장)
        val token = runBlocking { tokenManager.accessToken.first() }

        // Authorization 헤더 자동 추가
        val jwtRequest = if (!token.isNullOrEmpty()) {
            request.newBuilder()
                .addHeader("Authorization", token) // Bearer 없이 전송
                .build()
        } else {
            request
        }

        // OkHttp가 jwtRequest를 서버로 전송함.
        return chain.proceed(jwtRequest)
    }
}
