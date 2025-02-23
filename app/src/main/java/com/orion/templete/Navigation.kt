package com.orion.templete

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.orion.templete.presentation.auth.login.Login
import com.orion.templete.presentation.common.BottomAppNev
import com.orion.templete.presentation.auth.login.LoginScreen
import com.orion.templete.presentation.auth.signup.Signup
import com.orion.templete.presentation.common.Screens
import com.orion.templete.presentation.user_register.UserRegisterScreen

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun Navigation(startDest :String) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = startDest) {
        composable(Screens.Login.route) {
            Login(navController = navController)
        }
        composable(Screens.Signup.route) {
            Signup(navController = navController)
        }
        composable(Screens.UserRegister.route) {
            UserRegisterScreen(onNavigateToHome = {
                navController.navigate(Screens.Home.route) {
                    popUpTo(Screens.UserRegister.route) { inclusive = true }
                }
            })
        }
        composable(Screens.Home.route) {
            BottomAppNev(
                navigateToLoginScreen = {
                navController.navigate(Screens.Login.route) {
                    popUpTo(Screens.Home.route) { inclusive = true }
                }
            })
        }

    }
}