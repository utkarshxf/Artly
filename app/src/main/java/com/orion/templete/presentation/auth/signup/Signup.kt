package com.orion.templete.presentation.auth.signup

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.orion.templete.presentation.auth.login.AuthViewModel

@Composable
fun Signup(
    navController: NavController
) {
    val viewModel: AuthViewModel = hiltViewModel()
    SignupScreen(
        uiState = viewModel.signupData,
        signupUser = { viewModel.signup(it) },
        onNavigateToLogin = {
            navController.navigate("login_screen") {
                popUpTo("signup_screen") { inclusive = true }
            }
        },
        onNavigateToHome = {
            navController.navigate("register_screen") {
                popUpTo("signup_screen") { inclusive = true }
            }
        }
    )
}