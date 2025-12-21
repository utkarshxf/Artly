package com.orion.templete

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.orion.templete.presentation.auth.AuthViewModel
import com.orion.templete.presentation.auth.forgetpassword.ForgetPasswordScreen
import com.orion.templete.presentation.auth.login.Login
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
        composable(Screens.Home.route) {
            Home(navigateToLoginScreen = {
                navController.navigate(Screens.Login.route) {
                    popUpTo(Screens.Home.route) { inclusive = true }
                }
            })
        }
        composable(Screens.ForgetPassword.route) {
            val viewModel: AuthViewModel = hiltViewModel()
            ForgetPasswordScreen(
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
                        popUpTo(Screens.ForgetPassword.route) { inclusive = true }
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
