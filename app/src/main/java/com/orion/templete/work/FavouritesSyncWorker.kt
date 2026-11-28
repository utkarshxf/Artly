package com.orion.templete.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.orion.templete.data.local.dao.FavouriteDao
import com.orion.templete.data.local.entity.FavouriteOutboxEntity
import com.orion.templete.data.network.ApiService
import com.orion.templete.data.repository.FavouritesOfflineStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

/**
 * Flushes the favourites outbox to the backend when the network is back.
 * ADD ops replay the save into the collection the user picked. REMOVE ops
 * have no backend endpoint yet and are dropped once the artwork is gone
 * locally; they are left for a later backend slice.
 */
class FavouritesSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun favouriteDao(): FavouriteDao
        fun favouritesOfflineStore(): FavouritesOfflineStore
        fun apiService(): ApiService
    }

    override suspend fun doWork(): Result {
        val deps = EntryPointAccessors.fromApplication(applicationContext, Deps::class.java)
        val ops = deps.favouritesOfflineStore().pendingOps()
        if (ops.isEmpty()) return Result.success()
        var hadFailure = false
        for (op in ops) {
            try {
                when (op.op) {
                    FavouriteOutboxEntity.OP_ADD -> {
                        val collectionId = op.collectionId
                        if (collectionId == null) {
                            // Cannot route the save; leave the row pending and drop the op
                            deps.favouriteDao().deleteOutboxOp(op.id)
                        } else {
                            val response = deps.apiService().saveOnFavorites(collectionId, op.artworkId)
                            if (response.isSuccessful) {
                                deps.favouritesOfflineStore().confirmSynced(op)
                            } else {
                                deps.favouritesOfflineStore().noteOpFailed(op)
                                hadFailure = true
                            }
                        }
                    }
                    else -> {
                        // REMOVE: no backend endpoint yet; drop the op, local state already reflects it
                        deps.favouriteDao().deleteOutboxOp(op.id)
                    }
                }
            } catch (t: Throwable) {
                deps.favouritesOfflineStore().noteOpFailed(op)
                hadFailure = true
            }
        }
        return if (hadFailure) Result.retry() else Result.success()
    }

    companion object {
        private const val UNIQUE_NAME = "favourites-outbox-sync"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<FavouritesSyncWorker>(1, TimeUnit.HOURS)
                .setConstraints(
                    androidx.work.Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
