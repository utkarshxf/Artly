package com.orion.templete.presentation.auth.login

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@Composable
fun Login(
    navController: NavController
) {
    val viewMode: AuthViewModel = hiltViewModel()
    LoginScreen(
        uiState = viewMode.signingData,
        loginUser = { viewMode.loginUser(it) },
        onNavigateToSignup = {
            navController.navigate("signup_screen") {
                popUpTo("login_screen") { inclusive = true }
            }
        },
        onNavigateToHome = {
            navController.navigate("home_screen") {
                popUpTo("login_screen") { inclusive = true }
            }
        }
    )
}