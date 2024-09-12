package com.orion.templete.domain.repository

import com.orion.templete.data.model.artwork_model.RecommendedArtworkDTO

interface ArtworkRepository {
    suspend fun getArtwork(): RecommendedArtworkDTO
    suspend fun paginationArtwork( offset : Int, pageSize:Int): List<RecommendedArtworkDTO>
    suspend fun likeArtwork(artworkId: String, userId: String): Boolean
}