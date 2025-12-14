package com.example.mynewsmobileappfe.feature.auth.data.remote.api

import com.example.mynewsmobileappfe.feature.auth.data.remote.dto.LoginRequest
import com.example.mynewsmobileappfe.feature.auth.data.remote.dto.SignUpRequest
import com.example.mynewsmobileappfe.feature.auth.data.remote.dto.TokenRefreshRequest
import com.example.mynewsmobileappfe.feature.auth.data.remote.dto.TokenResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {

    /**
     * 회원가입
     * @param request 이메일과 비밀번호를 담은 요청 바디
     */
    @POST("auth/signup")
    suspend fun signUp(
        @Body request: SignUpRequest
    ): Response<Unit>

    /**
     * 로그인
     * @param request 이메일과 비밀번호를 담은 요청 바디
     * @return AccessToken, RefreshToken 등 토큰 정보
     */
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<TokenResponse>

    /**
     * 로그아웃
     * Redis에 저장된 RefreshToken을 삭제
     */
    @POST("auth/logout")
    suspend fun logout(): Response<Unit>

}
