package com.orion.templete.data.model.artwork_model.comments

data class GetCommentsDTO(
    val createdAt: String?=null,
    val edited: Boolean?=null,
    val id: String?=null,
    val text: String?=null,
    val updatedAt: String?=null,
    val userId: String?=null,
    val userName: String?=null,
    val userProfilePicture: String?=null
)