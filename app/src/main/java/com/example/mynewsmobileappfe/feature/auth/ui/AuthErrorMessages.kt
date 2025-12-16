package com.example.mynewsmobileappfe.feature.auth.ui

/**
 * 인증 관련 에러 메시지 상수
 * - Context 주입 없이 문자열 관리
 * - 일관성 있는 에러 메시지 제공
 */
object AuthErrorMessages {
    // 로그인 에러
    const val LOGIN_FAILED = "이메일 또는 비밀번호가 올바르지 않습니다."
    const val NETWORK_ERROR = "네트워크 오류가 발생했습니다."

    // 회원가입 에러
    const val SIGNUP_FAILED = "회원가입에 실패했습니다."

    // 로그아웃 에러
    const val LOGOUT_FAILED = "로그아웃에 실패했습니다."

    // 비밀번호 보안 가이드
    const val PASSWORD_WEAK_WARNING = "보안을 위해 특수문자를 포함하는 것을 권장합니다."
}