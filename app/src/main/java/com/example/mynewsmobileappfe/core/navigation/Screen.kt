package com.example.mynewsmobileappfe.core.navigation

import com.example.mynewsmobileappfe.feature.news.domain.model.Section

/**
 * 앱 내 화면 라우트 정의
 */
sealed class Screen(val route: String) {
    // Auth
    object Login : Screen("login")
    object SignUp : Screen("signup")

    // News Categories (Bottom Navigation)
    object Politics : Screen("politics")
    object Economy : Screen("economy")
    object Social : Screen("social")
    object Technology : Screen("technology")

    // Profile (Bottom Navigation)
    object Profile : Screen("profile")

    // Bookmark (Profile에서 접근)
    object Bookmark : Screen("bookmark")

    // Article Detail
    object ArticleDetail : Screen("article/{articleId}") {
        fun createRoute(articleId: Long) = "article/$articleId"
    }

    companion object {
        // 로그인이 필요한 화면 목록 (북마크는 프로필에서 접근, 프로필만 로그인 필요)
        val authRequiredScreens = listOf(Profile.route)

        // Bottom Navigation에 표시될 화면 (4개 카테고리 + 프로필)
        val bottomNavScreens = listOf(Politics, Economy, Social, Technology, Profile)

        // 카테고리 화면 목록
        val categoryScreens = listOf(Politics, Economy, Social, Technology)

        // Screen to Section 매핑
        fun screenToSection(screen: Screen): Section? = when (screen) {
            Politics -> Section.POLITICS
            Economy -> Section.ECONOMY
            Social -> Section.SOCIAL
            Technology -> Section.TECHNOLOGY
            else -> null
        }

        // route to Section 매핑
        fun routeToSection(route: String?): Section? = when (route) {
            Politics.route -> Section.POLITICS
            Economy.route -> Section.ECONOMY
            Social.route -> Section.SOCIAL
            Technology.route -> Section.TECHNOLOGY
            else -> null
        }
    }
}
