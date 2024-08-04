package com.orion.templete

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.orion.templete.presentation.login.LoginScreen
import com.orion.templete.presentation.login.RegisterScreen
import com.orion.templete.presentation.ui.theme.TempleteTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            TempleteTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "login_screen") {
                        composable("login_screen") {
                            LoginScreen(navigateToRegisterScreen = {
                                navController.navigate("register_screen") {
                                    popUpTo("login_screen") { inclusive = true }
                                }
                            })
                        }
                        composable("register_screen") {
                            RegisterScreen(navigateToLoginScreen = {
                                navController.navigate("login_screen") {
                                    popUpTo("login_screen") { inclusive = true }
                                }
                            })
                        }
                    }
                }
            }
        }
    }
}

