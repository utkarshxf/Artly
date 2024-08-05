package com.orion.templete.usecase

import com.orion.templete.data.model.ArtWorkDTO
import com.orion.templete.domain.repository.GetArtworkRepository
import com.orion.templete.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetArtworkUseCase @Inject constructor(
    private val getArtworkRepository: GetArtworkRepository
) {
    operator fun invoke(): Flow<Resource<ArtWorkDTO>> = flow {
        emit(Resource.Loading(null))
        try {
            emit(Resource.Success(getArtworkRepository.getArtwork()))
        } catch (e: Exception) {
            emit(Resource.Error(e.message))
        }
    }
}