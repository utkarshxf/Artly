package com.orion.templete.work

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.orion.templete.R
import com.orion.templete.data.network.ApiService
import com.orion.templete.util.SecureStorage
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Posts the daily "artwork of the day" notification with a one-line story.
 *
 * Periodic WorkManager, no exact alarms, no battery-optimization exemption
 * (Play policy). The worker tries one quick fetch of today's pick and falls
 * back to 30 entries bundled in assets (rotated by day of year) when offline,
 * so the notification still fires with no network.
 */
class DailyArtworkNotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun apiService(): ApiService
    }

    private data class DailyEntry(
        val title: String,
        val artist: String,
        val story: String,
        val imageUrl: String
    )

    override suspend fun doWork(): Result {
        val entry = fetchToday() ?: fallbackEntry()
        postNotification(entry)
        return Result.success()
    }

    private suspend fun fetchToday(): DailyEntry? {
        return try {
            val deps = EntryPointAccessors.fromApplication(applicationContext, Deps::class.java)
            val userId = SecureStorage(applicationContext).getUserId()?.takeIf { it.isNotBlank() } ?: "guest"
            val response = deps.apiService().getRecommendedForToday(userId, 0, 1)
            val artwork = response.body()?.firstOrNull() ?: return null
            val desc = artwork.description?.trim().orEmpty()
            var story = desc.substringBefore(". ").trim()
            if (story.isNotEmpty() && !story.endsWith(".")) story += "."
            DailyEntry(
                title = artwork.title ?: return null,
                artist = artwork.artist ?: "",
                story = story.take(220),
                imageUrl = artwork.image_url_compressed ?: artwork.imageUrl ?: ""
            )
        } catch (t: Throwable) {
            null
        }
    }

    private fun fallbackEntry(): DailyEntry {
        val json = applicationContext.assets.open(FALLBACK_ASSET).bufferedReader().use { it.readText() }
        val array = JSONArray(json)
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val obj = array.getJSONObject(dayOfYear % array.length())
        return DailyEntry(
            title = obj.optString("title"),
            artist = obj.optString("artist"),
            story = obj.optString("story"),
            imageUrl = obj.optString("image")
        )
    }

    private fun loadThumbnail(url: String): Bitmap? {
        if (url.isBlank()) return null
        return try {
            val request = Request.Builder().url(url).build()
            OkHttpClient().newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val bytes = response.body.bytes()
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                var sample = 1
                while (bounds.outWidth / (sample * 2) >= 1024) sample *= 2
                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            }
        } catch (t: Throwable) {
            null
        }
    }

    private fun postNotification(entry: DailyEntry) {
        val manager = NotificationManagerCompat.from(applicationContext)
        if (!manager.areNotificationsEnabled()) return

        val launch = applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)
        val contentIntent = PendingIntent.getActivity(
            applicationContext, 0, launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = if (entry.artist.isNotBlank()) "${entry.title} by ${entry.artist}" else entry.title
        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_daily)
            .setContentTitle("Artwork of the day")
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val thumbnail = loadThumbnail(entry.imageUrl)
        if (thumbnail != null) {
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(thumbnail)
                    .setSummaryText(entry.story.ifBlank { text })
            )
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(entry.story.ifBlank { text }))
        }

        try {
            manager.notify(NOTIFICATION_ID, builder.build())
        } catch (se: SecurityException) {
            // POST_NOTIFICATIONS not granted on Android 13+; skip quietly
        }
    }

    companion object {
        const val CHANNEL_ID = "daily_artwork"
        private const val NOTIFICATION_ID = 4101
        private const val UNIQUE_NAME = "daily-artwork-notification"
        private const val FALLBACK_ASSET = "daily_artworks.json"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DailyArtworkNotificationWorker>(1, TimeUnit.DAYS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun createChannel(context: Context) {
            val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName("Artwork of the day")
                .setDescription("A daily artwork with a short story")
                .build()
            NotificationManagerCompat.from(context).createNotificationChannel(channel)
        }
    }
}
