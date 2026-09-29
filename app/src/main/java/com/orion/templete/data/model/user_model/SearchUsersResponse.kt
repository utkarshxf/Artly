package com.orion.templete.data.model.user_model

data class SearchUsersResponse(
    val count: Int,
    val users: List<SearchUserItem>,
    val status: Boolean
)

data class SearchUserItem(
    val id: String,
    val name: String?,
    val profile_pic: String?,
    val username: String
)
