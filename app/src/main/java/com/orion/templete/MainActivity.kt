package com.orion.templete

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.orion.templete.util.GalleryLauncher
import com.orion.templete.presentation.common.Screens
import com.orion.templete.presentation.ui.theme.TempleteTheme
import com.orion.templete.util.ImageCropper
import com.orion.templete.util.SecureStorage
import com.orion.templete.util.TrackEvents
import dagger.hilt.android.AndroidEntryPoint

@RequiresApi(Build.VERSION_CODES.O)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var imageCropper = ImageCropper(this)
    private var galleryLauncher = GalleryLauncher(this, imageCropper)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val token = SecureStorage(this).getToken()
        val userId = SecureStorage(this).getUserId()
        val currentUserId = SecureStorage(this).getUserDetails()?.id
        installSplashScreen()
        setContent {
            val startDestination = if (token.isNullOrBlank() || userId.isNullOrBlank() || userId != currentUserId) {
                SecureStorage(this).clearSharedPref()
                Screens.Signup.route
            } else {
                Screens.Home.route
            }
            Log.d("TAG", "onCreate: $userId   $currentUserId  $startDestination ${!userId.equals(currentUserId)}")
            TempleteTheme {
//                WindowCompat.setDecorFitsSystemWindows(window, false)
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Android 15+ draws apps edge-to-edge; keep every screen clear of the status and navigation bars
                    Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                        Navigation(startDestination , this@MainActivity)
                    }
                }
            }
        }
    }
    fun getImageCropper(): ImageCropper = imageCropper
    fun getGalleryLauncher(): GalleryLauncher = galleryLauncher
    override fun onDestroy() {
        super.onDestroy()
        TrackEvents(this).trackAppClosed()
    }
}

