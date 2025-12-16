package com.example.mynewsmobileappfe.feature.auth.ui.validation

import android.util.Patterns

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val message: String) : ValidationResult()
}

/**
 * 비밀번호 강도
 */
sealed class PasswordStrength {
    object Strong : PasswordStrength()   // 영문 + 숫자 + 특수문자
    object Medium : PasswordStrength()   // 영문 + 숫자
    object Weak : PasswordStrength()     // 기타
}

object SignUpValidator {

    // 기본 정책: 영문 + 숫자 (8자 이상)
    private val PASSWORD_REGEX_BASIC =
        Regex("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@\$!%*#?&]{8,}$")

    // 강력한 정책: 영문 + 숫자 + 특수문자 (8자 이상)
    private val PASSWORD_REGEX_STRONG =
        Regex("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@\$!%*#?&])[A-Za-z\\d@\$!%*#?&]{8,}$")

    fun validateEmail(email: String): ValidationResult {
        val e = email.trim()

        return when {
            e.isEmpty() ->
                ValidationResult.Invalid("이메일을 입력해 주세요.")

            e.length > 254 ->
                ValidationResult.Invalid("이메일이 너무 깁니다.")

            !Patterns.EMAIL_ADDRESS.matcher(e).matches() ->
                ValidationResult.Invalid("이메일 형식을 확인해 주세요.")

            ".." in e ->
                ValidationResult.Invalid("이메일에 연속된 점(..)은 사용할 수 없습니다.")

            else -> ValidationResult.Valid
        }
    }

    /**
     * 비밀번호 유효성 검사
     * - 최소 8자
     * - 영문 필수
     * - 숫자 필수
     * - 특수문자 권장 (선택)
     */
    fun validatePassword(password: String, email: String): ValidationResult {
        val localPart = email.substringBefore("@", "")

        return when {
            password.isEmpty() ->
                ValidationResult.Invalid("비밀번호를 입력해 주세요.")

            password.contains(" ") ->
                ValidationResult.Invalid("비밀번호에 공백은 사용할 수 없습니다.")

            password.length < 8 ->
                ValidationResult.Invalid("비밀번호는 8자 이상이어야 합니다.")

            !PASSWORD_REGEX_BASIC.matches(password) ->
                ValidationResult.Invalid("비밀번호는 영문, 숫자를 포함해야 합니다.")

            localPart.isNotEmpty() && password.contains(localPart, ignoreCase = true) ->
                ValidationResult.Invalid("비밀번호에 이메일 정보를 포함할 수 없습니다.")

            Regex("(.)\\1\\1").containsMatchIn(password) ->
                ValidationResult.Invalid("동일한 문자를 3번 이상 연속 사용할 수 없습니다.")

            else -> ValidationResult.Valid
        }
    }

    /**
     * 비밀번호 강도 체크
     * - Strong: 영문 + 숫자 + 특수문자
     * - Medium: 영문 + 숫자
     * - Weak: 기타
     */
    fun checkPasswordStrength(password: String): PasswordStrength {
        return when {
            PASSWORD_REGEX_STRONG.matches(password) -> PasswordStrength.Strong
            PASSWORD_REGEX_BASIC.matches(password) -> PasswordStrength.Medium
            else -> PasswordStrength.Weak
        }
    }

    fun validatePasswordConfirm(
        password: String,
        confirm: String
    ): ValidationResult =
        if (password == confirm) ValidationResult.Valid
        else ValidationResult.Invalid("비밀번호가 일치하지 않습니다.")
}
