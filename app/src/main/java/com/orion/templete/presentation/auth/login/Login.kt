package com.orion.templete.presentation.auth.login

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.orion.templete.presentation.auth.common.authViewModel

@Composable
fun Login(
    navController: NavController
) {
    val viewMode: authViewModel = hiltViewModel()
    LoginScreen(
        uiState = viewMode.signingData,
        loginUser = { viewMode.loginUser(it) },
        onNavigateToSignup = {
            navController.navigate("register_screen") {
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