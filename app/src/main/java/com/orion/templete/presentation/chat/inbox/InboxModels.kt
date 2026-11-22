package com.orion.templete.presentation.chat.inbox

import androidx.compose.runtime.Immutable
import com.orion.templete.data.model.chat.ChatUser
import com.orion.templete.data.model.chat.Conversation
import com.orion.templete.data.model.chat.MessagePreview
import com.orion.templete.data.model.chat.MessageType
import com.orion.templete.presentation.chat.components.ChatTime

// A peer keystroke counts as "typing" for this long (matches the thread screen)
internal const val TYPING_WINDOW_MS = 6_000L

// Own last message shows "Seen"/"Sent 2h ago" inside this window, "You: preview · 3w" after it
private const val STATUS_WINDOW_MS = 7 * 24 * 60 * 60 * 1000L

// lastMessage.preview after the sender unsent it (the type is kept, only the preview changes)
internal const val UNSENT_PREVIEW = "Unsent a message"

private val WHITESPACE = Regex("\\s+")

// One inbox row, fully resolved for display
@Immutable
data class InboxRowUi(
    val conversationId: String,
    val peer: String,
    val title: String,
    val avatar: String?,
    val active: Boolean,
    val subtitle: String,
    val time: String?, // rendered as " · 2h" after the (ellipsized) subtitle
    val unread: Boolean, // unread messages or "Mark as unread": bold title/subtitle + blue dot
    val unreadCount: Int,
    val typing: Boolean,
    val muted: Boolean,
)

// A person in the "Active now" row, search results or suggestions
@Immutable
data class InboxPersonUi(
    val username: String,
    val name: String,
    val avatar: String?,
    val active: Boolean,
)

sealed interface InboxContent {
    data object Loading : InboxContent

    data class Error(val message: String, val notConfigured: Boolean) : InboxContent

    @Immutable
    data class Ready(
        val rows: List<InboxRowUi>,
        val activeNow: List<InboxPersonUi>,
        val searchQuery: String, // normalized; empty = not searching
        val searchMatches: List<InboxRowUi>,
        val people: List<InboxPersonUi>, // remote "More people", without me and without searchMatches
        val peopleLoading: Boolean,
        val peopleFailed: Boolean,
    ) : InboxContent
}

@Immutable
data class InboxUiState(
    val me: String = "",
    val content: InboxContent = InboxContent.Loading,
)

// Trimmed, case-preserving query without a leading "@"
internal fun normalizeQuery(raw: String): String = raw.trim().removePrefix("@").trim()

internal fun InboxRowUi.matches(query: String): Boolean =
    title.contains(query, ignoreCase = true) || peer.contains(query, ignoreCase = true)

internal fun InboxPersonUi.matches(query: String): Boolean =
    name.contains(query, ignoreCase = true) || username.contains(query, ignoreCase = true)

internal fun ChatUser.toPersonUi(now: Long): InboxPersonUi =
    InboxPersonUi(username = username, name = displayName, avatar = avatar, active = isActiveNow(now))

// The peer typed within the last 6 s, after the last message was written
internal fun Conversation.isPeerTyping(now: Long): Boolean {
    val at = peerTypingAt
    if (at <= 0L) return false
    if (at <= (lastMessage?.createdAt ?: 0L)) return false
    return now - at < TYPING_WINDOW_MS
}

internal fun buildInboxRow(conversation: Conversation, peer: ChatUser?, me: String, now: Long): InboxRowUi {
    val last = conversation.lastMessage
    val typing = conversation.isPeerTyping(now)
    val unreadCount = conversation.unreadCount.coerceAtLeast(0)
    var time: String? = last?.let { ChatTime.short(it.createdAt, now) }
    val subtitle: String = when {
        typing -> {
            time = null
            "Typing…"
        }
        last == null -> {
            time = null
            "Tap to chat"
        }
        unreadCount > 1 -> "${if (unreadCount > 99) "99+" else unreadCount.toString()} new messages"
        // A call is not a message I sent: no "Seen" / "Sent 2h ago", it is described from my side instead.
        // The other person's side shows the backend's preview as it is (theirPreview below).
        last.type == MessageType.CALL && last.sender == me -> ownCallPreview(last)
        last.sender == me -> {
            val (text, suffix) = ownStatus(conversation, last, now)
            time = suffix
            text
        }
        else -> theirPreview(last)
    }
    return InboxRowUi(
        conversationId = conversation.id,
        peer = conversation.peer,
        title = peer?.displayName ?: conversation.peer,
        avatar = peer?.avatar?.takeIf { it.isNotBlank() },
        active = peer?.isActiveNow(now) == true,
        subtitle = subtitle,
        time = time,
        unread = conversation.hasUnread,
        unreadCount = unreadCount,
        typing = typing,
        muted = conversation.muted,
    )
}

// My own last message: "Seen · 2h" / "Seen just now" / "Sent 2h ago" while recent, "You: preview · 3w" afterwards
private fun ownStatus(conversation: Conversation, last: MessagePreview, now: Long): Pair<String, String?> {
    val short = ChatTime.short(last.createdAt, now)
    if (now - last.createdAt < STATUS_WINDOW_MS && last.preview != UNSENT_PREVIEW) {
        val seen = conversation.peerLastRead > 0L && conversation.peerLastRead >= last.createdAt
        return when {
            seen && short == "now" -> "Seen just now" to null
            seen -> "Seen" to short
            short == "now" -> "Sent just now" to null
            else -> "Sent $short ago" to null
        }
    }
    return ownPreview(last) to short
}

private fun ownPreview(last: MessagePreview): String = when {
    last.preview == UNSENT_PREVIEW -> "You unsent a message"
    last.type == MessageType.IMAGE -> "You sent a photo"
    last.type == MessageType.ARTWORK -> "You shared a post"
    last.type == MessageType.PROFILE -> "You shared a profile"
    else -> "You: ${theirPreview(last)}"
}

private fun theirPreview(last: MessagePreview): String {
    val clean = last.preview.replace(WHITESPACE, " ").trim()
    if (clean.isNotEmpty()) return clean
    return when (last.type) {
        MessageType.IMAGE -> "Sent a photo"
        MessageType.ARTWORK -> "Shared a post"
        MessageType.PROFILE -> "Shared a profile"
        MessageType.LIKE -> "❤️"
        MessageType.TEXT -> "Sent a message"
        MessageType.CALL -> "Call"
    }
}

// A call I placed. The backend's preview is worded for the person who was called ("Missed audio call",
// "Declined video call"); from my side the same call reads "You called · No answer" / "You called · Declined".
// A call that was answered is just "Audio call" / "Video call" for both.
private fun ownCallPreview(last: MessagePreview): String {
    val clean = last.preview.replace(WHITESPACE, " ").trim()
    return when {
        clean.startsWith("Audio call", ignoreCase = true) || clean.startsWith("Video call", ignoreCase = true) -> clean
        clean.startsWith("Declined", ignoreCase = true) -> "You called · Declined"
        else -> "You called · No answer"
    }
}
