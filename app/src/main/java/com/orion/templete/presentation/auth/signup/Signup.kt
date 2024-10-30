package com.orion.templete.presentation.auth.signup

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.orion.templete.presentation.auth.AuthViewModel
import com.orion.templete.presentation.common.Screens

@Composable
fun Signup(
    navController: NavController
) {
    val viewModel: AuthViewModel = hiltViewModel()
    SignupScreen(
        uiState = viewModel.signupData,
        signupUser = { viewModel.signup(it) },
        onNavigateToLogin = {
            navController.navigate(Screens.Login.route) {
                popUpTo(Screens.Signup.route) { inclusive = true }
            }
        },
        onNavigateToRegister = {
            navController.navigate(Screens.UserRegister.route) {
                popUpTo(Screens.Signup.route) { inclusive = true }
            }
        }
    )
}