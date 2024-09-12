package com.orion.templete.data.repository


import com.orion.templete.data.model.artwork_model.RecommendedArtworkDTO
import com.orion.templete.data.network.ApiService
import com.orion.templete.domain.repository.ArtworkRepository
import com.orion.templete.util.SafeApiRequest
import javax.inject.Inject

class ArtworkRepositoryImplementation @Inject constructor(private  val apiService: ApiService):
    ArtworkRepository, SafeApiRequest() {
        override suspend fun getArtwork(): RecommendedArtworkDTO {
        val response = safeApiRequest { apiService.getAllArtworks() }
        return response
    }

    override suspend fun paginationArtwork(offset: Int, pageSize: Int): List<RecommendedArtworkDTO> {
        val response = safeApiRequest { apiService.paginationArtwork("1" ,offset, pageSize) }
        return response
    }

    override suspend fun likeArtwork(artworkId: String, userId: String): Boolean {
        val response =  apiService.likeArtwork(artworkId, userId)
        return response.isSuccessful
    }

}