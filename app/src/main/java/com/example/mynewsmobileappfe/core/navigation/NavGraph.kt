package com.example.mynewsmobileappfe.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.mynewsmobileappfe.feature.auth.ui.LoginScreen
import com.example.mynewsmobileappfe.feature.auth.ui.SignUpScreen
import com.example.mynewsmobileappfe.feature.bookmark.ui.BookmarkScreen
import com.example.mynewsmobileappfe.feature.news.ui.ArticleDetailScreen
import com.example.mynewsmobileappfe.feature.news.cache.ArticleCache
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
 *
 * [NavController 확장 함수]
 * ```
 * fun NavHostController.navigateToLogin() {
 *     navigate(Screen.Login.route) {
 *         launchSingleTop = true
 *     }
 * }
 *
 * fun NavHostController.navigateToPoliticsAfterAuth() {
 *     navigate(Screen.Politics.route) {
 *         popUpTo(Screen.Login.route) { inclusive = true }
 *     }
 * }
 *
 * fun NavHostController.navigateToLoginAndClearStack() {
 *     navigate(Screen.Login.route) {
 *         popUpTo(0) { inclusive = true }
 *     }
 * }
 * ```
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    isLoggedIn: Boolean,
    onLoginRequired: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Politics.route  // 정치 카테고리에서 시작 (로그인 불필요)
    ) {
        // ===== Auth Screens =====
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToSignUp = {
                    navController.navigate(Screen.SignUp.route)
                },
                onNavigateToHome = {
                    // 로그인 후 정치 카테고리로 이동
                    navController.navigate(Screen.Politics.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.SignUp.route) {
            SignUpScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSignUpSuccess = {
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
            HomeScreen(
                section = politicsSection,
                onLoginRequired = onLoginRequired,
                onArticleClick = { articleId ->
                    navController.navigate(Screen.ArticleDetail.createRoute(articleId))
                }
            )
        }

        // 경제
        composable(Screen.Economy.route) {
            val economySection = Screen.routeToSection(Screen.Economy.route)
            android.util.Log.d("NavGraph", "Economy composable - section: $economySection")
            HomeScreen(
                section = economySection,
                onLoginRequired = onLoginRequired,
                onArticleClick = { articleId ->
                    navController.navigate(Screen.ArticleDetail.createRoute(articleId))
                }
            )
        }

        // 사회
        composable(Screen.Social.route) {
            val socialSection = Screen.routeToSection(Screen.Social.route)
            android.util.Log.d("NavGraph", "Social composable - section: $socialSection")
            HomeScreen(
                section = socialSection,
                onLoginRequired = onLoginRequired,
                onArticleClick = { articleId ->
                    navController.navigate(Screen.ArticleDetail.createRoute(articleId))
                }
            )
        }

        // 기술
        composable(Screen.Technology.route) {
            val technologySection = Screen.routeToSection(Screen.Technology.route)
            android.util.Log.d("NavGraph", "Technology composable - section: $technologySection")
            HomeScreen(
                section = technologySection,
                onLoginRequired = onLoginRequired,
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
                onNavigateToLogin = {
                    // 로그아웃/탈퇴 후 로그인 화면으로 (백스택 클리어)
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
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
            )
        ) { backStackEntry ->
            val articleId = backStackEntry.arguments?.getLong("articleId") ?: 0L
            ArticleDetailScreen(
                articleId = articleId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLoginRequired = onLoginRequired
            )
        }
    }
}
