package com.orion.templete.data.model.user_model

data class TopUsersResponse(
    val users: List<TopUserProjection>?,
    val totalPages: Int,
    val totalElements: Long,
    val size: Int,
    val number: Int
)

