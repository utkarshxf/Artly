package com.orion.templete.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest
import com.orion.templete.data.local.entity.FavouriteEntity
import com.orion.templete.work.FavouriteArtworkDownloadWorker

/**
 * Coil model that prefers the offline copy of a favourite artwork.
 * Falls back to the network URL when there is no READY local file.
 * A stable memoryCacheKey avoids flicker when the source switches.
 */
@Composable
fun rememberOfflineFirstImageRequest(artworkId: String?, url: String?): ImageRequest {
    val context = LocalContext.current
    val data by produceState<Any?>(initialValue = url, artworkId, url) {
        if (artworkId == null) return@produceState
        value = try {
            val deps = dagger.hilt.android.EntryPointAccessors.fromApplication(
                context,
                FavouriteArtworkDownloadWorker.Deps::class.java
            )
            val row = deps.favouriteDao().getById(artworkId)
            if (row?.offlineState == FavouriteEntity.OFFLINE_READY &&
                row.localPath != null && java.io.File(row.localPath).exists()
            ) {
                java.io.File(row.localPath)
            } else {
                url
            }
        } catch (t: Throwable) {
            url
        }
    }
    return ImageRequest.Builder(context)
        .data(data)
        .memoryCacheKey("artwork-${artworkId ?: url}")
        .diskCacheKey("artwork-${artworkId ?: url}")
        .build()
}
