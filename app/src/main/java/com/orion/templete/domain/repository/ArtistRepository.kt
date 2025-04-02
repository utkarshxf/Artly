package com.orion.templete.domain.repository

import com.orion.templete.data.model.artist_model.SearchArtistResponse
import com.orion.templete.data.model.artwork_model.ArtworkDTO
import kotlinx.coroutines.flow.Flow

interface ArtistRepository {
    suspend fun searchArtist(query: String): Flow<List<SearchArtistResponse>>
    suspend fun getPopularArtworks(userId :String): Flow<List<ArtworkDTO>>
    suspend fun getNewArtworks(userId :String): Flow<List<ArtworkDTO>>
    suspend fun getRecommendedForToday(userId :String): Flow<List<ArtworkDTO>>
    suspend fun getTodayBiggestHit(): Flow<ArtworkDTO>
}
