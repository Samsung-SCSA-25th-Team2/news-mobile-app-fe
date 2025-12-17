package com.example.mynewsmobileappfe.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.mynewsmobileappfe.feature.auth.ui.view.LoginScreen
import com.example.mynewsmobileappfe.feature.auth.ui.view.SignUpScreen
import com.example.mynewsmobileappfe.feature.bookmark.ui.BookmarkScreen
import com.example.mynewsmobileappfe.feature.news.ui.ArticleDetailScreen
import com.example.mynewsmobileappfe.feature.news.cache.ArticleCache
import com.example.mynewsmobileappfe.feature.news.ui.HomeViewModel
import com.example.mynewsmobileappfe.feature.news.ui.HomeScreen
import com.example.mynewsmobileappfe.feature.profile.ui.ProfileScreen

/**
 * ========================================
 * NavGraph - 동료 개발자 구현 가이드
 * ========================================
 *
 * [네비게이션 흐름]
 *
 * 1. 앱 시작 → Politics (정치 카테고리) - 로그인 불필요
 *    - 모든 카테고리 화면에서 기사 목록 조회 가능
 *    - 북마크/좋아요 클릭 시 → 로그인 확인 → 미로그인 시 LoginScreen
 *
 * 2. Bottom Navigation (5개)
 *    - 정치/경제/사회/기술: 로그인 불필요, 누구나 접근 가능
 *    - 내 정보(Profile): 로그인 필요 → 미로그인 시 LoginScreen으로 리다이렉트
 *
 * 3. 북마크
 *    - Bottom Nav에 없음
 *    - Profile 화면 내에서 접근 가능
 *
 * 4. 로그인 성공 후 → Politics (시작 화면)
 *
 * 5. 로그아웃/회원탈퇴 → Politics (백스택 클리어)
 */
@Composable
fun NavGraph(
    navController: NavHostController, // 실제 화면 이동 수행
    isLoggedIn: Boolean, // 로그인 여부
    onLoginRequired: () -> Unit, // 로그인 화면으로 보내는 동작
    modifier: Modifier = Modifier
) {
    /**
     * 지금 어떤 화면(Composable)을 보여줄지 결정해서 실제로 그려주는 컨테이너
     */
    NavHost(
        navController = navController,
        startDestination = Screen.Politics.route,  // 정치 카테고리에서 시작 (로그인 불필요)
        modifier = modifier
    ) {
        // ===== Auth Screens =====
        composable(Screen.Login.route) {
            // `라우트 가드`: 이미 로그인 상태면 LoginScreen 렌더링하지 않고 바로 redirect
            // - UX 개선: 로그인 화면 깜빡임 방지
            // - launchSingleTop: 중복 navigate 방지
            if (isLoggedIn) { // 로그인 O -> 로그인 화면으로 접근하려고 하면, 메인으로 라우팅
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Politics.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            } else { // 로그인 X -> 로그인 화면 O
                LoginScreen(
                    onNavigateToSignUp = {
                        navController.navigate(Screen.SignUp.route)
                    },
                    onNavigateToHome = {
                        // 로그인 후 정치 카테고리로 이동
                        navController.navigate(Screen.Politics.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }

        composable(Screen.SignUp.route) {
            SignUpScreen(
                onNavigateBack = { // 함수 인자로 넘기기
                    navController.popBackStack()
                },
                onSignUpSuccess = { // 함수 인자로 넘기기
                    // 회원가입 성공 → 로그인 화면으로
                    navController.popBackStack()
                }
            )
        }

        // ===== News Category Screens (Bottom Navigation) =====
        // 정치
        composable(Screen.Politics.route) {
            val politicsSection = Screen.routeToSection(Screen.Politics.route)
            android.util.Log.d("NavGraph", "Politics composable - section: $politicsSection")
            val homeViewModel = rememberSharedHomeViewModel(navController)
            HomeScreen(
                section = politicsSection,
                isLoggedIn = isLoggedIn,
                onLoginRequired = onLoginRequired,
                viewModel = homeViewModel,
                onArticleClick = { articleId ->
                    navController.navigate(Screen.ArticleDetail.createRoute(articleId))
                }
            )
        }

        // 경제
        composable(Screen.Economy.route) {
            val economySection = Screen.routeToSection(Screen.Economy.route)
            android.util.Log.d("NavGraph", "Economy composable - section: $economySection")
            val homeViewModel = rememberSharedHomeViewModel(navController)
            HomeScreen(
                section = economySection,
                isLoggedIn = isLoggedIn,
                onLoginRequired = onLoginRequired,
                viewModel = homeViewModel,
                onArticleClick = { articleId ->
                    navController.navigate(Screen.ArticleDetail.createRoute(articleId))
                }
            )
        }

        // 사회
        composable(Screen.Social.route) {
            val socialSection = Screen.routeToSection(Screen.Social.route)
            android.util.Log.d("NavGraph", "Social composable - section: $socialSection")
            val homeViewModel = rememberSharedHomeViewModel(navController)
            HomeScreen(
                section = socialSection,
                isLoggedIn = isLoggedIn,
                onLoginRequired = onLoginRequired,
                viewModel = homeViewModel,
                onArticleClick = { articleId ->
                    navController.navigate(Screen.ArticleDetail.createRoute(articleId))
                }
            )
        }

        // 기술
        composable(Screen.Technology.route) {
            val technologySection = Screen.routeToSection(Screen.Technology.route)
            android.util.Log.d("NavGraph", "Technology composable - section: $technologySection")
            val homeViewModel = rememberSharedHomeViewModel(navController)
            HomeScreen(
                section = technologySection,
                isLoggedIn = isLoggedIn,
                onLoginRequired = onLoginRequired,
                viewModel = homeViewModel,
                onArticleClick = { articleId ->
                    navController.navigate(Screen.ArticleDetail.createRoute(articleId))
                }
            )
        }

        // ===== Profile Screen (Bottom Navigation) =====
        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateToBookmark = {
                    // 프로필에서 북마크로 이동
                    navController.navigate(Screen.Bookmark.route)
                },
                onLoginRequired = onLoginRequired
            )
        }

        // ===== Bookmark Screen (Profile에서 접근) =====
        composable(Screen.Bookmark.route) {
            BookmarkScreen(
                onNavigateBack = {
                    // 뒤로가기 (Profile로 돌아감)
                    navController.popBackStack()
                },
                onArticleClick = { article ->
                    ArticleCache.putArticle(article)
                    navController.navigate(Screen.ArticleDetail.createRoute(article.articleId))
                }
            )
        }

        // ===== Article Detail Screen =====
        composable(
            route = Screen.ArticleDetail.route,
            arguments = listOf(
                navArgument("articleId") { type = NavType.LongType }
            ),
            deepLinks = listOf(
                // NFC 리더에서 쏘는 딥링크: nfcnews://article/{articleId}
                navDeepLink {
                    uriPattern = "nfcnews://article/{articleId}"
                }
            )
        ) { backStackEntry ->
            val articleId = backStackEntry.arguments?.getLong("articleId") ?: 0L
            ArticleDetailScreen(
                articleId = articleId,
                isLoggedIn = isLoggedIn,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLoginRequired = onLoginRequired
            )
        }
    }
}

@Composable
private fun rememberSharedHomeViewModel(navController: NavHostController): HomeViewModel {
    // Start destination(정치) 백스택 엔트리를 기준으로 ViewModel을 공유해서
    // 섹션 전환 시에도 캐시를 재사용한다.
    val startDestinationEntry = remember(navController) {
        navController.getBackStackEntry(Screen.Politics.route)
    }
    return hiltViewModel(startDestinationEntry)
}
