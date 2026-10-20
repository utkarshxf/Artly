package com.orion.templete.data.chat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

// Compresses a picked photo (max 1600 px on the long side, JPEG 82, EXIF orientation applied) and uploads it to
// Storage at chat/{conversationId}/{me}/{uuid}.jpg. Returns (downloadUrl, width, height) of the uploaded image.
@Singleton
class ChatMediaUploader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storage: FirebaseStorage,
) {
    private class Compressed(val bytes: ByteArray, val width: Int, val height: Int)

    suspend fun upload(
        conversationId: String,
        me: String,
        uri: Uri,
        onProgress: (Float) -> Unit,
    ): Triple<String, Int, Int> {
        onProgress(0f)
        val image = withContext(Dispatchers.IO) { compress(uri) }
        val ref = storage.reference.child("chat/$conversationId/$me/${UUID.randomUUID()}.jpg")
        val metadata = StorageMetadata.Builder().setContentType("image/jpeg").build()
        val task = ref.putBytes(image.bytes, metadata)
        task.addOnProgressListener { snapshot ->
            val total = snapshot.totalByteCount
            if (total > 0) onProgress((snapshot.bytesTransferred.toFloat() / total).coerceIn(0f, 0.99f))
        }
        try {
            task.await()
        } catch (e: CancellationException) {
            task.cancel()
            throw e
        }
        val url = ref.downloadUrl.await().toString()
        onProgress(1f)
        return Triple(url, image.width, image.height)
    }

    private fun compress(uri: Uri): Compressed {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // bounds-only decoding always returns null; success shows up as outWidth/outHeight
        val input = try {
            resolver.openInputStream(uri)
        } catch (e: Exception) {
            throw IOException("Couldn't open the selected photo", e)
        } ?: throw IOException("Couldn't open the selected photo")
        input.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("This photo format isn't supported")

        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2

        val decoded = decode(uri, sample)
        val (rotation, flipped) = orientation(uri)
        val longSide = max(decoded.width, decoded.height)
        val scale = if (longSide > MAX_SIDE) MAX_SIDE.toFloat() / longSide else 1f

        // ExifInterface describes orientations 2, 4, 5 and 7 as "mirror horizontally, then rotate by
        // rotationDegrees", so the mirror has to be applied before the rotation
        val matrix = Matrix()
        if (scale < 1f) matrix.postScale(scale, scale)
        if (flipped) matrix.postScale(-1f, 1f)
        if (rotation != 0) matrix.postRotate(rotation.toFloat())

        var bitmap = decoded
        if (!matrix.isIdentity) {
            bitmap = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            if (bitmap !== decoded) decoded.recycle()
        }
        if (bitmap.hasAlpha()) {
            // JPEG has no transparency: put transparent PNGs/WebPs on white instead of black
            val opaque = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            Canvas(opaque).apply {
                drawColor(Color.WHITE)
                drawBitmap(bitmap, 0f, 0f, null)
            }
            bitmap.recycle()
            bitmap = opaque
        }

        val out = ByteArrayOutputStream()
        if (!bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)) {
            bitmap.recycle()
            throw IOException("Couldn't compress the photo")
        }
        val result = Compressed(out.toByteArray(), bitmap.width, bitmap.height)
        bitmap.recycle()
        return result
    }

    private fun decode(uri: Uri, sample: Int): Bitmap {
        var inSample = sample
        repeat(3) {
            val options = BitmapFactory.Options().apply {
                inSampleSize = inSample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            try {
                val bitmap = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }
                return bitmap ?: throw IOException("This photo format isn't supported")
            } catch (e: OutOfMemoryError) {
                inSample *= 2 // Very large photo on a low-memory device: decode smaller
            }
        }
        throw IOException("This photo is too large")
    }

    // (rotation in degrees, mirrored) from the photo's EXIF data; (0, false) when there is none
    private fun orientation(uri: Uri): Pair<Int, Boolean> = try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val exif = ExifInterface(stream)
            exif.rotationDegrees to exif.isFlipped
        } ?: (0 to false)
    } catch (e: Exception) {
        0 to false
    }

    private companion object {
        const val MAX_SIDE = 1600
        const val JPEG_QUALITY = 82
    }
}
