package com.orion.templete.data.model.artist_model

data class TopArtistsResponse(
    val artists: List<TopArtistProjection>?,
    val currentPage: Int,
    val totalPages: Int,
    val totalArtists: Long,
    val pageSize: Int
)

