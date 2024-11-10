package com.orion.templete.data.model.artwork_model.comments

data class CommentRequest(
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)