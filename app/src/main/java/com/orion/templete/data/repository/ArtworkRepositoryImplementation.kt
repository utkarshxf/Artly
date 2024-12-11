package com.orion.templete.data.repository


import com.orion.templete.data.model.artwork_model.ArtworkDTO
import com.orion.templete.data.network.ApiService
import com.orion.templete.domain.repository.ArtworkRepository
import com.orion.templete.util.ResponseStates
import com.orion.templete.util.SafeApiRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ArtworkRepositoryImplementation @Inject constructor(private val apiService: ApiService) :
    ArtworkRepository, SafeApiRequest() {
    override suspend fun getArtwork(): ArtworkDTO {
        val response = safeApiRequest { apiService.getAllArtworks() }
        return response
    }

    override suspend fun paginationArtwork(offset: Int, pageSize: Int): List<ArtworkDTO> {
        val response = safeApiRequest { apiService.paginationArtwork("1", offset, pageSize) }
        return response
    }

    override suspend fun likeArtwork(artworkId: String, userId: String): Boolean {
        val response = apiService.likeArtwork(artworkId, userId)
        return response.isSuccessful
    }

    override suspend fun getArtworkById(userId: String): Flow<ResponseStates<ArtworkDTO>> = flow {
            emit(ResponseStates.Loading)
            try {
                val response = safeApiRequest { apiService.getArtworkById(userId) }
                emit(ResponseStates.Success(response))
            } catch (e: Exception) {
                emit(ResponseStates.Error(e.message ?: "Unknown error occurred"))
            }
        }
}