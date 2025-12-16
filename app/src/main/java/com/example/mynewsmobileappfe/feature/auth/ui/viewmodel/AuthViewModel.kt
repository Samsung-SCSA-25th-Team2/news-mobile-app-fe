package com.example.mynewsmobileappfe.feature.auth.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mynewsmobileappfe.core.common.Resource
import com.example.mynewsmobileappfe.feature.auth.domain.repository.AuthRepository
import com.example.mynewsmobileappfe.feature.auth.ui.AuthEffect
import com.example.mynewsmobileappfe.feature.auth.ui.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    // One-shot Effect (네비게이션 등 1회성 이벤트)
    // - extraBufferCapacity = 1: emit 시 collector 없어도 1개까지 버퍼링
    // - recomposition과 무관하게 정확히 한 번만 소비
    private val _effect = MutableSharedFlow<AuthEffect>(extraBufferCapacity = 1)
    val effect = _effect.asSharedFlow()

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
                when (result) {
                    is Resource.Loading -> {
                        _loginState.value = AuthState.Loading
                    }
                    is Resource.Success -> {
                        _isLoggedIn.value = true
                        _loginState.value = AuthState.Success
                        _effect.emit(AuthEffect.NavigateHome) // 로그인 성공 → Home으로 navigate (1회성 이벤트)
                    }
                    is Resource.Error -> {
                        val msg = result.message ?: "이메일 또는 비밀번호가 올바르지 않습니다."

                        _effect.emit(AuthEffect.Toast(msg)) // Toast 발생
                        _loginState.value = AuthState.Error(msg)
                    }
                }
            }
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
    }

    // 회원가입
    fun signUp(email: String, password: String) {
        authRepository.signUp(email, password)
            .onEach { result ->
                when (result) {
                    is Resource.Loading -> {
                        _signUpState.value = AuthState.Loading
                    }
                    is Resource.Success -> {
                        _signUpState.value = AuthState.Success
                        _effect.emit(AuthEffect.NavigateToLogin) // 회원가입 성공 → Login 화면으로 navigate (1회성 이벤트)
                    }
                    is Resource.Error -> {
                        val msg = result.message ?: "회원가입에 실패했습니다."

                        _effect.emit(AuthEffect.Toast(msg))
                        _signUpState.value = AuthState.Error(msg)
                    }
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
