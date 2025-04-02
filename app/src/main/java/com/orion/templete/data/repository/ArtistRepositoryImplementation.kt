package com.orion.templete.data.repository

import android.util.Log
import com.orion.templete.data.model.artist_model.SearchArtistResponse
import com.orion.templete.data.model.artwork_model.ArtworkDTO
import com.orion.templete.data.network.ApiService
import com.orion.templete.domain.repository.ArtistRepository
import com.orion.templete.util.SafeApiRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ArtistRepositoryImplementation @Inject constructor(private  val apiService: ApiService):
    ArtistRepository, SafeApiRequest() {
    override suspend fun searchArtist(query: String): Flow<List<SearchArtistResponse>> {
        return flow {
            val response = safeApiRequest {
                Log.d("query", "searchArtist: $query")
                apiService.getAllArtists(query)
            }

            emit(response)
        }
    }

    override suspend fun getPopularArtworks(userId :String): Flow<List<ArtworkDTO>>  = flow {
        try {
            val response = safeApiRequest { apiService.getPopularArtworks(userId) }
            emit(response)
        } catch (e: Exception) {
            Log.e("ArtistRepository", "Error fetching popular artworks: ${e.message}")
        }
    }

    override suspend fun getNewArtworks(userId :String): Flow<List<ArtworkDTO>> = flow {
        try {
            val response = safeApiRequest { apiService.getNewArtworks(userId) }
            emit(response)
        } catch (e: Exception) {
            Log.e("ArtistRepository", "Error fetching new artworks: ${e.message}")
        }
    }

    override suspend fun getRecommendedForToday(userId :String): Flow<List<ArtworkDTO>> = flow{
        try {
            val response = safeApiRequest { apiService.getRecommendedForToday(userId) }
            emit(response)
        } catch (e: Exception) {
            Log.e("ArtistRepository", "Error fetching recommended artworks: ${e.message}")
        }
    }

    override suspend fun getTodayBiggestHit(): Flow<ArtworkDTO> = flow {
        try {
            val response = safeApiRequest { apiService.getTodayBiggestHit() }
            emit(response)
        } catch (e: Exception) {
            Log.e("ArtistRepository", "Error fetching today's biggest hit: ${e.message}")
        }
    }
}