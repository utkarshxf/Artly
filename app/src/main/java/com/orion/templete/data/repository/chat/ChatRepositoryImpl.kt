package com.orion.templete.data.repository.chat

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.orion.templete.data.model.chat.ChatUser
import com.orion.templete.data.model.chat.Conversation
import com.orion.templete.data.model.chat.Message
import com.orion.templete.data.model.user_model.SearchUsersResponse
import com.orion.templete.data.network.ApiService
import com.orion.templete.domain.repository.chat.ChatRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepositoryImpl(
    private val db: FirebaseFirestore,
    private val api: ApiService
) : ChatRepository {

    private val conversations = db.collection("conversations")

    override fun observeRecentConversations(currentUsername: String): Flow<List<Conversation>> = callbackFlow {
        val reg = conversations
            .whereArrayContains("usernames", currentUsername)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    // Do not clear the current list on transient errors; keep the last successful value
                    // This avoids inbox flicker where items appear briefly and then disappear.
                    return@addSnapshotListener
                }
                val list = snap?.documents?.map { d ->
                    Conversation(
                        id = d.id,
                        usernames = (d.get("usernames") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        lastMessage = d.getString("lastMessage"),
                        lastSender = d.getString("lastSender"),
                        updatedAt = d.getTimestamp("updatedAt"),
                        unread = (d.get("unread") as? Map<*, *>)?.mapNotNull { (k, v) ->
                            (k as? String)?.let { key -> key to ((v as? Number)?.toInt() ?: 0) }
                        }?.toMap() ?: emptyMap()
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    override fun observeMessages(conversationId: String): Flow<List<Message>> = callbackFlow {
        val reg = conversations.document(conversationId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.documents?.map { d ->
                    Message(
                        id = d.id,
                        conversationId = conversationId,
                        sender = d.getString("sender") ?: "",
                        text = d.getString("text") ?: "",
                        createdAt = d.getTimestamp("createdAt") ?: Timestamp.now()
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    override suspend fun ensureConversation(currentUsername: String, peerUsername: String): String {
        // Guard: usernames must not be empty for Firestore operations
        require(currentUsername.isNotBlank()) { "Current username cannot be empty" }
        require(peerUsername.isNotBlank()) { "Peer username cannot be empty" }

        val id = buildConversationId(currentUsername, peerUsername)
        val doc = conversations.document(id).get().await()
        if (!doc.exists()) {
            val data = mapOf(
                "usernames" to listOf(currentUsername, peerUsername),
                "lastMessage" to null,
                "lastSender" to null,
                "updatedAt" to Timestamp.now(),
                "unread" to mapOf<String, Int>()
            )
            conversations.document(id).set(data).await()
        }
        return id
    }

    override suspend fun sendTextMessage(conversationId: String, senderUsername: String, text: String) {
        val msg = mapOf(
            "sender" to senderUsername,
            "text" to text,
            "createdAt" to Timestamp.now()
        )
        val convRef = conversations.document(conversationId)
        convRef.collection("messages").add(msg).await()
        val convSnap = convRef.get().await()
        val usernames = (convSnap.get("usernames") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        val other = usernames.firstOrNull { it != senderUsername }
        val unread = (convSnap.get("unread") as? Map<*, *>)?.mapNotNull { (k, v) ->
            (k as? String)?.let { key -> key to ((v as? Number)?.toInt() ?: 0) }
        }?.toMap()?.toMutableMap() ?: mutableMapOf()
        if (other != null) unread[other] = (unread[other] ?: 0) + 1
        convRef.update(
            mapOf(
                "lastMessage" to text,
                "lastSender" to senderUsername,
                "updatedAt" to Timestamp.now(),
                "unread" to unread
            )
        ).await()
    }

    override suspend fun markConversationRead(conversationId: String, username: String) {
        // Guard: Firestore field keys cannot be empty
        if (conversationId.isBlank() || username.isBlank()) return

        val convRef = conversations.document(conversationId)
        val snap = convRef.get().await()
        val unread = (snap.get("unread") as? Map<*, *>)?.mapNotNull { (k, v) ->
            (k as? String)?.let { key -> key to ((v as? Number)?.toInt() ?: 0) }
        }?.toMap()?.toMutableMap() ?: mutableMapOf()
        unread[username] = 0
        convRef.update("unread", unread).await()
    }

    override suspend fun searchUsers(key: String, limit: Int): Result<List<ChatUser>> {
        return try {
            val res = api.searchUsers(key, limit)
            if (res.isSuccessful) {
                val body: SearchUsersResponse? = res.body()
                val mapped = body?.users?.map {
                    ChatUser(
                        id = it.id,
                        username = it.username.trim(),
                        name = it.name,
                        profilePic = it.profile_pic
                    )
                } ?: emptyList()
                Result.success(mapped)
            } else {
                Result.failure(IllegalStateException("HTTP ${res.code()}"))
            }
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    private fun buildConversationId(a: String, b: String): String {
        val (x, y) = if (a <= b) a to b else b to a
        // Use proper Kotlin string interpolation so each 1-1 pair gets a unique, stable id
        return "${x}_${y}"
    }
}
