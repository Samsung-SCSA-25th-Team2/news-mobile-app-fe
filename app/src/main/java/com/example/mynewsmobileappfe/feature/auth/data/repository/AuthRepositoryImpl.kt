package com.example.mynewsmobileappfe.feature.auth.data.repository

import com.example.mynewsmobileappfe.core.common.Resource
import com.example.mynewsmobileappfe.core.datastore.TokenManager
import com.example.mynewsmobileappfe.feature.auth.data.remote.api.AuthApiService
import com.example.mynewsmobileappfe.feature.auth.data.remote.dto.LoginRequest
import com.example.mynewsmobileappfe.feature.auth.data.remote.dto.SignUpRequest
import com.example.mynewsmobileappfe.feature.auth.data.remote.dto.TokenResponse
import com.example.mynewsmobileappfe.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApiService: AuthApiService,
    private val tokenManager: TokenManager
) : AuthRepository {

    override fun signUp(email: String, password: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val response = authApiService.signUp(SignUpRequest(email, password))
            if (response.isSuccessful) {
                emit(Resource.Success(Unit))
            } else {
                val errorMessage = when (response.code()) {
                    400 -> "입력 정보를 확인해주세요."
                    409 -> "이미 사용 중인 이메일입니다."
                    else -> "회원가입에 실패했습니다."
                }
                emit(Resource.Error(errorMessage))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "네트워크 오류가 발생했습니다."))
        }
    }.flowOn(Dispatchers.IO)

    override fun login(email: String, password: String): Flow<Resource<TokenResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = authApiService.login(LoginRequest(email, password))
            if (response.isSuccessful && response.body() != null) {
                val tokenResponse = response.body()!!
                // 토큰 저장
                tokenManager.saveTokens(
                    accessToken = tokenResponse.accessToken,
                    refreshToken = tokenResponse.refreshToken
                )
                emit(Resource.Success(tokenResponse))
            } else {
                val errorMessage = when (response.code()) {
                    400 -> "입력 정보를 확인해주세요."
                    401 -> "이메일 또는 비밀번호가 일치하지 않습니다."
                    else -> "로그인에 실패했습니다."
                }
                emit(Resource.Error(errorMessage))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "네트워크 오류가 발생했습니다."))
        }
    }.flowOn(Dispatchers.IO)

    override fun logout(): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val response = authApiService.logout()
            // 서버 응답과 관계없이 로컬 토큰은 삭제
            tokenManager.clearTokens()
            if (response.isSuccessful) {
                emit(Resource.Success(Unit))
            } else {
                // 서버 로그아웃 실패해도 로컬은 이미 정리됨
                emit(Resource.Success(Unit))
            }
        } catch (e: Exception) {
            // 네트워크 오류 시에도 로컬 토큰은 삭제
            tokenManager.clearTokens()
            emit(Resource.Success(Unit))
        }
    }.flowOn(Dispatchers.IO)

    override fun isLoggedIn(): Flow<Boolean> {
        return tokenManager.accessToken.map { token ->
            !token.isNullOrEmpty()
        }
    }
}
