package com.example.mynewsmobileappfe.core.common

/**
 * 네트워크/DB 요청의 상태를 명확하게 표현하기 위한 공통 Response Wrapper
 * @param <T>
 *
 * - Resource는 이 세 가지 상태를 하나의 타입으로 묶어서 ViewModel → UI 로 전달할 수 있게 함.
 */
sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null
) {
    class Success<T>(data: T) : Resource<T>(data)
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)
    class Loading<T> : Resource<T>()
}
