package com.orion.templete.presentation.chat.thread

import android.net.Uri
import androidx.compose.runtime.Immutable
import com.orion.templete.data.model.chat.ArtworkRef
import com.orion.templete.data.model.chat.ProfileRef
import com.orion.templete.data.model.chat.ChatMessage
import com.orion.templete.data.model.chat.MessagePreview
import com.orion.templete.data.model.chat.MessageType

// UI models of the conversation screen. Everything the rows need is resolved in the ViewModel (off the main
// thread), so composition only lays things out.

const val THREAD_HEART = "❤️"

// Instagram's quick reactions: ❤️ 😂 😮 😢 😡 👍
val THREAD_QUICK_REACTIONS: List<String> = listOf(
    THREAD_HEART,
    "😂",
    "😮",
    "😢",
    "😡",
    "👍",
)

const val THREAD_PAGE_SIZE = 40
const val THREAD_MAX_TEXT = 4000

@Immutable
data class ThreadLink(val start: Int, val end: Int, val url: String)

// Photo bubble: `model` is the download URL, or the picked content Uri while it is still uploading
@Immutable
data class ThreadImageUi(
    val model: Any,
    val width: Int?,
    val height: Int?,
    // Coil memory-cache key of the local preview; shown while the uploaded copy loads, so a sent photo never flashes
    val placeholderKey: String? = null,
    // Explicit memory-cache key for a local preview
    val memoryKey: String? = null,
) {
    // width / height, clamped like Instagram (very tall or very wide photos are cropped)
    val aspectRatio: Float
        get() {
            val w = width ?: 0
            val h = height ?: 0
            return if (w > 0 && h > 0) (w.toFloat() / h.toFloat()).coerceIn(0.5f, 2f) else 1f
        }
}

@Immutable
data class ThreadReplyUi(
    val targetId: String,
    val label: String,
    val preview: String,
    val thumbnailUrl: String?,
    val unavailable: Boolean,
)

@Immutable
data class ThreadReaction(val username: String, val emoji: String)

@Immutable
data class ThreadReactionsUi(
    val emojis: List<String>, // distinct, most used first (max 3)
    val count: Int,
    val mine: String?,
    val byUser: List<ThreadReaction>, // me first
)

enum class ThreadStatusKind { SENDING, SENT, SEEN, FAILED }

@Immutable
data class ThreadStatusUi(val kind: ThreadStatusKind, val label: String)

// A call shown as a card in the thread, already worded for the person looking at it
@Immutable
data class ThreadCallUi(
    val video: Boolean,
    // A call I didn't take (missed, or declined): red, like Instagram's missed calls
    val missed: Boolean,
    val title: String, // "Video call", "Missed audio call", "No answer", "Declined"
    val detail: String, // "2 min · 10:42", "Video call · 10:42", "10:42"
    // "Call back" / "Call again"; null when the row can't say what kind of call it was (malformed document)
    val action: String?,
)

@Immutable
data class ThreadMessageUi(
    val key: String,
    val id: String, // Firestore id, or the local id of a message that is not on the server yet
    val localId: String?, // non-null = local outgoing row (uploading, waiting to be written, or failed)
    val mine: Boolean,
    val sender: String,
    val type: MessageType,
    val text: String,
    val links: List<ThreadLink>,
    val emojiOnly: Boolean,
    val image: ThreadImageUi?,
    val artwork: ArtworkRef?,
    val profile: ProfileRef?,
    val call: ThreadCallUi?, // non-null for every MessageType.CALL row
    val replyTo: ThreadReplyUi?,
    val reactions: ThreadReactionsUi?,
    val createdAt: Long,
    val pending: Boolean,
    val failed: Boolean,
    val uploadProgress: Float?, // 0..1 while a picked photo uploads
    val groupedWithPrev: Boolean,
    val groupedWithNext: Boolean,
    val showAvatar: Boolean,
    val status: ThreadStatusUi?,
    val preview: MessagePreview, // what a reply to this message quotes
    val source: ChatMessage?, // the server message (null for local rows)
) {
    val isLocal: Boolean get() = localId != null

    // Reactions and replies need the message to exist on the server. Call rows are written by the backend and are
    // inert: no reaction, reply, swipe, copy or unsend.
    val canReact: Boolean get() = localId == null && type != MessageType.CALL

    // Bubble-less content (big emoji / like) keeps its own look instead of a coloured bubble
    val bubbleless: Boolean get() = type == MessageType.LIKE || (type == MessageType.TEXT && emojiOnly)

    val copyText: String? get() = text.takeIf { type == MessageType.TEXT && it.isNotBlank() }
}

