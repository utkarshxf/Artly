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
        val storage = SecureStorage(this)
        val token = storage.getToken()
        val userId = storage.getUserId()
        val loggedIn = !token.isNullOrBlank() && !userId.isNullOrBlank()
        // The cached profile is only filled once Home loads it, so it must not decide whether the user is logged in.
        // Profile details left over from a different account are dropped; Home fetches the right ones.
        val cachedDetailsId = storage.getUserDetails()?.id
        if (loggedIn && cachedDetailsId != null && cachedDetailsId != userId) {
            storage.clearUserDetails()
        }
        installSplashScreen()
        setContent {
            val startDestination = if (loggedIn) {
                Screens.Home.route
            } else {
                storage.clearSharedPref()
                Screens.Signup.route
            }
            Log.d("TAG", "onCreate: $userId $cachedDetailsId $startDestination")
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

