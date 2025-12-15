package com.example.mynewsmobileappfe.feature.profile.ui

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * ========================================
 * ProfileScreen - 동료 개발자 구현 가이드
 * ========================================
 *
 * [변경 사항]
 * - 북마크는 Bottom Nav에서 제거됨
 * - 북마크는 이 화면에서 "북마크한 기사" 메뉴로 접근
 * - onNavigateToBookmark 콜백 사용
 *
 * [화면 구조]
 * ```
 * Scaffold(
 *     topBar = { TopAppBar(title = "내 정보") },
 *     bottomBar = { BottomNavigation (정치, 경제, 사회, 기술, 내 정보) }
 * ) {
 *     Column {
 *         // 프로필 카드
 *         ProfileCard(user)
 *
 *         // 메뉴 리스트
 *         SettingsMenuList()
 *     }
 * }
 * ```
 *
 * [프로필 카드]
 * ```
 * Card {
 *     Row {
 *         // 프로필 아이콘 (기본 아바타)
 *         Icon(Icons.Default.AccountCircle, ...)
 *
 *         Column {
 *             Text(user.email)
 *             Text("북마크 ${user.bookmarkCount}개")
 *         }
 *     }
 * }
 * ```
 *
 * [설정 메뉴]
 * ```
 * // 북마크한 기사 보기 ⭐ 추가됨!
 * SettingsMenuItem(
 *     icon = Icons.Default.Bookmark,
 *     title = "북마크한 기사",
 *     onClick = { onNavigateToBookmark() }
 * )
 *
 * // 로그아웃
 * SettingsMenuItem(
 *     icon = Icons.Default.Logout,
 *     title = "로그아웃",
 *     onClick = { showLogoutDialog = true }
 * )
 *
 * // 회원 탈퇴
 * SettingsMenuItem(
 *     icon = Icons.Default.Delete,
 *     title = "회원 탈퇴",
 *     titleColor = MaterialTheme.colorScheme.error,
 *     onClick = { showDeleteDialog = true }
 * )
 * ```
 *
 * [로그아웃 다이얼로그]
 * ```
 * AlertDialog(
 *     onDismissRequest = { showLogoutDialog = false },
 *     title = { Text("로그아웃") },
 *     text = { Text("정말 로그아웃하시겠습니까?") },
 *     confirmButton = {
 *         TextButton(onClick = { viewModel.logout() }) {
 *             Text("로그아웃")
 *         }
 *     },
 *     dismissButton = {
 *         TextButton(onClick = { showLogoutDialog = false }) {
 *             Text("취소")
 *         }
 *     }
 * )
 * ```
 *
 * [회원 탈퇴 다이얼로그]
 * ```
 * AlertDialog(
 *     title = { Text("회원 탈퇴") },
 *     text = { Text("정말 탈퇴하시겠습니까?\n모든 데이터가 삭제되며 복구할 수 없습니다.") },
 *     confirmButton = {
 *         TextButton(
 *             onClick = { viewModel.deleteAccount() },
 *             colors = ButtonDefaults.textButtonColors(
 *                 contentColor = MaterialTheme.colorScheme.error
 *             )
 *         ) {
 *             Text("탈퇴")
 *         }
 *     },
 *     dismissButton = { ... }
 * )
 * ```
 *
 * [이벤트 처리]
 * ```
 * val logoutEvent by viewModel.logoutEvent.collectAsStateWithLifecycle()
 *
 * LaunchedEffect(logoutEvent) {
 *     if (logoutEvent) {
 *         viewModel.resetEvents()
 *         onNavigateToLogin()
 *     }
 * }
 * ```
 */
@Composable
fun ProfileScreen(
    onNavigateToHome: () -> Unit = {},
    onNavigateToBookmark: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val userState by viewModel.userState.collectAsStateWithLifecycle()
    val logoutEvent by viewModel.logoutEvent.collectAsStateWithLifecycle()
    val deleteEvent by viewModel.deleteAccountEvent.collectAsStateWithLifecycle()

    if (logoutEvent || deleteEvent) {
        viewModel.resetEvents()
        onNavigateToLogin()
    }

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        when (userState) {
            is ProfileState.Loading -> Text("불러오는 중...", modifier = Modifier.align(Alignment.Center))
            is ProfileState.Success -> {
                val user = (userState as ProfileState.Success).user
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("이메일: ${user.email}", style = MaterialTheme.typography.titleMedium)
                    Text("북마크 수: ${user.bookmarkCount}")

                    // 북마크한 기사 보기
                    Button(onClick = onNavigateToBookmark) { Text("북마크한 기사") }

                    Button(onClick = { viewModel.logout() }) { Text("로그아웃") }
                    Button(onClick = { viewModel.deleteAccount() }) { Text("회원 탈퇴") }
                }
            }
            is ProfileState.Error -> Text(
                (userState as ProfileState.Error).message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Center)
            )
            else -> {}
        }
    }
}

/**
 * 설정 메뉴 아이템 컴포넌트
 */
@Composable
fun SettingsMenuItem(
    // icon: ImageVector,
    title: String = "",
    onClick: () -> Unit = {}
) {
    // TODO: 설정 메뉴 아이템 UI 구현
}
