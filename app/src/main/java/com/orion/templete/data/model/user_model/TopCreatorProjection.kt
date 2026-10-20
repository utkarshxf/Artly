package com.orion.templete.data.model.user_model

data class TopCreatorProjection(
    val userId: String,
    val name: String,
    val profilePicture: String?,
    val artistId: String,
    val artistName: String,
    val totalLikes: Long,
    val rank: Int
)

