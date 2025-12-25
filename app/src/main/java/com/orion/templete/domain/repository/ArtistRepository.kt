package com.orion.templete.domain.repository

import androidx.paging.PagingData
import com.orion.templete.data.model.artist_model.SearchArtistResponse
import com.orion.templete.data.model.artist_model.TopArtistProjection
import com.orion.templete.data.model.artwork_model.ArtworkDTO
import com.orion.templete.util.ResponseStates
import kotlinx.coroutines.flow.Flow

interface ArtistRepository {
    suspend fun searchArtist(query: String): Flow<List<SearchArtistResponse>>
    suspend fun getPopularArtworks(userId :String): Flow<List<ArtworkDTO>>
    suspend fun getNewArtworks(userId :String): Flow<List<ArtworkDTO>>
    suspend fun getRecommendedForToday(userId :String): Flow<List<ArtworkDTO>>
    suspend fun getTodayBiggestHit(): Flow<ArtworkDTO>
    suspend fun getTopArtists(): Flow<ResponseStates<List<TopArtistProjection>>>
    fun getTopArtistsPaged(): Flow<PagingData<TopArtistProjection>>
}
