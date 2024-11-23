package com.orion.templete.domain.repository

import com.orion.templete.data.model.artwork_model.ArtworkDTO
import com.orion.templete.data.model.artwork_model.ArtworkDetailsDTO
import com.orion.templete.util.ResponseStates
import kotlinx.coroutines.flow.Flow

interface ArtworkRepository {
    suspend fun getArtwork(): ArtworkDetailsDTO
    suspend fun paginationArtwork( offset : Int, pageSize:Int): List<ArtworkDetailsDTO>
    suspend fun likeArtwork(artworkId: String, userId: String): Boolean
    suspend fun getArtworkById(userId: String): Flow<ResponseStates<ArtworkDetailsDTO>>
}