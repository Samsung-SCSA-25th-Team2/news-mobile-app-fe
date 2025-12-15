package com.example.mynewsmobileappfe.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mynewsmobileappfe.core.common.Resource
import com.example.mynewsmobileappfe.feature.auth.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

/**
 * ========================================
 * AuthViewModel - 동료 개발자 구현 가이드
 * ========================================
 *
 * [사용 가능한 Repository 메서드]
 * 1. authRepository.signUp(email, password): Flow<Resource<Unit>>
 *    - 회원가입
 *    - 성공: Resource.Success(Unit)
 *    - 실패: Resource.Error("이미 사용 중인 이메일입니다." 등)
 *
 * 2. authRepository.login(email, password): Flow<Resource<TokenResponse>>
 *    - 로그인 (성공 시 토큰이 자동으로 DataStore에 저장됨)
 *    - 성공: Resource.Success(TokenResponse)
 *    - 실패: Resource.Error("이메일 또는 비밀번호가 일치하지 않습니다." 등)
 *
 * 3. authRepository.logout(): Flow<Resource<Unit>>
 *    - 로그아웃 (DataStore 토큰 삭제)
 *
 * 4. authRepository.isLoggedIn(): Flow<Boolean>
 *    - 로그인 상태 확인
 *
 * [구현 예시]
 * ```
 * fun login(email: String, password: String) {
 *     authRepository.login(email, password)
 *         .onEach { result ->
 *             _loginState.value = when (result) {
 *                 is Resource.Loading -> AuthState.Loading
 *                 is Resource.Success -> AuthState.Success
 *                 is Resource.Error -> AuthState.Error(result.message ?: "오류")
 *             }
 *         }
 *         .launchIn(viewModelScope)
 * }
 * ```
 *
 * [UI 상태 관리]
 * - MutableStateFlow로 상태 관리
 * - sealed class로 상태 정의 (Loading, Success, Error, Idle)
 *
 * [화면 구성]
 * - LoginScreen: 이메일, 비밀번호 입력 → 로그인 버튼
 * - SignUpScreen: 이메일, 비밀번호, 비밀번호 확인 → 회원가입 버튼
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    // 로그인 상태
    private val _loginState = MutableStateFlow<AuthState>(AuthState.Idle)
    val loginState: StateFlow<AuthState> = _loginState.asStateFlow()

    // 회원가입 상태
    private val _signUpState = MutableStateFlow<AuthState>(AuthState.Idle)
    val signUpState: StateFlow<AuthState> = _signUpState.asStateFlow()

    // 로그인 여부 확인
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    init {
        // 앱 시작 시 로그인 상태 확인
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        authRepository.isLoggedIn()
            .onEach { isLoggedIn ->
                _isLoggedIn.value = isLoggedIn
            }
            .launchIn(viewModelScope)
    }

    // 로그인
    fun login(email: String, password: String) {
        authRepository.login(email, password)
            .onEach { result ->
                _loginState.value = when (result) {
                    is Resource.Loading -> AuthState.Loading
                    is Resource.Success -> {
                        _isLoggedIn.value = true
                        AuthState.Success
                    }
                    is Resource.Error -> AuthState.Error(result.message ?: "로그인에 실패했습니다.")
                }
            }
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
    }

    // 회원가입
    fun signUp(email: String, password: String) {
        authRepository.signUp(email, password)
            .onEach { result ->
                _signUpState.value = when (result) {
                    is Resource.Loading -> AuthState.Loading
                    is Resource.Success -> AuthState.Success
                    is Resource.Error -> AuthState.Error(result.message ?: "회원가입에 실패했습니다.")
                }
            }
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
    }

    // 로그아웃
    fun logout() {
        authRepository.logout()
            .onEach { result ->
                if (result is Resource.Success) {
                    _isLoggedIn.value = false
                }
            }
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
    }

    // 상태 초기화 (화면 이동 후 호출)
    fun resetState() {
        _loginState.value = AuthState.Idle
        _signUpState.value = AuthState.Idle
    }
}

/**
 * 인증 상태 sealed class
 */
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}
