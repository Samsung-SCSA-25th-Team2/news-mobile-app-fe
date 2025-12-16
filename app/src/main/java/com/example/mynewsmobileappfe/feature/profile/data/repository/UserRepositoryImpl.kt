package com.example.mynewsmobileappfe.feature.profile.data.repository

import com.example.mynewsmobileappfe.core.common.Resource
import com.example.mynewsmobileappfe.core.jwt.TokenManager
import com.example.mynewsmobileappfe.feature.profile.data.remote.api.UserApiService
import com.example.mynewsmobileappfe.feature.profile.data.remote.dto.UserResponse
import com.example.mynewsmobileappfe.feature.profile.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userApiService: UserApiService,
    private val tokenManager: TokenManager
) : UserRepository {

    override fun getMyInfo(): Flow<Resource<UserResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = userApiService.getMyInfo()
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                val errorMessage = when (response.code()) {
                    401, 403 -> {
                        // 인증 실패 → 로컬 토큰 삭제 후 재로그인 유도
                        tokenManager.clearTokens()
                        "로그인이 필요합니다."
                    }
                    404 -> "사용자를 찾을 수 없습니다."
                    else -> "사용자 정보를 불러오는데 실패했습니다."
                }
                emit(Resource.Error(errorMessage))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "네트워크 오류가 발생했습니다."))
        }
    }.flowOn(Dispatchers.IO)

    override fun deleteUser(userId: Long): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val response = userApiService.deleteUser(userId)
            if (response.isSuccessful) {
                // 탈퇴 성공 시 로컬 토큰 삭제
                tokenManager.clearTokens()
                emit(Resource.Success(Unit))
            } else {
                val errorMessage = when (response.code()) {
                    401, 403 -> {
                        tokenManager.clearTokens()
                        "로그인이 필요합니다."
                    }
                    404 -> "사용자를 찾을 수 없습니다."
                    else -> "회원 탈퇴에 실패했습니다."
                }
                emit(Resource.Error(errorMessage))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "네트워크 오류가 발생했습니다."))
        }
    }.flowOn(Dispatchers.IO)
}
