package com.orion.templete.domain.repository.chat

import android.net.Uri
import com.orion.templete.data.model.chat.ChatMessage
import com.orion.templete.data.model.chat.ChatUser
import com.orion.templete.data.model.chat.Conversation
import com.orion.templete.data.model.chat.MessagePage
import com.orion.templete.data.model.chat.OutgoingMessage
import kotlinx.coroutines.flow.Flow

// Every call waits for ChatSession.ensureSignedIn(); flows fail (throw) on auth/permission errors instead of
// silently emitting empty lists, so screens can show an error + retry.
interface ChatRepository {
    val me: String

    fun conversationIdWith(peer: String): String

    // Inbox, newest first, without conversations the user deleted (clearedAt >= updatedAt)
    fun observeConversations(): Flow<List<Conversation>>

    // Single conversation (null until it exists)
    fun observeConversation(conversationId: String): Flow<Conversation?>

    // Newest `limit` messages after the user's clearedAt, oldest first; unsent messages are included
    // (flagged) so the UI can decide
    fun observeMessages(conversationId: String, limit: Int): Flow<MessagePage>

    // Number of conversations with unread messages (badge on the swipe screen)
    fun observeUnreadConversationCount(): Flow<Int>

    // Public profile + presence, live; falls back to the backend profile when the Firestore doc is missing
    fun observeUser(username: String): Flow<ChatUser?>

    suspend fun getUser(username: String): ChatUser?

    suspend fun searchUsers(query: String, limit: Int = 20): Result<List<ChatUser>>

    // Creates the conversation doc if needed; returns its id
    suspend fun ensureConversation(peer: String): String

    // Returns the new message id. Also notifies the backend (push) without failing the send if that fails.
    suspend fun send(conversationId: String, message: OutgoingMessage): String

    // Uploads a picked image (compressed) and returns (downloadUrl, width, height); progress 0..1
    suspend fun uploadImage(conversationId: String, uri: Uri, onProgress: (Float) -> Unit = {}): Triple<String, Int, Int>

    suspend fun markRead(conversationId: String)
    suspend fun setTyping(conversationId: String, typing: Boolean)
    suspend fun setReaction(conversationId: String, messageId: String, emoji: String?)
    suspend fun unsend(conversationId: String, message: ChatMessage)
    suspend fun setMuted(conversationId: String, muted: Boolean)
    suspend fun setMarkedUnread(conversationId: String, unread: Boolean)
    suspend fun deleteForMe(conversationId: String)
}
