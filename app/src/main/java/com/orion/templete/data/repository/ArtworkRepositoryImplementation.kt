package com.orion.templete.data.repository


import android.util.Log
import com.orion.templete.data.model.artwork_model.ArtworkDTO
import com.orion.templete.data.model.favorits.favoritesDTO
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

    override suspend fun paginationArtwork(userId: String , offset: Int, pageSize: Int): List<ArtworkDTO> {
        val response = safeApiRequest { apiService.paginationArtwork(userId, offset, pageSize) }
        return response
    }

    override suspend fun likeArtwork(artworkId: String, userId: String): Boolean {
        val response = apiService.likeArtwork(artworkId, userId)
        return response.isSuccessful
    }

    override suspend fun getArtworkById(userId: String,artworkId: String): Flow<ResponseStates<ArtworkDTO>> = flow {
            emit(ResponseStates.Loading)
            try {
                val response = safeApiRequest { apiService.getArtworkById(userId = userId, artworkId = artworkId) }
                emit(ResponseStates.Success(response))
            } catch (e: Exception) {
                emit(ResponseStates.Error(e.message ?: "Unknown error occurred"))
            }
        }

    override suspend fun getArtworkByFavoriteId(favoriteId: String): Flow<ResponseStates<List<ArtworkDTO>>> = flow {
        emit(ResponseStates.Loading)
        try {
            val response = safeApiRequest { apiService.getArtworkByFavoriteId(favoriteId) }
            emit(ResponseStates.Success(response))
        } catch (e: Exception) {
            emit(ResponseStates.Error(e.message ?: "Unknown error occurred"))
        }
    }

    override suspend fun saveOnFavorites(
        favoritesId: String,
        artworkId: String
    ): Flow<ResponseStates<Unit>>  = flow  {
        emit(ResponseStates.Loading)
        try {
            Log.d("Exception" , favoritesId)
            Log.d("Exception" , artworkId)
            val response = safeApiRequest { apiService.saveOnFavorites(favoritesId , artworkId) }
            emit(ResponseStates.Success(response))
        } catch (e: Exception) {
            Log.d("Exception" , e.message.toString())
            emit(ResponseStates.Error(e.message ?: "Unknown error occurred"))
        }
    }
    override suspend fun getAllFavorites(userId: String): Flow<ResponseStates<List<favoritesDTO>>> = flow{
        emit(ResponseStates.Loading)
        try {
            val response = safeApiRequest { apiService.getFavoritesByUserId(userId) }
            emit(ResponseStates.Success(response))
        } catch (e: Exception) {
            emit(ResponseStates.Error(e.message ?: "Unknown error occurred"))
        }
    }
    override suspend fun createNewFavorites(
        userId: String,
        favorites: favoritesDTO
    ): Flow<ResponseStates<favoritesDTO>> = flow {
        emit(ResponseStates.Loading)
        try {
            val response = safeApiRequest { apiService.createNewFavorites(userId , favorites) }
            emit(ResponseStates.Success(response))
        } catch (e: Exception) {
            emit(ResponseStates.Error(e.message ?: "Unknown error occurred"))
        }
    }
}