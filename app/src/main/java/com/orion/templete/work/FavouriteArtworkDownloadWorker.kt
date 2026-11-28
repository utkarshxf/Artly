package com.orion.templete.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.orion.templete.data.local.dao.FavouriteDao
import com.orion.templete.data.local.entity.FavouriteEntity
import com.orion.templete.data.repository.OfflineImageManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Downloads one favourite artwork's compressed image into the offline store.
 * One unique job per artwork; requires connectivity and not-low storage.
 * On success the row flips QUEUED -> READY inside OfflineImageManager.
 */
class FavouriteArtworkDownloadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun favouriteDao(): FavouriteDao
        fun offlineImageManager(): OfflineImageManager
    }

    override suspend fun doWork(): Result {
        val deps = EntryPointAccessors.fromApplication(applicationContext, Deps::class.java)
        val artworkId = inputData.getString(KEY_ARTWORK_ID) ?: return Result.failure()
        val row = deps.favouriteDao().getById(artworkId) ?: return Result.failure()
        val url = row.imageUrlCompressed?.takeIf { it.isNotBlank() } ?: row.imageUrl
        if (url.isBlank()) return Result.failure()
        return try {
            deps.offlineImageManager().downloadAndStore(artworkId, url)
            Result.success()
        } catch (t: Throwable) {
            if (runAttemptCount < 3) Result.retry() else {
                deps.favouriteDao().updateOfflineCopy(artworkId, FavouriteEntity.OFFLINE_NONE, null, 0)
                Result.failure()
            }
        }
    }

    companion object {
        const val KEY_ARTWORK_ID = "artworkId"
        private const val UNIQUE_PREFIX = "fav-download-"

        fun enqueue(context: Context, artworkId: String) {
            val data = Data.Builder().putString(KEY_ARTWORK_ID, artworkId).build()
            val request = OneTimeWorkRequestBuilder<FavouriteArtworkDownloadWorker>()
                .setInputData(data)
                .setConstraints(
                    androidx.work.Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresStorageNotLow(true)
                        .build()
                )
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_PREFIX + artworkId, ExistingWorkPolicy.KEEP, request)
        }
    }
}
