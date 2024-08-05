package com.orion.templete

import androidx.compose.runtime.Composable
import androidx.navigation.Navigation
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.orion.templete.presentation.common.BottomAppNev
import com.orion.templete.presentation.login.LoginScreen
import com.orion.templete.presentation.login.RegisterScreen

@Composable
fun Navigation(startDest :String) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = startDest) {
        composable("login_screen") {
            LoginScreen(navigateToRegisterScreen = {
                navController.navigate("register_screen") {
                    popUpTo("login_screen") { inclusive = true }
                }
            } ,
                navigateToHomeScreen = {
                    navController.navigate("home_screen") {
                        popUpTo("login_screen") { inclusive = true }
                    }
                })
        }
        composable("register_screen") {
            RegisterScreen(
                navigateToLoginScreen = {
                    navController.navigate("login_screen") {
                        popUpTo("login_screen") { inclusive = true }
                    }
                })
        }
        composable("home_screen") {
            BottomAppNev()
        }
    }
}