package com.example.mynewsmobileappfe

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.mynewsmobileappfe.core.datastore.TokenManager
import com.example.mynewsmobileappfe.core.navigation.BottomNavBar
import com.example.mynewsmobileappfe.core.navigation.NavGraph
import com.example.mynewsmobileappfe.core.navigation.Screen
import com.example.mynewsmobileappfe.core.ui.theme.MyNewsMobileAppFETheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.mynewsmobileappfe.feature.auth.ui.AuthViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyNewsMobileAppFETheme {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = hiltViewModel()
                val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route

                /**
                 * 강제 로그아웃 이벤트 처리
                 * - 토큰 만료 / 재발급 실패 시 TokenManager가 emit
                 * - 안내 메시지 출력 후 Home으로 이동
                 * - 백스택 정리 + Home 중복 방지
                 */
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    tokenManager.logoutEvent.collect {
                        Toast.makeText(
                            this@MainActivity,
                            "세션이 만료되었습니다. 다시 로그인해주세요.",
                            Toast.LENGTH_LONG
                        ).show()

                        navController.navigate(Screen.Home.route) {
                            // NavGraph의 시작 지점까지 pop
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = true
                            }
                            launchSingleTop = true
                            restoreState = false
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (currentRoute in Screen.bottomNavScreens.map { it.route }) {
                            BottomNavBar(
                                currentRoute = currentRoute,
                                onNavigate = { screen ->
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.Home.route) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                },
                                isLoggedIn = isLoggedIn,
                                onLoginRequired = {
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
                            navController.navigate(Screen.Login.route) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        }
    }
}
