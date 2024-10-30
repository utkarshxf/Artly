package com.orion.templete

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.flashcall.me.data.local.AppDatabase
import com.flashcall.me.data.local.dao.UserDao
import com.orion.templete.presentation.common.Screens
import com.orion.templete.presentation.ui.theme.TempleteTheme
import com.orion.templete.presentation.user_register.UserRegisterScreenViewModel
import com.orion.templete.util.SecureStorage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val token = SecureStorage(this).getToken()
        val userId = SecureStorage(this).getUserId()
        installSplashScreen()
        setContent {
            val startDestination = if (token.isNullOrBlank() && userId.isNullOrBlank()) {
                Screens.Login.route
            } else {  Screens.Home.route}
            TempleteTheme {
                Surface() {
                    Navigation(startDestination)
                }
            }
        }
    }
}

