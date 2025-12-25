package com.orion.templete.data.model.artist_model

data class TopArtistProjection(
    val artistId: String,
    val name: String,
    val imageUrl: String?,
    val totalLikes: Long,
    val rank: Int
)

