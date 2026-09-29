package com.orion.templete.data.model.chat

// POST chat/token -> Firebase custom token whose uid is the caller's username
data class ChatTokenResponse(
    val token: String? = null,
    val uid: String? = null,
)

// POST chat/notify -> the backend verifies the message and pushes it to the recipient's devices
data class ChatNotifyRequest(
    val conversationId: String,
    val messageId: String,
)
