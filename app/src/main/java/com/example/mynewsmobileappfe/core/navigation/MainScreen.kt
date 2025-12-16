package com.example.mynewsmobileappfe.core.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mynewsmobileappfe.feature.auth.ui.viewmodel.AuthViewModel

/**
 * ========================================
 * MainScreen - 앱의 메인 네비게이션 컨테이너
 * ========================================
 *
 * [요약]
 * 현재 route와 로그인 상태를 관찰해서,
 * BottomNavBar를 보여줄지 결정하고,
 * NavGraph를 통해 화면 전환을 담당하는 컨테이너.
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
    // navController를 파라미터로 받되, 안 주면 내부에서 rememberNavController()로 만든다.
    navController: NavHostController = rememberNavController()
) {
    // authViewModel의 로그인 여부 관찰 -> recomposition 위함
    val authViewModel: AuthViewModel = hiltViewModel()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsStateWithLifecycle() // flow -> state

    // backStackEntry = “현재 목적지(화면)”
    // NavController의 “현재 목적지(화면)”가 바뀌는 걸 State로 관찰하는 코드
    // 화면 전환이 일어날 때마다 backStackEntry 값이 바뀌고 → recomposition.
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route // 정의한 특정화면 문자열

    Scaffold(
        // 하단 네비게이션바 자리 확보
        // 본문 컨텐츠가 하단바에 가려지지 않도록 padding 계산

        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentRoute in Screen.bottomNavScreens.map { it.route }) {
                // 네비게이션바를 표시할 화면 중 하나면 map
                // 로그인, 회원가입, 기사 상세 화면에서는 네비게이션바 숨기기

                BottomNavBar(
                    currentRoute = currentRoute, // 현재 앱화면 넘기기
                    onNavigate = { // 함수를 변수(인자)로 넘긴 것
                        screen ->
                        navController.navigate(screen.route) {
                            popUpTo(Screen.Politics.route) { inclusive = false } // Politics(시작 화면)은 항상 백스택의 루트로 유지
                            launchSingleTop = true // 탭 중복 쌓기 방지
                        }
                    },
                    isLoggedIn = isLoggedIn,
                    onLoginRequired = { // 함수를 변수(인자)로 넘긴 것
                        // 로그인 필요 시 Login 화면으로 이동 -> BottomNavBar에서 이벤트 발생
                        navController.navigate(Screen.Login.route) {
                            launchSingleTop = true // 탭 중복 쌓기 방지
                        }
                    }
                )
            }
        }
    ) { innerPadding -> // 위에서 사용하고, 남은 영역 innerPadding
        NavGraph(
            navController = navController,
            isLoggedIn = isLoggedIn,
            onLoginRequired = {
                // 기사 좋아요/북마크 등 로그인 필요 액션 시
                navController.navigate(Screen.Login.route) {
                    launchSingleTop = true
                }
            },
            modifier = Modifier.padding(innerPadding)
        )
    }
}

