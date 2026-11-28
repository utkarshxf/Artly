package com.orion.templete.data.repository

import com.orion.templete.data.local.dao.FavouriteDao
import com.orion.templete.data.local.entity.FavouriteEntity
import com.orion.templete.data.local.entity.FavouriteOutboxEntity
import com.orion.templete.data.model.artwork_model.ArtworkDTO
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local source of truth for favourite artworks (offline favourites feature).
 *
 * Every change is written here first and queued in the outbox; a later sync
 * slice flushes pending ops to the backend when the network is available.
 * This class never touches the network, so favourites keep working fully
 * offline.
 */
@Singleton
class FavouritesOfflineStore @Inject constructor(
    private val dao: FavouriteDao
) {
    fun observeFavourites(): Flow<List<FavouriteEntity>> = dao.observeFavourites()

    suspend fun isFavourite(artworkId: String): Boolean =
        dao.getById(artworkId)?.let { it.syncState != FavouriteEntity.PENDING_REMOVE } == true

    suspend fun markFavourite(artwork: ArtworkDTO) {
        val id = artwork.id ?: return
        val now = System.currentTimeMillis()
        val existing = dao.getById(id)
        dao.upsert(
            FavouriteEntity(
                artworkId = id,
                title = artwork.title ?: "",
                artist = artwork.artist ?: "",
                imageUrl = artwork.imageUrl ?: "",
                imageUrlCompressed = artwork.image_url_compressed,
                canSave = artwork.canSave == true,
                canSetWallpaper = artwork.canSetWallpaper == true,
                addedAt = existing?.addedAt ?: now,
                lastViewedAt = now,
                syncState = if (existing?.syncState == FavouriteEntity.SYNCED) FavouriteEntity.SYNCED else FavouriteEntity.PENDING_ADD,
                localPath = existing?.localPath,
                localBytes = existing?.localBytes ?: 0,
                offlineState = existing?.offlineState ?: FavouriteEntity.OFFLINE_NONE
            )
        )
        if (existing?.syncState != FavouriteEntity.SYNCED) {
            dao.insertOutbox(FavouriteOutboxEntity(artworkId = id, op = FavouriteOutboxEntity.OP_ADD, createdAt = now))
        }
    }

    suspend fun unmarkFavourite(artworkId: String) {
        val existing = dao.getById(artworkId) ?: return
        if (existing.syncState == FavouriteEntity.SYNCED) {
            dao.updateSyncState(artworkId, FavouriteEntity.PENDING_REMOVE)
        }
        dao.insertOutbox(FavouriteOutboxEntity(artworkId = artworkId, op = FavouriteOutboxEntity.OP_REMOVE, createdAt = System.currentTimeMillis()))
    }

    suspend fun pendingOps(): List<FavouriteOutboxEntity> = dao.pendingOps()

    /** Called by the sync slice after the backend confirms an op. */
    suspend fun confirmSynced(op: FavouriteOutboxEntity) {
        when (op.op) {
            FavouriteOutboxEntity.OP_ADD -> dao.updateSyncState(op.artworkId, FavouriteEntity.SYNCED)
            FavouriteOutboxEntity.OP_REMOVE -> dao.delete(op.artworkId)
        }
        dao.deleteOutboxOp(op.id)
    }

    suspend fun noteOpFailed(op: FavouriteOutboxEntity) = dao.incrementOutboxAttempts(op.id)

    /** Slice B: record a downloaded offline copy and evict LRU copies over the cap. */
    suspend fun recordOfflineCopy(artworkId: String, path: String, bytes: Long, capBytes: Long): List<FavouriteEntity> {
        dao.updateOfflineCopy(artworkId, FavouriteEntity.OFFLINE_READY, path, bytes)
        val evicted = mutableListOf<FavouriteEntity>()
        while (dao.totalLocalBytes() > capBytes) {
            val victim = dao.readyOldestFirst().firstOrNull { it.artworkId != artworkId } ?: break
            dao.updateOfflineCopy(victim.artworkId, FavouriteEntity.OFFLINE_EVICTED, null, 0)
            evicted.add(victim)
        }
        return evicted
    }

    suspend fun touch(artworkId: String) = dao.touch(artworkId, System.currentTimeMillis())

    suspend fun readyCopies(): List<FavouriteEntity> = dao.readyCopies()
}
