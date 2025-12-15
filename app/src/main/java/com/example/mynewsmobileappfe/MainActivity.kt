package com.example.mynewsmobileappfe

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.rememberNavController
import com.example.mynewsmobileappfe.core.datastore.TokenManager
import com.example.mynewsmobileappfe.core.navigation.MainScreen
import com.example.mynewsmobileappfe.core.navigation.Screen
import com.example.mynewsmobileappfe.core.ui.theme.MyNewsMobileAppFETheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MyNewsMobileAppFETheme {
                val navController = rememberNavController()

                // 강제 로그아웃 이벤트 처리
                LaunchedEffect(Unit) {
                    tokenManager.logoutEvent.collect {
                        Toast.makeText(
                            this@MainActivity,
                            "세션이 만료되었습니다. 다시 로그인해주세요.",
                            Toast.LENGTH_LONG
                        ).show()

                        navController.navigate(Screen.Politics.route) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                            restoreState = false
                        }
                    }
                }

                MainScreen(navController = navController)
            }
        }
    }
}
