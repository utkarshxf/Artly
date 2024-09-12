package com.orion.templete.usecase


import com.orion.templete.data.model.artwork_model.RecommendedArtworkDTO
import com.orion.templete.domain.repository.ArtworkRepository
import com.orion.templete.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetArtworkUseCase @Inject constructor(
    private val artworkRepository: ArtworkRepository
) {
     fun getArtwork(): Flow<Resource<RecommendedArtworkDTO>> = flow {
        emit(Resource.Loading(null))
        try {
            emit(Resource.Success(artworkRepository.getArtwork()))
        } catch (e: Exception) {
            emit(Resource.Error(e.message))
        }
    }
    fun likeArtwork(artworkId: String, userId: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading(null))
        try {
            val isLiked = artworkRepository.likeArtwork(artworkId, userId)
            if (isLiked) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Failed to like artwork"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message))
        }
    }
}
