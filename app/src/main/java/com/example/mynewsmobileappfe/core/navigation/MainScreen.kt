package com.example.mynewsmobileappfe.core.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mynewsmobileappfe.feature.auth.ui.AuthViewModel

/**
 * ========================================
 * MainScreen - 앱의 메인 네비게이션 컨테이너
 * ========================================
 *
 * [역할]
 * - NavController 생성 및 관리
 * - Bottom Navigation Bar 표시 (5개 탭)
 * - 로그인 상태 관리
 * - 화면 전환 로직
 *
 * [구조]
 * ```
 * MainScreen
 * ├── NavController (rememberNavController)
 * ├── AuthViewModel (로그인 상태 관리)
 * └── Scaffold
 *     ├── BottomNavBar (정치/경제/사회/기술/내 정보)
 *     └── NavGraph (화면 전환)
 * ```
 *
 * [Bottom Navigation 구성]
 * 1. 정치 (Politics) - 로그인 불필요
 * 2. 경제 (Economy) - 로그인 불필요
 * 3. 사회 (Social) - 로그인 불필요
 * 4. 기술 (Technology) - 로그인 불필요
 * 5. 내 정보 (Profile) - 로그인 필요
 *
 * [북마크]
 * - Bottom Nav에 없음
 * - Profile 화면에서 "북마크한 기사" 메뉴로 접근
 *
 * [로그인 상태 관리]
 * - AuthViewModel의 isLoggedIn Flow 구독
 * - TokenManager에 토큰이 있으면 true, 없으면 false
 * - Bottom Navigation과 NavGraph에서 활용
 *
 * [주의]
 * - 로그아웃 이벤트는 MainActivity에서 전역적으로 관찰
 * - 이 컴포넌트는 순수하게 네비게이션 구조만 담당
 */
@Composable
fun MainScreen(
    navController: NavHostController = rememberNavController()
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        // 하단 네비게이션바 자리 확보
        // 본문 컨텐츠가 하단바에 가려지지 않도록 padding 계산

        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentRoute in Screen.bottomNavScreens.map { it.route }) {
                // 네비게이션바를 표시할 화면 중 하나면 map
                // 로그인, 회원가입, 기사 상세 화면에서는 네비게이션바 숨기기

                BottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { screen ->
                        navController.navigate(screen.route) {
                            // Politics(시작 화면)은 항상 백스택의 루트로 유지
                            popUpTo(Screen.Politics.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    isLoggedIn = isLoggedIn,
                    onLoginRequired = {
                        // 로그인 필요 시 Login 화면으로 이동
                        navController.navigate(Screen.Login.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavGraph(
            navController = navController,
            isLoggedIn = isLoggedIn,
            onLoginRequired = {
                // 기사 좋아요/북마크 등 로그인 필요 액션 시
                navController.navigate(Screen.Login.route) {
                    launchSingleTop = true
                }
            }
        )
    }
}

