package com.orion.templete.presentation.chat.inbox

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

internal const val CHAT_PREFS = "chat_prefs"
private const val KEY_NOTIFICATION_PERMISSION_ASKED = "notification_permission_asked"

// Android 13+: ask for POST_NOTIFICATIONS the first time the inbox opens, once per install. The "asked" flag is
// stored before the system dialog shows, so a rotation or process death while it is open never asks twice.
@Composable
internal fun RequestChatNotificationPermissionOnce() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        val prefs = context.applicationContext.getSharedPreferences(CHAT_PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_NOTIFICATION_PERMISSION_ASKED, false)) return@LaunchedEffect
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            prefs.edit().putBoolean(KEY_NOTIFICATION_PERMISSION_ASKED, true).apply()
            return@LaunchedEffect
        }
        // Let the inbox appear first instead of opening on top of a system dialog
        delay(600)
        prefs.edit().putBoolean(KEY_NOTIFICATION_PERMISSION_ASKED, true).apply()
        try {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } catch (e: IllegalStateException) {
            // No activity result registry available (e.g. preview); nothing to ask
        } catch (e: ActivityNotFoundException) {
            // Some devices have no permission controller UI
        }
    }
}
