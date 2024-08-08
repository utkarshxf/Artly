package com.orion.templete.usecase


import com.orion.templete.data.model.ArtworkDTO
import com.orion.templete.domain.repository.GetArtworkRepository
import com.orion.templete.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetArtworkUseCase @Inject constructor(
    private val getArtworkRepository: GetArtworkRepository
) {
     fun getArtwork(): Flow<Resource<ArtworkDTO>> = flow {
        emit(Resource.Loading(null))
        try {
            emit(Resource.Success(getArtworkRepository.getArtwork()))
        } catch (e: Exception) {
            emit(Resource.Error(e.message))
        }
    }


}
