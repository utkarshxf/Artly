package com.orion.templete.presentation.auth.login

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.orion.templete.presentation.auth.AuthViewModel
import com.orion.templete.presentation.common.Screens

@Composable
fun Login(
    navController: NavController
) {
    val viewMode: AuthViewModel = hiltViewModel()
    val context = LocalContext.current
    LoginScreen(
        uiState = viewMode.signingData,
        loginUser = { viewMode.loginUser(it) },
        onNavigateToSignup = {
            navController.navigate(Screens.Signup.route) {
                popUpTo(Screens.Login.route) { inclusive = true }
            }
        },
        onNavigateToHome = {
            navController.navigate(Screens.Home.route) {
                popUpTo(Screens.Login.route) { inclusive = true }
            }
        }
    )
}