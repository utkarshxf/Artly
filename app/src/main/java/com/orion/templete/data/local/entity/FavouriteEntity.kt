package com.orion.templete.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local source of truth for favourite artworks (offline favourites feature).
 *
 * syncState: SYNCED, PENDING_ADD, PENDING_REMOVE - whether the backend knows.
 * offlineState: NONE, QUEUED, READY, EVICTED - whether a local image copy
 * exists in the app's private file store (slice B). EVICTED rows stay
 * favourites but lost their offline copy to the 200 MB LRU cap and
 * re-download when viewed online.
 */
@Entity(tableName = "favourite_artworks")
data class FavouriteEntity(
    @PrimaryKey val artworkId: String,
    val title: String,
    val artist: String,
    val imageUrl: String,
    val imageUrlCompressed: String?,
    val canSave: Boolean,
    val canSetWallpaper: Boolean,
    val addedAt: Long,
    val lastViewedAt: Long,
    val syncState: String,
    val localPath: String?,
    val localBytes: Long,
    val offlineState: String
) {
    companion object {
        const val SYNCED = "SYNCED"
        const val PENDING_ADD = "PENDING_ADD"
        const val PENDING_REMOVE = "PENDING_REMOVE"

        const val OFFLINE_NONE = "NONE"
        const val OFFLINE_QUEUED = "QUEUED"
        const val OFFLINE_READY = "READY"
        const val OFFLINE_EVICTED = "EVICTED"
    }
}

/**
 * Pending sync operations, flushed to the backend when the network returns.
 * op: ADD or REMOVE.
 */
@Entity(tableName = "favourite_outbox")
data class FavouriteOutboxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val artworkId: String,
    val op: String,
    val createdAt: Long,
    val attempts: Int = 0
) {
    companion object {
        const val OP_ADD = "ADD"
        const val OP_REMOVE = "REMOVE"
    }
}
