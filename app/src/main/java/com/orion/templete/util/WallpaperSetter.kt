package com.orion.templete.util

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Sets a downloaded artwork file as the device wallpaper.
 *
 * Used only for works the backend marks as wallpaper-eligible
 * (`can_set_wallpaper = true`, public-domain / CC0 only, decided server-side).
 *
 * The source is never decoded at full size: museum originals can be
 * 6,000-12,000 px, and passing a huge bitmap to the wallpaper service can put
 * SystemUI into a crash loop on some devices. We decode bounds first, then
 * downsample with inSampleSize so the long edge lands around 2x the screen.
 */
object WallpaperSetter {

    suspend fun setFromFile(ctx: Context, src: File): Unit = withContext(Dispatchers.IO) {
        val appCtx = ctx.applicationContext
        val wm = WallpaperManager.getInstance(appCtx)
        if (!wm.isWallpaperSupported) error("This device does not support setting wallpapers")
        if (!wm.isSetWallpaperAllowed) error("Setting wallpapers is not allowed on this device")

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(src.absolutePath, bounds)
        val srcW = bounds.outWidth
        val srcH = bounds.outHeight
        if (srcW <= 0 || srcH <= 0) error("Could not decode the downloaded image")

        val metrics = appCtx.resources.displayMetrics
        val targetW = metrics.widthPixels * 2
        val targetH = metrics.heightPixels * 2
        var sample = 1
        while (srcW / (sample * 2) >= targetW && srcH / (sample * 2) >= targetH) sample *= 2

        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = BitmapFactory.decodeFile(src.absolutePath, opts)
            ?: error("Could not decode the downloaded image")
        try {
            // null crop rect: the system center-crops to the screen aspect ratio
            wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
        } finally {
            bitmap.recycle()
        }
    }
}