// One LazyColumn row. The list is reversed (index 0 = newest, at the bottom).
@Immutable
sealed interface ThreadItem {
    val key: String
    val contentType: Int

    @Immutable
    data class Message(val ui: ThreadMessageUi) : ThreadItem {
        override val key: String get() = ui.key
        override val contentType: Int
            get() = when {
                ui.type == MessageType.CALL -> TYPE_CALL
                ui.mine -> TYPE_MINE
                else -> TYPE_PEER
            }
    }

    @Immutable
    data class Separator(override val key: String, val label: String) : ThreadItem {
        override val contentType: Int get() = TYPE_SEPARATOR
    }

    @Immutable
    data object Typing : ThreadItem {
        override val key: String get() = "typing"
        override val contentType: Int get() = TYPE_TYPING
    }

    // Profile card at the very top once the whole history is loaded
    @Immutable
    data object Header : ThreadItem {
        override val key: String get() = "header"
        override val contentType: Int get() = TYPE_HEADER
    }

    // Top sentinel while older messages exist; becoming visible loads the next page
    @Immutable
    data object LoadingOlder : ThreadItem {
        override val key: String get() = "loading_older"
        override val contentType: Int get() = TYPE_LOADING
    }

    companion object {
        const val TYPE_MINE = 1
        const val TYPE_PEER = 2
        const val TYPE_SEPARATOR = 3
        const val TYPE_TYPING = 4
        const val TYPE_HEADER = 5
        const val TYPE_LOADING = 6
        const val TYPE_CALL = 7
    }
}

@Immutable
data class ThreadListUi(
    val items: List<ThreadItem> = emptyList(),
    val loaded: Boolean = false, // the first page arrived
    val hasOlder: Boolean = false,
    val loadingOlder: Boolean = false,
    val newestMessageKey: String? = null,
    val newestMessageAt: Long = 0L,
    val messageCount: Int = 0,
    // Local rows the server already has (a send that failed after it was written); the ViewModel drops them
    val deliveredLocalIds: List<String> = emptyList(),
) {
    // Peer messages newer than `since`, counted from the bottom (for the "N new messages" pill)
    fun peerMessagesAfter(since: Long): Int {
        var count = 0
        for (item in items) {
            val ui = (item as? ThreadItem.Message)?.ui ?: continue
            if (ui.createdAt <= since) break
            // A call row is not a message: it never adds to "N new messages"
            if (!ui.mine && ui.type != MessageType.CALL) count++
        }
        return count
    }

    fun indexOfMessage(messageId: String): Int =
        items.indexOfFirst { it is ThreadItem.Message && it.ui.id == messageId && it.ui.localId == null }
}

@Immutable
data class ThreadHeaderUi(
    val username: String,
    val name: String,
    val avatar: String?,
    val lastActive: Long?,
)

@Immutable
sealed interface ThreadContent {
    @Immutable
    data object Loading : ThreadContent

    @Immutable
    data object Ready : ThreadContent

    @Immutable
    data class Error(
        val title: String,
        val message: String,
        val retryable: Boolean = true,
        val notConfigured: Boolean = false,
    ) : ThreadContent
}

sealed interface ThreadEvent {
    data class Message(val text: String) : ThreadEvent
    data object ScrollToBottom : ThreadEvent
}

// ---- Local outgoing messages (see ThreadOutbox) ----

sealed interface ThreadLocalContent {
    val type: MessageType

    data class Text(val text: String) : ThreadLocalContent {
        override val type: MessageType get() = MessageType.TEXT
    }

    data object Like : ThreadLocalContent {
        override val type: MessageType get() = MessageType.LIKE
    }

    data class Image(val uri: Uri) : ThreadLocalContent {
        override val type: MessageType get() = MessageType.IMAGE
    }

    data class Artwork(val artwork: ArtworkRef) : ThreadLocalContent {
        override val type: MessageType get() = MessageType.ARTWORK
    }
}

sealed interface ThreadLocalPhase {
    data object Sending : ThreadLocalPhase
    data class Uploading(val progress: Float) : ThreadLocalPhase
    data class Failed(val reason: String?) : ThreadLocalPhase
}

data class ThreadUploadedImage(val url: String, val width: Int, val height: Int)

data class ThreadOutgoing(
    val localId: String,
    val owner: String,
    val peer: String,
    val conversationId: String,
    val content: ThreadLocalContent,
    val replyTo: MessagePreview?,
    val createdAt: Long,
    val phase: ThreadLocalPhase,
    // Message ids already in the thread when this was queued: the server copy is a message that is not in here
    val baseline: Set<String>,
    val uploaded: ThreadUploadedImage? = null,
    val previewKey: String,
) {
    val failed: Boolean get() = phase is ThreadLocalPhase.Failed
}
