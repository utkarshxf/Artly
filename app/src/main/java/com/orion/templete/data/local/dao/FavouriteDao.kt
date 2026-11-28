package com.orion.templete.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.orion.templete.data.local.entity.FavouriteEntity
import com.orion.templete.data.local.entity.FavouriteOutboxEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavouriteDao {

    @Query("SELECT * FROM favourite_artworks WHERE syncState != 'PENDING_REMOVE' ORDER BY addedAt DESC")
    fun observeFavourites(): Flow<List<FavouriteEntity>>

    @Query("SELECT * FROM favourite_artworks WHERE artworkId = :artworkId")
    suspend fun getById(artworkId: String): FavouriteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FavouriteEntity)

    @Query("UPDATE favourite_artworks SET syncState = :syncState WHERE artworkId = :artworkId")
    suspend fun updateSyncState(artworkId: String, syncState: String)

    @Query("UPDATE favourite_artworks SET offlineState = :offlineState, localPath = :localPath, localBytes = :localBytes WHERE artworkId = :artworkId")
    suspend fun updateOfflineCopy(artworkId: String, offlineState: String, localPath: String?, localBytes: Long)

    @Query("UPDATE favourite_artworks SET lastViewedAt = :lastViewedAt WHERE artworkId = :artworkId")
    suspend fun touch(artworkId: String, lastViewedAt: Long)

    @Query("DELETE FROM favourite_artworks WHERE artworkId = :artworkId")
    suspend fun delete(artworkId: String)

    @Query("SELECT COALESCE(SUM(localBytes), 0) FROM favourite_artworks WHERE offlineState = 'READY'")
    suspend fun totalLocalBytes(): Long

    @Query("SELECT * FROM favourite_artworks WHERE offlineState = 'READY' ORDER BY lastViewedAt ASC")
    suspend fun readyOldestFirst(): List<FavouriteEntity>

    @Query("SELECT * FROM favourite_artworks WHERE offlineState = 'READY'")
    suspend fun readyCopies(): List<FavouriteEntity>

    // Outbox

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutbox(op: FavouriteOutboxEntity): Long

    @Query("SELECT * FROM favourite_outbox ORDER BY createdAt ASC")
    suspend fun pendingOps(): List<FavouriteOutboxEntity>

    @Query("DELETE FROM favourite_outbox WHERE id = :id")
    suspend fun deleteOutboxOp(id: Long)

    @Query("UPDATE favourite_outbox SET attempts = attempts + 1 WHERE id = :id")
    suspend fun incrementOutboxAttempts(id: Long)
}
