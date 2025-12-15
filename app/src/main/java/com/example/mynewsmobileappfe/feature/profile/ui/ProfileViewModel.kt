package com.example.mynewsmobileappfe.feature.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mynewsmobileappfe.core.common.Resource
import com.example.mynewsmobileappfe.feature.auth.domain.repository.AuthRepository
import com.example.mynewsmobileappfe.feature.profile.data.remote.dto.UserResponse
import com.example.mynewsmobileappfe.feature.profile.domain.repository.UserRepository
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
 * ProfileViewModel - 동료 개발자 구현 가이드
 * ========================================
 *
 * [사용 가능한 Repository 메서드]
 *
 * === UserRepository ===
 * 1. userRepository.getMyInfo(): Flow<Resource<UserResponse>>
 *    - 내 정보 조회
 *    - UserResponse: { id: Long, email: String, bookmarkCount: Int }
 *
 * 2. userRepository.deleteUser(userId): Flow<Resource<Unit>>
 *    - 회원 탈퇴 (탈퇴 시 자동으로 토큰 삭제됨)
 *
 * === AuthRepository ===
 * 3. authRepository.logout(): Flow<Resource<Unit>>
 *    - 로그아웃
 *
 * [구현 예시]
 * ```
 * fun loadUserInfo() {
 *     userRepository.getMyInfo()
 *         .onEach { result ->
 *             _userState.value = when (result) {
 *                 is Resource.Loading -> ProfileState.Loading
 *                 is Resource.Success -> ProfileState.Success(result.data!!)
 *                 is Resource.Error -> ProfileState.Error(result.message ?: "오류")
 *             }
 *         }
 *         .launchIn(viewModelScope)
 * }
 *
 * fun logout() {
 *     authRepository.logout()
 *         .onEach { result ->
 *             if (result is Resource.Success) {
 *                 _logoutEvent.emit(Unit)  // 로그인 화면으로 이동
 *             }
 *         }
 *         .launchIn(viewModelScope)
 * }
 *
 * fun deleteAccount() {
 *     val userId = (_userState.value as? ProfileState.Success)?.user?.id ?: return
 *     userRepository.deleteUser(userId)
 *         .onEach { result ->
 *             if (result is Resource.Success) {
 *                 _deleteAccountEvent.emit(Unit)  // 로그인 화면으로 이동
 *             }
 *         }
 *         .launchIn(viewModelScope)
 * }
 * ```
 *
 * [화면 구성]
 * - 프로필 섹션: 이메일, 북마크 개수
 * - 설정 메뉴: 로그아웃, 회원 탈퇴
 * - 앱 정보: 버전 등 (선택)
 *
 * [회원 탈퇴 시]
 * - 확인 다이얼로그 필수
 * - "정말 탈퇴하시겠습니까? 모든 데이터가 삭제됩니다."
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // TODO: 사용자 정보 상태
    private val _userState = MutableStateFlow<ProfileState>(ProfileState.Idle)
    val userState: StateFlow<ProfileState> = _userState.asStateFlow()

    // TODO: 로그아웃 이벤트 (SharedFlow 사용 권장)
    private val _logoutEvent = MutableStateFlow(false)
    val logoutEvent: StateFlow<Boolean> = _logoutEvent.asStateFlow()

    // TODO: 회원탈퇴 이벤트
    private val _deleteAccountEvent = MutableStateFlow(false)
    val deleteAccountEvent: StateFlow<Boolean> = _deleteAccountEvent.asStateFlow()

    init {
        loadUserInfo()
    }

    // TODO: 사용자 정보 로드
    fun loadUserInfo() {
        userRepository.getMyInfo()
            .onEach { result ->
                _userState.value = when (result) {
                    is Resource.Loading -> ProfileState.Loading
                    is Resource.Success -> ProfileState.Success(result.data!!)
                    is Resource.Error -> ProfileState.Error(result.message ?: "사용자 정보를 불러오지 못했습니다.")
                }
            }
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
    }

    // TODO: 로그아웃
    fun logout() {
        authRepository.logout()
            .onEach { result ->
                if (result is Resource.Success) {
                    _logoutEvent.value = true
                }
            }
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
    }

    // TODO: 회원 탈퇴
    fun deleteAccount() {
        val userId = (_userState.value as? ProfileState.Success)?.user?.id ?: return
        userRepository.deleteUser(userId)
            .onEach { result ->
                if (result is Resource.Success) {
                    _deleteAccountEvent.value = true
                } else if (result is Resource.Error) {
                    _userState.value = ProfileState.Error(result.message ?: "회원 탈퇴에 실패했습니다.")
                }
            }
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
    }

    // TODO: 이벤트 소비 후 초기화
    fun resetEvents() {
        _logoutEvent.value = false
        _deleteAccountEvent.value = false
    }
}

/**
 * 프로필 화면 상태 sealed class
 */
sealed class ProfileState {
    object Idle : ProfileState()
    object Loading : ProfileState()
    data class Success(val user: UserResponse) : ProfileState()
    data class Error(val message: String) : ProfileState()
}
