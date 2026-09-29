package com.orion.templete

import android.content.Intent
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
import com.orion.templete.data.model.chat.ChatDeepLink
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

    // True when this copy only handed a chat notification over to the copy that was already running
    private var forwardedChatDeepLink = false

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

        // A chat notification opens the conversation. Only a fresh launch reads it: after a rotation or process
        // death the back stack (possibly already on that chat) is restored instead.
        if (savedInstanceState == null && handleChatIntent(intent) && !isTaskRoot) {
            // The app was already open underneath (MainActivity uses the standard launch mode): let that copy,
            // which is now collecting ChatDeepLink.pendingPeer, open the chat instead of starting a second app.
            forwardedChatDeepLink = true
            finish()
            @Suppress("DEPRECATION") overridePendingTransition(0, 0)
            return
        }

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

    // A chat notification tapped while this activity is running (singleTop launch or CLEAR_TOP | SINGLE_TOP flags)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleChatIntent(intent)
    }

    // Publishes the peer of a tapped chat notification; Home opens the thread and clears it. Returns true if the
    // intent carried one for a logged-in user.
    private fun handleChatIntent(intent: Intent?): Boolean {
        if (intent == null) return false
        // Reopened from Recents: the notification was already handled when it was tapped
        if ((intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) return false
        val peer = intent.getStringExtra(ChatDeepLink.EXTRA_PEER)?.trim()
        if (peer.isNullOrEmpty()) return false
        intent.removeExtra(ChatDeepLink.EXTRA_PEER)
        val storage = SecureStorage(this)
        val me = storage.getUserId()
        // Logged out (e.g. an old notification after logout): ignore, it must not open a chat for the next account
        if (storage.getToken().isNullOrBlank() || me.isNullOrBlank() || peer == me) return false
        ChatDeepLink.pendingPeer.value = peer
        return true
    }

    fun getImageCropper(): ImageCropper = imageCropper
    fun getGalleryLauncher(): GalleryLauncher = galleryLauncher
    override fun onDestroy() {
        super.onDestroy()
        if (!forwardedChatDeepLink) {
            TrackEvents(this).trackAppClosed()
        }
    }
}
