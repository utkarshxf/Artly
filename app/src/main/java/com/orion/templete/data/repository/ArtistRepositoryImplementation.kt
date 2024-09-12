package com.orion.templete.data.repository

import com.orion.templete.data.model.artist_model.SearchArtistResponse
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
            val response = safeApiRequest { apiService.getAllArtists(query) }
            emit(response)
        }
    }
}