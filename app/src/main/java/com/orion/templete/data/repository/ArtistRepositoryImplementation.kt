package com.orion.templete.data.repository

import android.content.Context
import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.orion.templete.data.model.artist_model.SearchArtistResponse
import com.orion.templete.data.model.artist_model.TopArtistProjection
import com.orion.templete.data.model.artwork_model.ArtworkDTO
import com.orion.templete.data.network.ApiService
import com.orion.templete.data.paging.TopArtistsPagingSource
import com.orion.templete.domain.repository.ArtistRepository
import com.orion.templete.util.ResponseStates
import com.orion.templete.util.SafeApiRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ArtistRepositoryImplementation @Inject constructor(
    private val apiService: ApiService,
    private val context: Context
): ArtistRepository, SafeApiRequest() {
    override suspend fun searchArtist(query: String): Flow<List<SearchArtistResponse>> {
        return flow {
            val response = safeApiRequest {
                Log.d("query", "searchArtist: $query")
                apiService.getAllArtists(query)
            }

            emit(response)
        }
    }

    override suspend fun search(query: String, type: String, skip: Int, limit: Int) =
        safeApiRequest { apiService.search(query, type, skip, limit) }

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

    override suspend fun getTopArtists(): Flow<ResponseStates<List<TopArtistProjection>>> = flow {
        emit(ResponseStates.Loading)
        try {
            val response = safeApiRequest { apiService.getTopArtists(page = 0, size = 10) }
            emit(ResponseStates.Success(response.artists ?: emptyList()))
        } catch (e: Exception) {
            Log.d("Exception", e.message.toString())
            emit(ResponseStates.Error(e.message ?: "Unknown error occurred"))
        }
    }

    override fun getTopArtistsPaged(): Flow<PagingData<TopArtistProjection>> {
        return Pager(
            config = PagingConfig(
                pageSize = 10,
                enablePlaceholders = false,
                initialLoadSize = 10
            ),
            pagingSourceFactory = { TopArtistsPagingSource(apiService, context) }
        ).flow
    }
}