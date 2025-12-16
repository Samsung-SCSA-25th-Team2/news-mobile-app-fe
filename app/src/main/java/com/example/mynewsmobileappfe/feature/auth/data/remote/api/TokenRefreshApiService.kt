package com.example.mynewsmobileappfe.feature.auth.data.remote.api

import com.example.mynewsmobileappfe.feature.auth.data.remote.dto.TokenRefreshRequest
import com.example.mynewsmobileappfe.feature.auth.data.remote.dto.TokenResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface TokenRefreshApiService {

    /**
     * Refresh Token으로 새로운 Access Token 발급
     * @param request accessToken과 refreshToken을 담은 요청 바디
     */
    @POST("auth/reissue")
    suspend fun refreshToken(
        @Body request: TokenRefreshRequest
    ): Response<TokenResponse>
    
}