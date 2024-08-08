package com.orion.templete.domain.repository

import com.orion.templete.data.model.ArtworkDTO

interface GetArtworkRepository {
    suspend fun getArtwork(): ArtworkDTO
    suspend fun paginationArtwork( offset : Int, pageSize:Int): ArtworkDTO
}