package com.orion.templete.data.repository


import android.util.Log
import com.orion.templete.data.model.ArtworkDTO
import com.orion.templete.data.model.Content
import com.orion.templete.data.network.ApiService
import com.orion.templete.domain.repository.GetArtworkRepository
import com.orion.templete.util.SafeApiRequest
import javax.inject.Inject

class GetArtworkRepositoryImplementation @Inject constructor(private  val apiService: ApiService):
    GetArtworkRepository, SafeApiRequest() {
        override suspend fun getArtwork(): ArtworkDTO {
        val response = safeApiRequest { apiService.getAllArtworks() }
        return response
    }

    override suspend fun paginationArtwork(offset: Int, pageSize: Int): List<Content> {
        val response = safeApiRequest { apiService.paginationArtwork("user_test1" ,offset, pageSize) }
        return response
    }
}