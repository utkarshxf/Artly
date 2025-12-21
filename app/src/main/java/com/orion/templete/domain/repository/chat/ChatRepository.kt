package com.orion.templete.domain.repository.chat

import com.orion.templete.data.model.chat.ChatUser
import com.orion.templete.data.model.chat.Conversation
import com.orion.templete.data.model.chat.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeRecentConversations(currentUsername: String): Flow<List<Conversation>>
    fun observeMessages(conversationId: String): Flow<List<Message>>
    suspend fun ensureConversation(currentUsername: String, peerUsername: String): String
    suspend fun sendTextMessage(conversationId: String, senderUsername: String, text: String)
    suspend fun markConversationRead(conversationId: String, username: String)

    // Remote user search
    suspend fun searchUsers(key: String, limit: Int = 20): Result<List<ChatUser>>
}
