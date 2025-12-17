package com.example.mynewsmobileappfe

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.example.mynewsmobileappfe.core.jwt.TokenManager
import com.example.mynewsmobileappfe.core.navigation.MainScreen
import com.example.mynewsmobileappfe.core.navigation.Screen
import com.example.mynewsmobileappfe.core.ui.theme.MyNewsMobileAppFETheme
import com.example.mynewsmobileappfe.feature.auth.ui.AuthEffect
import com.example.mynewsmobileappfe.feature.auth.ui.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenManager: TokenManager

    // ✅ Compose가 변경을 감지할 수 있도록 state로 변경
    private var pendingNfcArticleId by mutableStateOf<Long?>(null)

    private fun updatePendingFromIntent(intent: Intent?) {
        val id = intent?.getLongExtra("nfc_article_id", -1L) ?: -1L
        if (id > 0) {
            pendingNfcArticleId = id
            android.util.Log.d("MainActivity", "pendingNfcArticleId set to $id")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        updatePendingFromIntent(intent)
        android.util.Log.d("MainActivity", "onCreate, intent=$intent, data=${intent?.data}")

        setContent {
            MyNewsMobileAppFETheme {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = hiltViewModel()

                // ✅ NFC로 들어온 기사ID가 있으면 상세 화면으로 이동 (앱 켜져있든/꺼져있든 동일)
                LaunchedEffect(pendingNfcArticleId) {
                    val nfcId = pendingNfcArticleId ?: return@LaunchedEffect
                    android.util.Log.d("MainActivity", "NFC navigate to articleId=$nfcId")

                    navController.navigate(Screen.ArticleDetail.createRoute(nfcId)) {
                        launchSingleTop = true
                    }

                    // 중복 이동 방지
                    pendingNfcArticleId = null
                }

                // AuthEffect 전역 처리
                LaunchedEffect(Unit) {
                    authViewModel.effect.collect { effect ->
                        when (effect) {
                            AuthEffect.NavigateHome -> {
                                if (pendingNfcArticleId != null) {
                                    // NFC 이동이 우선이므로 여기선 아무 것도 하지 않음
                                    return@collect
                                }
                                // ✅ NFC 이동은 위 LaunchedEffect가 전담하므로 여기선 홈 네비게이션만
                                navController.navigate(Screen.Politics.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }

                            AuthEffect.NavigateToAuthScreen -> {
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }

                            is AuthEffect.Toast -> {
                                Toast.makeText(
                                    this@MainActivity,
                                    effect.message,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }

                // logoutEvent 처리
                LaunchedEffect(Unit) {
                    tokenManager.logoutEvent.collect {
                        Toast.makeText(
                            this@MainActivity,
                            "세션이 만료되었습니다. 다시 로그인해주세요.",
                            Toast.LENGTH_LONG
                        ).show()

                        navController.navigate(Screen.Login.route) {
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        updatePendingFromIntent(intent)
    }
}
