package com.orion.templete

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.orion.templete.presentation.auth.login.Login
import com.orion.templete.presentation.common.BottomAppNev
import com.orion.templete.presentation.auth.login.LoginScreen
import com.orion.templete.presentation.auth.signup.Signup

@Composable
fun Navigation(startDest :String) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = startDest) {
        composable("login_screen") {
            Login(navController = navController)
        }
        composable("register_screen") {
            Signup(navController = navController)
        }
        composable("home_screen") {
            BottomAppNev(navigateToLoginScreen = {
                navController.navigate("login_screen") {
                    popUpTo("home_screen") { inclusive = true }
                }
            })
        }
    }
}