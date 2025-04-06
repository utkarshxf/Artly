package com.orion.templete.data.repository

import android.util.Log
import com.orion.templete.data.model.ai_model.GeneratedImageResponse
import com.orion.templete.data.model.artist_model.SearchArtistResponse
import com.orion.templete.data.model.favorits.favoritesDTO
import com.orion.templete.data.network.ApiService
import com.orion.templete.domain.repository.AIRepository
import com.orion.templete.domain.repository.ArtistRepository
import com.orion.templete.util.ResponseStates
import com.orion.templete.util.SafeApiRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class AIRepositoryImplementation @Inject constructor(private  val apiService: ApiService):
    AIRepository, SafeApiRequest(){

    override suspend fun generateImage(
        image: String?,
        prompt: String,
        strength: Float,
        guidanceScale: Float,
        steps: Int,
        seed: String?
    ): Flow<ResponseStates<GeneratedImageResponse>> = flow {
        emit(ResponseStates.Loading)
        try {
            val response = safeApiRequest {apiService.generateImage(image, prompt, strength, guidanceScale, steps, seed) }
            emit(ResponseStates.Success(response))
        } catch (e: Exception) {
            emit(ResponseStates.Error(e.message ?: "Unknown error occurred"))
        }
    }


}