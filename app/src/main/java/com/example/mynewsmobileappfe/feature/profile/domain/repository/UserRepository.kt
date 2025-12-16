package com.example.mynewsmobileappfe.feature.profile.domain.repository

import com.example.mynewsmobileappfe.core.common.Resource
import com.example.mynewsmobileappfe.feature.profile.data.remote.dto.UserResponse
import kotlinx.coroutines.flow.Flow

interface UserRepository {

    /**
     * 내 정보 조회
     */
    fun getMyInfo(): Flow<Resource<UserResponse>>

    /**
     * 회원 탈퇴
     * - 탈퇴 후 로컬 토큰도 삭제됨
     */
    fun deleteUser(userId: Long): Flow<Resource<Unit>>
}