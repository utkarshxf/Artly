package com.orion.templete.data.model.chat

// Domain models for the Instagram-style DMs. The Firestore wire format is documented next to the
// repository implementation; all times here are epoch millis.

const val ACTIVE_NOW_WINDOW_MS = 3 * 60 * 1000L

enum class MessageType(val wire: String) {
    TEXT("text"), IMAGE("image"), ARTWORK("artwork"), LIKE("like"), PROFILE("profile");

    companion object {
        fun fromWire(value: String?): MessageType = entries.firstOrNull { it.wire == value } ?: TEXT
    }
}

// Public profile + presence (Firestore users/{username}, falls back to the backend profile)
data class ChatUser(
    val username: String,
    val name: String? = null,
    val avatar: String? = null,
    val lastActive: Long? = null,
) {
    val displayName: String get() = name?.takeIf { it.isNotBlank() } ?: username
    fun isActiveNow(now: Long = System.currentTimeMillis()): Boolean =
        lastActive != null && now - lastActive < ACTIVE_NOW_WINDOW_MS
}

data class MessagePreview(
    val id: String,
    val sender: String,
    val type: MessageType,
    val preview: String,
    val createdAt: Long,
)

data class ArtworkRef(
    val id: String,
    val title: String? = null,
    val imageUrl: String? = null,
    val artistName: String? = null,
)

// A shared profile (Instagram "share profile"): id is what the profile screen opens (the username for an Artistry
// account, the artist id for historical artists); subtitle is a short line such as "@username" or "Artist".
data class ProfileRef(
    val id: String,
    val name: String? = null,
    val avatar: String? = null,
    val subtitle: String? = null,
)

// One inbox row, already resolved for the signed-in user
data class Conversation(
    val id: String,
    val usernames: List<String>,
    val peer: String,
    val lastMessage: MessagePreview?,
    val updatedAt: Long,
    val unreadCount: Int,
    val markedUnread: Boolean,
    val muted: Boolean,
    val myLastRead: Long,
    val peerLastRead: Long,
    val peerTypingAt: Long,
    val clearedAt: Long,
) {
    val hasUnread: Boolean get() = unreadCount > 0 || markedUnread
}

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val sender: String,
    val type: MessageType,
    val text: String = "",
    val imageUrl: String? = null,
    val imageWidth: Int? = null,
    val imageHeight: Int? = null,
    val artwork: ArtworkRef? = null,
    val profile: ProfileRef? = null,
    val replyTo: MessagePreview? = null,
    val reactions: Map<String, String> = emptyMap(), // username -> emoji
    val createdAt: Long,
    val pending: Boolean = false, // written locally, not yet acknowledged by the server
    val unsent: Boolean = false,
)

// What the composer hands to the repository
sealed interface OutgoingMessage {
    val replyTo: MessagePreview?

    data class Text(val text: String, override val replyTo: MessagePreview? = null) : OutgoingMessage
    data class Like(override val replyTo: MessagePreview? = null) : OutgoingMessage
    data class Image(
        val url: String,
        val width: Int?,
        val height: Int?,
        val caption: String = "",
        override val replyTo: MessagePreview? = null,
    ) : OutgoingMessage
    data class Artwork(val artwork: ArtworkRef, override val replyTo: MessagePreview? = null) : OutgoingMessage
    data class Profile(val profile: ProfileRef, override val replyTo: MessagePreview? = null) : OutgoingMessage
}

// A page of a thread; hasOlder = more history can be loaded with a bigger limit
data class MessagePage(
    val messages: List<ChatMessage>, // oldest first
    val hasOlder: Boolean,
)

sealed interface ChatAuthState {
    data object SigningIn : ChatAuthState
    data object Ready : ChatAuthState
    data class Error(val message: String, val notConfigured: Boolean = false) : ChatAuthState
}
