package com.orion.templete

import android.os.Build
import androidx.annotation.RequiresApi
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.orion.templete.presentation.auth.PhoneLoginUiState
import com.orion.templete.presentation.auth.components.findActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.orion.templete.presentation.auth.AuthViewModel
import com.orion.templete.presentation.auth.forgetpassword.ForgetPasswordScreen
import com.orion.templete.presentation.auth.login.Login
import com.orion.templete.presentation.auth.signup.CreateAccount
import com.orion.templete.presentation.auth.signup.Signup
import com.orion.templete.presentation.artist_register.EditArtistScreen
import com.orion.templete.presentation.home.Home
import com.orion.templete.presentation.common.Screens

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun Navigation(startDest: String, activity: MainActivity) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = startDest) {
        composable(Screens.Login.route) {
            Login(navController = navController)
        }
        composable(Screens.Signup.route) {
            Signup(navController = navController, activity)
        }
        composable(Screens.CreateAccount.route) {
            CreateAccount(navController = navController)
        }
        composable(Screens.Home.route) {
            // after logout, start again from the phone-number step
            Home(navigateToLoginScreen = {
                navController.navigate(Screens.Signup.route) {
                    popUpTo(Screens.Home.route) { inclusive = true }
                }
            })
        }
        composable(Screens.ForgetPassword.route) {
            val viewModel: AuthViewModel = hiltViewModel()
            val context = LocalContext.current
            val googleState by viewModel.phoneLoginState.collectAsState()
            // "Continue with Google" instead of resetting the password
            LaunchedEffect(googleState) {
                when (val state = googleState) {
                    is PhoneLoginUiState.LoggedIn -> navController.navigate(Screens.Home.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                    is PhoneLoginUiState.NeedsAccount -> {
                        viewModel.resetPhoneLoginState()
                        navController.navigate(Screens.CreateAccount.route)
                    }
                    is PhoneLoginUiState.Error -> {
                        Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                        viewModel.resetPhoneLoginState()
                    }
                    else -> {}
                }
            }
            ForgetPasswordScreen(
                onGoogleSignIn = { context.findActivity()?.let { viewModel.signInWithGoogle(it) } },
                googleLoading = googleState is PhoneLoginUiState.Loading,
                uiState = viewModel.forgetPasswordData,
                forgetPassword = { phoneNumber, newPassword ->
                    viewModel.forgetPassword(phoneNumber, newPassword)
                },
                onNavigateToLogin = {
                    navController.navigate(Screens.Login.route) {
                        popUpTo(Screens.ForgetPassword.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screens.Home.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
                createUserWithPhone = { phone, act ->
                    viewModel.createUserWithPhone(phone, act)
                },
                signInWithCredential = { otp ->
                    viewModel.signInWithCredential(otp)
                },
                authState = viewModel.authState.collectAsState().value,
                otpState = viewModel.otpState.collectAsState().value
            )
        }
    }
}
