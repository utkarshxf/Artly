package com.orion.templete.data.model.user_model

data class TopCreatorsResponse(
    val creators: List<TopCreatorProjection>?,
    val currentPage: Int,
    val totalPages: Int,
    val totalCreators: Long,
    val pageSize: Int
)

