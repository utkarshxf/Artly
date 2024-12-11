package com.orion.templete.usecase


import com.orion.templete.data.model.artwork_model.ArtworkDTO
import com.orion.templete.domain.repository.ArtworkRepository
import com.orion.templete.util.ResponseStates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetArtworkUseCase @Inject constructor(
    private val artworkRepository: ArtworkRepository
) {
    fun likeArtwork(artworkId: String, userId: String): Flow<ResponseStates<Boolean>> = flow {
        emit(ResponseStates.Loading)
        try {
            val isLiked = artworkRepository.likeArtwork(artworkId, userId)
            if (isLiked) {
                emit(ResponseStates.Success(true))
            } else {
                emit(ResponseStates.Error("Failed to like artwork"))
            }
        } catch (e: Exception) {
            emit(ResponseStates.Error(e.message.toString()))
        }
    }
}
