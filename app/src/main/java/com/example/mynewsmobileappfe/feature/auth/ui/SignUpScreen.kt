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

/**
 * ========================================
 * SignUpScreen - 동료 개발자 구현 가이드
 * ========================================
 *
 * [화면 구성 요소]
 * 1. 뒤로가기 버튼 (IconButton)
 * 2. 화면 타이틀 "회원가입"
 * 3. 이메일 입력 필드
 * 4. 비밀번호 입력 필드
 * 5. 비밀번호 확인 입력 필드
 * 6. 회원가입 버튼
 * 7. 로딩 인디케이터
 * 8. 에러/성공 메시지
 *
 * [ViewModel 연동]
 * ```
 * @Composable
 * fun SignUpScreen(
 *     viewModel: AuthViewModel = hiltViewModel(),
 *     onNavigateBack: () -> Unit,
 *     onSignUpSuccess: () -> Unit
 * ) {
 *     val signUpState by viewModel.signUpState.collectAsStateWithLifecycle()
 *
 *     LaunchedEffect(signUpState) {
 *         if (signUpState is AuthState.Success) {
 *             viewModel.resetState()
 *             onSignUpSuccess()  // 로그인 화면으로 이동
 *         }
 *     }
 * }
 * ```
 *
 * [유효성 검사]
 * - 이메일 형식 검사
 * - 비밀번호 최소 8자
 * - 비밀번호 확인 일치 여부
 * - 모든 필드 입력 여부
 *
 * [에러 처리]
 * - 409 Conflict: "이미 사용 중인 이메일입니다."
 * - 400 Bad Request: "입력 정보를 확인해주세요."
 */
@Composable
fun SignUpScreen(
    onNavigateBack: () -> Unit = {},
    onSignUpSuccess: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    val signUpState by viewModel.signUpState.collectAsStateWithLifecycle()

    var emailState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var passwordState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var passwordConfirmState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }

    LaunchedEffect(signUpState) {
        if (signUpState is AuthState.Success) {
            viewModel.resetState()
            onSignUpSuccess()
        }
    }

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
                        text = "회원가입",
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

                    OutlinedTextField(
                        value = passwordConfirmState.value,
                        onValueChange = { passwordConfirmState.value = it },
                        label = { Text("비밀번호 확인") },
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
                        onClick = {
                            if (passwordState.value == passwordConfirmState.value) {
                                viewModel.signUp(emailState.value.trim(), passwordState.value)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = signUpState !is AuthState.Loading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (signUpState is AuthState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            text = "회원가입",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    TextButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "뒤로가기",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (signUpState is AuthState.Error) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = (signUpState as AuthState.Error).message,
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
