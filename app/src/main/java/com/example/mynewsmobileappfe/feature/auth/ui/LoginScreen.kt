package com.example.mynewsmobileappfe.feature.auth.ui

import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mynewsmobileappfe.core.ui.theme.SamsungGradientEnd
import com.example.mynewsmobileappfe.core.ui.theme.SamsungGradientStart
import androidx.compose.runtime.*

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

    var emailState = remember { mutableStateOf("") }
    var passwordState = remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 헤더 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    SamsungGradientStart,
                                    SamsungGradientEnd
                                )
                            )
                        )
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "로그인",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.surface,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 입력 폼 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = emailState.value,
                        onValueChange = { emailState.value = it },
                        label = { Text("이메일") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    OutlinedTextField(
                        value = passwordState.value,
                        onValueChange = { passwordState.value = it },
                        label = { Text("비밀번호") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = { viewModel.login(emailState.value.trim(), passwordState.value) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = loginState !is AuthState.Loading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (loginState is AuthState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            text = "로그인",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    TextButton(
                        onClick = onNavigateToSignUp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "회원가입",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (loginState is AuthState.Error) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = (loginState as AuthState.Error).message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
