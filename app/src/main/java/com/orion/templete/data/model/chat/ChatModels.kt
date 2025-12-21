package com.orion.templete.data.model.chat

import com.google.firebase.Timestamp

data class ChatUser(
    val id: String,
    val username: String,
    val name: String? = null,
    val profilePic: String? = null,
)

data class Conversation(
    val id: String,
    val usernames: List<String>, // always two for 1-1
    val lastMessage: String? = null,
    val lastSender: String? = null,
    val updatedAt: Timestamp? = null,
    val unread: Map<String, Int> = emptyMap(), // username -> count
)

data class Message(
    val id: String,
    val conversationId: String,
    val sender: String, // sender username (source of truth)
    val text: String,
    val createdAt: Timestamp = Timestamp.now(),
)
