package com.orion.templete.data.model.artist_model

/**
 * Artist statistics response model
 */
data class ArtistStatsResponse(
    val artistId: String,
    val followers: Int,
    val totalLikesOnArtworks: Int,
    val totalArtworks: Int
)

