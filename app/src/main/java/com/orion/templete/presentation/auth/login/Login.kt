package com.orion.templete.presentation.auth.login

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.orion.templete.presentation.auth.AuthViewModel
import com.orion.templete.presentation.auth.PhoneLoginUiState
import com.orion.templete.presentation.auth.components.findActivity
import com.orion.templete.presentation.common.Screens

@Composable
fun Login(
    navController: NavController
) {
    val viewMode: AuthViewModel = hiltViewModel()
    val context = LocalContext.current
    val googleState by viewMode.phoneLoginState.collectAsState()

    // "Continue with Google": existing account -> Home, new account -> pick username and password
    LaunchedEffect(googleState) {
        when (val state = googleState) {
            is PhoneLoginUiState.LoggedIn -> navController.navigate(Screens.Home.route) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
            is PhoneLoginUiState.NeedsAccount -> {
                viewMode.resetPhoneLoginState()
                navController.navigate(Screens.CreateAccount.route)
            }
            is PhoneLoginUiState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewMode.resetPhoneLoginState()
            }
            else -> {}
        }
    }

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
                popUpTo(navController.graph.id) { inclusive = true }
            }
        },
        onNavigateToForgetPassword = {
            navController.navigate(Screens.ForgetPassword.route){
//                popUpTo(Screens.Login.route){inclusive = true}
            }
        },
        onGoogleSignIn = { context.findActivity()?.let { viewMode.signInWithGoogle(it) } },
        googleLoading = googleState is PhoneLoginUiState.Loading
    )
}
