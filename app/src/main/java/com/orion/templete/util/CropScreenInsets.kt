package com.orion.templete.util

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

// Android 15+ draws every screen edge-to-edge (and from SDK 36 apps can no longer opt out). Screens from libraries
// predate that: uCrop's toolbar (with the ✓ button) ended up under the status bar, and the same happens to the
// Freshchat support screens. Pad their content by the system bars; the crop screen also gets its dark bar colour.
// The app's own MainActivity (Compose) handles insets itself.
object CropScreenInsets : Application.ActivityLifecycleCallbacks {

    private const val UCROP_ACTIVITY = "com.yalantis.ucrop.UCropActivity"
    private const val APP_PACKAGE = "com.orion.templete."
    private const val ENFORCED_EDGE_TO_EDGE_SDK = 35 // Build.VERSION_CODES.VANILLA_ICE_CREAM (compileSdk is 34)

    override fun onActivityPostCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT < ENFORCED_EDGE_TO_EDGE_SDK) return
        if (activity.javaClass.name.startsWith(APP_PACKAGE)) return
        val isCrop = activity.javaClass.name == UCROP_ACTIVITY
        val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        if (isCrop) content.setBackgroundColor(ImageCropper.CROP_BAR_COLOR)
        ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.requestApplyInsets(content)
        if (isCrop) {
            // light icons on the dark bars
            WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
