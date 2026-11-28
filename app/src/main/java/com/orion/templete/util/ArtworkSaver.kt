package com.orion.templete.util

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * Downloads and saves high-resolution artwork to the user's gallery.
 *
 * Used only for works the backend marks as saveable (`can_save = true`,
 * public-domain / CC0 only, decided server-side in RightsPolicy). The client
 * never parses licence strings; a missing flag means no save.
 *
 * Bytes are streamed to a cache file first and copied from there into
 * MediaStore, so the original is never decoded into a Bitmap (museum
 * originals can be 6,000-12,000 px, hundreds of MB decoded). Bytes are
 * copied, never re-encoded, to keep embedded ICC profiles.
 */
object ArtworkSaver {

    private val client = OkHttpClient()

    suspend fun downloadToCache(ctx: Context, url: String): File = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Download failed (HTTP ${response.code})")
            val out = File(ctx.cacheDir, "artwork_${System.currentTimeMillis()}.img")
            response.body.byteStream().use { input ->
                out.outputStream().use { input.copyTo(it) }
            }
            out
        }
    }

    fun fileNameFor(title: String?, imageUrl: String): String {
        val base = (title ?: "artwork")
            .replace(Regex("[^A-Za-z0-9 _-]"), "")
            .trim()
            .replace(' ', '_')
            .ifBlank { "artwork" }
            .take(60)
        val ext = imageUrl.substringAfterLast('.', "jpg").take(5).lowercase()
            .let { if (it in listOf("jpg", "jpeg", "png", "webp")) it else "jpg" }
        return "$base.$ext"
    }

    private fun mimeFor(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "png" -> "image/png"
        "webp" -> "image/webp"
        else -> "image/jpeg"
    }

    suspend fun saveToGallery(ctx: Context, src: File, name: String): Uri = withContext(Dispatchers.IO) {
        val appCtx = ctx.applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = appCtx.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, mimeFor(name))
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Artistry")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values
            ) ?: error("MediaStore insert failed")
            try {
                resolver.openOutputStream(uri)!!.use { out -> src.inputStream().use { it.copyTo(out) } }
                resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
                uri
            } catch (t: Throwable) {
                resolver.delete(uri, null, null) // don't leave a stuck pending row
                throw t
            }
        } else {
            // API 24-28: WRITE_EXTERNAL_STORAGE (maxSdkVersion 28), requested at runtime
            @Suppress("DEPRECATION")
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "Artistry"
            ).apply { mkdirs() }
            val dest = File(dir, name).also { src.copyTo(it, overwrite = true) }
            MediaScannerConnection.scanFile(appCtx, arrayOf(dest.path), arrayOf(mimeFor(name)), null)
            Uri.fromFile(dest)
        }
    }
}
