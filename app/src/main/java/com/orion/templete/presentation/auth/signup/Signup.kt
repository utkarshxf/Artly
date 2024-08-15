package com.orion.templete.presentation.auth.signup

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.orion.templete.data.model.User
import com.orion.templete.presentation.auth.common.authViewModel

@Composable
fun Signup(
    navController: NavController
) {
    val viewModel: authViewModel = hiltViewModel()
    SignupScreen(
        uiState = viewModel.signupData,
        signupUser = { viewModel.signup(it) },
        onNavigateToLogin = {
            navController.navigate("login_screen") {
                popUpTo("signup_screen") { inclusive = true }
            }
        },
        onNavigateToHome = {
            navController.navigate("home_screen") {
                popUpTo("signup_screen") { inclusive = true }
            }
        }
    )
}