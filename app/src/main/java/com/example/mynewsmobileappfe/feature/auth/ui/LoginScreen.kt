package com.example.mynewsmobileappfe.feature.auth.ui

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * ========================================
 * LoginScreen - 동료 개발자 구현 가이드
 * ========================================
 *
 * [화면 구성 요소]
 * 1. 앱 로고 또는 타이틀
 * 2. 이메일 입력 필드 (OutlinedTextField)
 * 3. 비밀번호 입력 필드 (OutlinedTextField, visualTransformation)
 * 4. 로그인 버튼 (Button)
 * 5. 회원가입 화면으로 이동 링크 (TextButton)
 * 6. 로딩 인디케이터 (CircularProgressIndicator)
 * 7. 에러 메시지 표시 (Snackbar 또는 Text)
 *
 * [ViewModel 연동]
 * ```
 * @Composable
 * fun LoginScreen(
 *     viewModel: AuthViewModel = hiltViewModel(),
 *     onNavigateToSignUp: () -> Unit,
 *     onNavigateToHome: () -> Unit
 * ) {
 *     val loginState by viewModel.loginState.collectAsStateWithLifecycle()
 *
 *     LaunchedEffect(loginState) {
 *         if (loginState is AuthState.Success) {
 *             viewModel.resetState()
 *             onNavigateToHome()
 *         }
 *     }
 *
 *     // UI 구현...
 * }
 * ```
 *
 * [유효성 검사]
 * - 이메일 형식 검사: android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
 * - 비밀번호 최소 길이: 8자 이상 권장
 *
 * [참고]
 * - Material3 컴포넌트 사용
 * - 키보드 액션 설정 (imeAction)
 * - 포커스 관리 (FocusRequester)
 */
@Composable
fun LoginScreen(
    onNavigateToSignUp: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    val loginState by viewModel.loginState.collectAsStateWithLifecycle()
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()

    LaunchedEffect(loginState, isLoggedIn) {
        if (loginState is AuthState.Success || isLoggedIn) {
            viewModel.resetState()
            onNavigateToHome()
        }
    }

    var emailState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var passwordState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("로그인", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = emailState.value,
            onValueChange = { emailState.value = it },
            label = { Text("이메일") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = passwordState.value,
            onValueChange = { passwordState.value = it },
            label = { Text("비밀번호") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { viewModel.login(emailState.value.trim(), passwordState.value) },
            modifier = Modifier.fillMaxWidth(),
            enabled = loginState !is AuthState.Loading
        ) {
            if (loginState is AuthState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(8.dp))
            }
            Text("로그인")
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onNavigateToSignUp) {
            Text("회원가입")
        }

        if (loginState is AuthState.Error) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = (loginState as AuthState.Error).message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
