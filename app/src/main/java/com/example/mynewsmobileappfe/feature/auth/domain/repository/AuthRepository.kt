package com.example.mynewsmobileappfe.feature.auth.domain.repository

import com.example.mynewsmobileappfe.core.common.Resource
import com.example.mynewsmobileappfe.feature.auth.data.remote.dto.TokenResponse
import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    /**
     * 회원가입
     */
    fun signUp(email: String, password: String): Flow<Resource<Unit>>

    /**
     * 로그인
     * 성공 시 토큰을 DataStore에 저장
     */
    fun login(email: String, password: String): Flow<Resource<TokenResponse>>

    /**
     * 로그아웃
     * DataStore의 토큰 삭제
     */
    fun logout(): Flow<Resource<Unit>>

    /**
     * 로그인 상태 확인
     */
    fun isLoggedIn(): Flow<Boolean>
}