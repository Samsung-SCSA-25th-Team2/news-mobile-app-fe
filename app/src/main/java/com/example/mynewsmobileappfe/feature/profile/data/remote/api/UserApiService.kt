package com.example.mynewsmobileappfe.feature.profile.data.remote.api

import com.example.mynewsmobileappfe.feature.profile.data.remote.dto.UserResponse
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path

interface UserApiService {

    /**
     * 내 정보 조회
     * JWT 토큰이 필요합니다.
     */
    @GET("user/me")
    suspend fun getMyInfo(): Response<UserResponse>

    /**
     * 회원 탈퇴
     * 해당 사용자의 모든 북마크도 함께 삭제됩니다.
     * @param userId 탈퇴할 사용자 ID
     */
    @DELETE("user/{userId}")
    suspend fun deleteUser(
        @Path("userId") userId: Long
    ): Response<Unit>
}