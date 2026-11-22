package com.orion.templete.domain.repository

import com.orion.templete.data.model.artwork_model.ArtworkDTO
import com.orion.templete.data.model.artwork_model.ArtworkStatsResponse
import com.orion.templete.data.model.favorits.favoritesDTO
import com.orion.templete.util.ResponseStates
import kotlinx.coroutines.flow.Flow

interface ArtworkRepository {
    suspend fun getArtwork(): ArtworkDTO
    suspend fun paginationArtwork(userId: String , offset : Int, pageSize:Int): List<ArtworkDTO>
    suspend fun likeArtwork(artworkId: String, userId: String): Boolean
    suspend fun disLikeArtwork(artworkId: String, userId: String): Boolean
    suspend fun getArtworkById(userId: String , artworkId: String): Flow<ResponseStates<ArtworkDTO>>
    suspend fun getArtworkByArtistId(artworkId: String, artistId: String, currentArtworkId: String): Flow<ResponseStates<List<ArtworkDTO>>>
    suspend fun getSimilarArtwork(currentUserId: String, artworkId: String): Flow<ResponseStates<List<ArtworkDTO>>>
    suspend fun getArtworkByFavoriteId( favoriteId: String): Flow<ResponseStates<List<ArtworkDTO>>>
    suspend fun getAllFavorites(userId: String): Flow<ResponseStates<List<favoritesDTO>>>
    suspend fun createNewFavorites(userId: String, favorites: favoritesDTO): Flow<ResponseStates<favoritesDTO>>
    suspend fun saveOnFavorites(favoritesId: String, artworkId: String): Flow<ResponseStates<Unit>>

    /**
     * Get artwork statistics (likes and comments count)
     */
    suspend fun getArtworkStats(artworkId: String): Flow<ResponseStates<ArtworkStatsResponse>>
}