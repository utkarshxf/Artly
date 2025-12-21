package com.orion.templete.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.Coil
import coil.ImageLoader
import coil.request.ImageRequest
import com.orion.templete.data.model.artwork_model.ArtworkDTO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Utility class for sharing artwork information
 */
object ShareUtils {

    /**
     * Share artwork information with other apps (image required)
     *
     * @param context The context to use for creating the intent
     * @param artwork The artwork to share
     */
    fun shareArtwork(context: Context, artwork: ArtworkDTO) {
        val title = artwork.title ?: "Untitled Artwork"
        val artist = artwork.artist ?: "Unknown Artist"
        val year = artwork.releasedDate?.let { extractYear(it) } ?: ""

        val shareText = buildString {
            append("Check out this amazing artwork!\n\n")
            append("\"$title\" ")
            if (year != null && year.isNotEmpty()) {
                append("($year) ")
            }
            append("by $artist\n\n")

            // Add app download link
            append("\nExplore more artwork on Artistry. Download the app from https://artwrk.studio/")
        }

        // Only share if image URL is available
        if (!artwork.imageUrl.isNullOrEmpty()) {
            shareArtworkWithImage(context, shareText, artwork.imageUrl, title)
        }
        // No fallback - if no image, don't share at all
    }

    private fun shareArtworkWithImage(context: Context, shareText: String, imageUrl: String, title: String) {
        // Use coroutine to handle image download
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val imageUri = downloadAndSaveImage(context, imageUrl, title)
                if (imageUri != null) {
                    shareWithImage(context, shareText, imageUri)
                }
                // No fallback - if image download fails, don't share at all
            } catch (e: Exception) {
                // No fallback - on error, don't share at all
            }
        }
    }
    private suspend fun downloadAndSaveImage(context: Context, imageUrl: String, title: String): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val imageLoader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false)
                    .build()

                val drawable = imageLoader.execute(request).drawable
                val bitmap = (drawable as? BitmapDrawable)?.bitmap ?: return@withContext null

                val fileName = "${title.replace(Regex("[^A-Za-z0-9]"), "_")}_${System.currentTimeMillis()}.jpg"
                val file = File(context.cacheDir, fileName)

                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }

                FileProvider.getUriForFile(
                    context,
                    "com.orion.templete.provider",
                    file
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun shareWithImage(context: Context, shareText: String, imageUri: Uri) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "image/*"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_STREAM, imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserIntent = Intent.createChooser(shareIntent, "Share Artwork")
        ContextCompat.startActivity(context, chooserIntent, null)
    }
}
