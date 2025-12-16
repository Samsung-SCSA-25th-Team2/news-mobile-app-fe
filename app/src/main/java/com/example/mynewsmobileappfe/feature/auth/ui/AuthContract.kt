package com.example.mynewsmobileappfe.feature.auth.ui

/**
 * 인증 상태 sealed class
 */
sealed interface AuthState {
    object Idle : AuthState
    object Loading : AuthState
    object Success : AuthState
    data class Error(val message: String) : AuthState
}

/**
 * One-shot Event Pattern:
 * - MutableSharedFlow로 1회성 이벤트 emit
 * - recomposition과 무관하게 정확히 한 번만 소비
 * - 중복 navigate 방지
 */
sealed interface AuthEffect {
    /** 로그인 성공 → Home(Politics) 화면으로 이동 */
    data object NavigateHome : AuthEffect

    /** 회원가입 성공 → Login 화면으로 이동 */
    data object NavigateToLogin : AuthEffect

    /** Toast 메시지 표시 */
    data class Toast(val message: String) : AuthEffect
}