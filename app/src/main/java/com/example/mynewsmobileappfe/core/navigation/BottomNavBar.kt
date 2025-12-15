package com.example.mynewsmobileappfe.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Bottom Navigation 아이템 정의
 */
data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(
        screen = Screen.Politics,
        label = "정치",
        selectedIcon = Icons.Filled.AccountBalance,
        unselectedIcon = Icons.Outlined.AccountBalance
    ),
    BottomNavItem(
        screen = Screen.Economy,
        label = "경제",
        selectedIcon = Icons.Filled.Store,
        unselectedIcon = Icons.Outlined.Storefront
    ),
    BottomNavItem(
        screen = Screen.Social,
        label = "사회",
        selectedIcon = Icons.Filled.Groups,
        unselectedIcon = Icons.Outlined.Groups
    ),
    BottomNavItem(
        screen = Screen.Technology,
        label = "기술",
        selectedIcon = Icons.Filled.Science,
        unselectedIcon = Icons.Outlined.Science
    ),
    BottomNavItem(
        screen = Screen.Profile,
        label = "내 정보",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )
)

/**
 * ========================================
 * BottomNavBar - 동료 개발자 구현 가이드
 * ========================================
 *
 * [Bottom Navigation 구성]
 * - 정치: 정치 관련 기사
 * - 경제: 경제 관련 기사
 * - 사회: 사회 관련 기사
 * - 기술: 기술 관련 기사
 * - 내 정보: 프로필 (로그인 필요)
 *
 * [사용 예시]
 * ```
 * @Composable
 * fun BottomNavBar(
 *     currentRoute: String?,
 *     onNavigate: (Screen) -> Unit,
 *     isLoggedIn: Boolean,
 *     onLoginRequired: () -> Unit
 * ) {
 *     NavigationBar {
 *         bottomNavItems.forEach { item ->
 *             val isSelected = currentRoute == item.screen.route
 *
 *             NavigationBarItem(
 *                 selected = isSelected,
 *                 onClick = {
 *                     // 로그인 필요한 화면인지 확인
 *                     if (item.screen.route in Screen.authRequiredScreens && !isLoggedIn) {
 *                         onLoginRequired()
 *                     } else {
 *                         onNavigate(item.screen)
 *                     }
 *                 },
 *                 icon = {
 *                     Icon(
 *                         imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
 *                         contentDescription = item.label
 *                     )
 *                 },
 *                 label = { Text(item.label) }
 *             )
 *         }
 *     }
 * }
 * ```
 *
 * [로그인 필요 화면 처리]
 * - 카테고리 화면(정치/경제/사회/기술): 로그인 불필요
 * - 내 정보(Profile): 로그인 필요
 * - 북마크: 프로필 화면에서 접근
 * - 미로그인 시 onLoginRequired() 호출 → 로그인 화면으로 이동
 */
@Composable
fun BottomNavBar(
    currentRoute: String?,
    onNavigate: (Screen) -> Unit,
    isLoggedIn: Boolean = false,
    onLoginRequired: () -> Unit = {}
) {
    NavigationBar {
        bottomNavItems.forEach { item ->
            val isSelected = currentRoute == item.screen.route

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    // 로그인 필요한 화면인지 확인
                    if (item.screen.route in Screen.authRequiredScreens && !isLoggedIn) {
                        onLoginRequired()
                    } else {
                        onNavigate(item.screen)
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label) }
            )
        }
    }
}
