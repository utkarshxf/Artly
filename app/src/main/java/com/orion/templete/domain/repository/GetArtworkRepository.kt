package com.orion.templete.domain.repository

import com.orion.templete.data.model.RecommendedArtworkDTO

interface GetArtworkRepository {
    suspend fun getArtwork(): RecommendedArtworkDTO
    suspend fun paginationArtwork( offset : Int, pageSize:Int): List<RecommendedArtworkDTO>
    suspend fun likeArtwork(artworkId: String, userId: String): Boolean
}