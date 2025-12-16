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

@AndroidEntryPoint // Hilt를 사용하기 위한 애노테이션
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenManager: TokenManager
    // logoutEvent 담당
    // tokenManager가 logoutEvent.emit(Unit) 같은 걸 해주면 → MainActivity가 잡아서 처리하는 구조

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MyNewsMobileAppFETheme {
                // Composable 재구성(recomposition)에도 동일한 NavController 인스턴스를 유지
                // (단, Activity 재생성까지 영구 유지되는 건 아님)
                val navController = rememberNavController()

                // Composable에서 Flow를 "무한 수집"하는 작업은 side-effect이므로
                // LaunchedEffect 같은 코루틴 스코프에서 수행해야 안전함
                LaunchedEffect(Unit) {
                    // 로그아웃 이벤트 스트림을 계속 구독(수집)하면서 발생 시마다 처리
                    tokenManager.logoutEvent.collect {

                        Toast.makeText(
                            this@MainActivity, // Compose 람다 안에서 Activity 컨텍스트를 명확히 지정
                            "세션이 만료되었습니다. 다시 로그인해주세요.",
                            Toast.LENGTH_LONG
                        ).show()

                        navController.navigate(Screen.Politics.route) {
                            // 뒤로가기 시 인증이 필요한 이전 화면으로 돌아가지 못하게 백스택을 정리
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                            restoreState = false
                        }
                    }
                }

                MainScreen(navController = navController)
                // 이후 -> NavGraph를 띄우거나, BottomNavBar 포함한 메인 UI를 구성
            }
        }
    }
}
