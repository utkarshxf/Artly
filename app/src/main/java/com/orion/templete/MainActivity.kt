package com.orion.templete

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.orion.templete.presentation.registration.RegisterViewModel
import com.orion.templete.presentation.ui.theme.TempleteTheme
import com.orion.templete.util.SecureStorage
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val token = SecureStorage(this).getToken()
        installSplashScreen()
        val startDestination = if (token !=null) { "home_screen" } else { "login_screen" }
        setContent {
            TempleteTheme {
                Navigation(startDestination)
            }
        }
    }
}

