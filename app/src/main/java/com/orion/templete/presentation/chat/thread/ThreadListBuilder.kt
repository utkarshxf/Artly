package com.orion.templete.presentation.chat.thread

import android.text.format.DateFormat
import com.orion.templete.data.model.chat.CallLog
import com.orion.templete.data.model.chat.ChatMessage
import com.orion.templete.data.model.chat.Conversation
import com.orion.templete.data.model.chat.MessagePage
import com.orion.templete.data.model.chat.MessagePreview
import com.orion.templete.data.model.chat.MessageType
import com.orion.templete.presentation.chat.components.ChatTime

// Everything the list is built from (one immutable snapshot)
data class ThreadBuildInput(
    val page: MessagePage?, // null until the first page arrives
    val loadingOlder: Boolean,
    val local: List<ThreadOutgoing>,
    val conversation: Conversation?,
    val peerTyping: Boolean,
    val peerName: String,
    val now: Long,
)

// Turns messages into list rows: merges local outgoing rows, hides unsent messages, groups bubbles, adds time
// separators, statuses, the typing bubble and the profile header. Pure apart from a small text-analysis cache;
// called sequentially from one flow on a background dispatcher.
class ThreadListBuilder(
    private val me: String,
    private val previewKeyFor: (String?) -> String?,
) {
    private val textCache = HashMap<String, ThreadText.Info>()

    private class Entry(
        val key: String,
        val server: ChatMessage?,
        val local: ThreadOutgoing?,
        val sender: String,
        val createdAt: Long,
        val hasReply: Boolean,
        // A call card: stands alone (never grouped with bubbles) and never carries "Sent" / "Seen"
        val isCall: Boolean = false,
    )

    fun build(input: ThreadBuildInput): ThreadListUi {
        val page = input.page
        val server = page?.messages.orEmpty()
        val hasOlder = page?.hasOlder == true

        // 1. Local rows whose server copy is already in the listener are hidden (the server copy takes over)
        val claimed = HashSet<String>()
        val visibleLocal = ArrayList<ThreadOutgoing>(input.local.size)
        val delivered = ArrayList<String>()
        for (local in input.local.sortedBy { it.createdAt }) {
            val match = findServerCopy(local, server, claimed)
            if (match != null) {
                claimed += match.id
                if (local.failed && !match.pending) delivered += local.localId
                continue
            }
            visibleLocal += local
        }

        // 2. Visible entries, oldest first
        val byId = HashMap<String, ChatMessage>(server.size * 2)
        server.forEach { byId[it.id] = it }
        val entries = ArrayList<Entry>(server.size + visibleLocal.size)
        server.forEach { m ->
            if (!m.unsent) {
                entries += Entry(
                    key = "m:${m.id}",
                    server = m,
                    local = null,
                    sender = m.sender,
                    createdAt = m.createdAt,
                    hasReply = m.replyTo != null,
                    isCall = m.type == MessageType.CALL,
                )
            }
        }
        visibleLocal.forEach { l ->
            entries += Entry("l:${l.localId}", null, l, me, l.createdAt, l.replyTo != null)
        }

        // 3. Separators and grouping
        val count = entries.size
        val separatorBefore = BooleanArray(count)
        val groupedWithPrev = BooleanArray(count)
        for (i in 0 until count) {
            val entry = entries[i]
            if (i == 0) {
                separatorBefore[i] = page != null && !hasOlder
                continue
            }
            val prev = entries[i - 1]
            val gap = entry.createdAt - prev.createdAt
            separatorBefore[i] = gap > SEPARATOR_GAP_MS
            groupedWithPrev[i] = !separatorBefore[i] &&
                prev.sender == entry.sender &&
                gap < GROUP_GAP_MS &&
                !entry.hasReply &&
                // The bubbles around a call card keep their own rounded corners and avatar
                !entry.isCall &&
                !prev.isCall
        }

        // 4. Status under my newest message (and under every failed one). Call rows are skipped: a call I placed
        // is not a message I "sent", and a call after my last message doesn't take its "Seen" away.
        val newest = entries.lastOrNull()
        val newestMessage = entries.lastOrNull { !it.isCall }
        val conversation = input.conversation
        val peerLastRead = conversation?.peerLastRead ?: 0L

        val rows = ArrayList<ThreadItem>(count * 2 + 3)
        // Built newest -> oldest because the list is reversed
        if (input.peerTyping) rows += ThreadItem.Typing
        for (i in count - 1 downTo 0) {
            val entry = entries[i]
            val groupedWithNext = i + 1 < count && groupedWithPrev[i + 1]
            val isNewest = entry === newestMessage
            val ui = if (entry.server != null) {
                serverRow(entry, entry.server, byId, input, groupedWithPrev[i], groupedWithNext, isNewest, peerLastRead)
            } else {
                localRow(entry, entry.local!!, byId, input, groupedWithPrev[i], groupedWithNext, isNewest)
            }
            rows += ThreadItem.Message(ui)
            if (separatorBefore[i]) {
                rows += ThreadItem.Separator("sep:${entry.key}", ChatTime.separator(entry.createdAt, input.now))
            }
        }
        if (page != null) {
            rows += if (hasOlder) ThreadItem.LoadingOlder else ThreadItem.Header
        }

        if (textCache.size > TEXT_CACHE_LIMIT) textCache.clear()

        return ThreadListUi(
            items = rows,
            loaded = page != null,
            hasOlder = hasOlder,
            loadingOlder = input.loadingOlder,
            newestMessageKey = newest?.key,
            newestMessageAt = newest?.createdAt ?: 0L,
            messageCount = count,
            deliveredLocalIds = delivered,
        )
    }

    private fun findServerCopy(local: ThreadOutgoing, server: List<ChatMessage>, claimed: Set<String>): ChatMessage? {
        for (i in server.indices.reversed()) {
            val m = server[i]
            if (m.id in claimed || m.sender != me || m.id in local.baseline) continue
            if (m.type != local.content.type) continue
            if (!m.pending && m.createdAt < local.createdAt - MATCH_WINDOW_MS) break // older than the local row
            val same = when (val content = local.content) {
                is ThreadLocalContent.Text -> m.text == content.text
                is ThreadLocalContent.Like -> true
                is ThreadLocalContent.Artwork -> m.artwork?.id == content.artwork.id
                is ThreadLocalContent.Image -> local.uploaded != null && m.imageUrl == local.uploaded.url
            }
            if (same) return m
        }
        return null
    }

    private fun serverRow(
        entry: Entry,
        m: ChatMessage,
        byId: Map<String, ChatMessage>,
        input: ThreadBuildInput,
        groupedWithPrev: Boolean,
        groupedWithNext: Boolean,
        isNewest: Boolean,
        peerLastRead: Long,
    ): ThreadMessageUi {
        val mine = m.sender == me
        val isCall = m.type == MessageType.CALL
        val info = if (!isCall && (m.type == MessageType.TEXT || m.text.isNotBlank())) analyze(m.text) else null
        val status = when {
            isCall || !mine || !isNewest -> null
            m.pending -> ThreadStatusUi(ThreadStatusKind.SENDING, STATUS_SENDING)
            peerLastRead > 0 && peerLastRead >= m.createdAt ->
                ThreadStatusUi(ThreadStatusKind.SEEN, ChatTime.seen(peerLastRead, input.now))
            else -> ThreadStatusUi(ThreadStatusKind.SENT, STATUS_SENT)
        }
        return ThreadMessageUi(
            key = entry.key,
            id = m.id,
            localId = null,
            mine = mine,
            sender = m.sender,
            type = m.type,
            text = m.text,
            links = info?.links.orEmpty(),
            emojiOnly = m.type == MessageType.TEXT && info?.emojiOnly == true,
            image = m.imageUrl?.takeIf { m.type == MessageType.IMAGE && it.isNotBlank() }?.let { url ->
                ThreadImageUi(
                    model = url,
                    width = m.imageWidth,
                    height = m.imageHeight,
                    placeholderKey = previewKeyFor(url),
                )
            },
            artwork = m.artwork.takeIf { m.type == MessageType.ARTWORK },
            profile = m.profile.takeIf { m.type == MessageType.PROFILE },
            call = if (isCall) callUi(m, mine) else null,
            // Clients never reply to or react on a call row; anything stored there is ignored
            replyTo = if (isCall) null else m.replyTo?.let { replyUi(mine, it, byId, input.peerName) },
            reactions = if (isCall) null else reactionsUi(m.reactions),
            createdAt = m.createdAt,
            pending = m.pending,
            failed = false,
            uploadProgress = null,
            groupedWithPrev = groupedWithPrev,
            groupedWithNext = groupedWithNext,
            showAvatar = !mine && !groupedWithNext,
            status = status,
            preview = previewOf(m),
            source = m,
        )
    }

    private fun localRow(
        entry: Entry,
        l: ThreadOutgoing,
        byId: Map<String, ChatMessage>,
        input: ThreadBuildInput,
        groupedWithPrev: Boolean,
        groupedWithNext: Boolean,
        isNewest: Boolean,
    ): ThreadMessageUi {
        val content = l.content
        val text = (content as? ThreadLocalContent.Text)?.text.orEmpty()
        val info = if (content is ThreadLocalContent.Text) analyze(text) else null
        val phase = l.phase
        val status = when {
            phase is ThreadLocalPhase.Failed -> ThreadStatusUi(ThreadStatusKind.FAILED, STATUS_FAILED)
            isNewest && phase !is ThreadLocalPhase.Uploading -> ThreadStatusUi(ThreadStatusKind.SENDING, STATUS_SENDING)
            else -> null
        }
        val image = (content as? ThreadLocalContent.Image)?.let {
            ThreadImageUi(
                model = it.uri,
                width = l.uploaded?.width,
                height = l.uploaded?.height,
                memoryKey = l.previewKey,
            )
        }
        val preview = MessagePreview(
            id = l.localId,
            sender = me,
            type = content.type,
            preview = previewText(content.type, text),
            createdAt = l.createdAt,
        )
        return ThreadMessageUi(
            key = entry.key,
            id = l.localId,
            localId = l.localId,
            mine = true,
            sender = me,
            type = content.type,
            text = text,
            links = info?.links.orEmpty(),
            emojiOnly = info?.emojiOnly == true,
            image = image,
            artwork = (content as? ThreadLocalContent.Artwork)?.artwork,
            profile = null, // profiles are shared from the share sheet, never from the composer
            call = null, // calls are logged by the backend, never sent from here
            replyTo = l.replyTo?.let { replyUi(true, it, byId, input.peerName) },
            reactions = null,
            createdAt = l.createdAt,
            pending = true,
            failed = phase is ThreadLocalPhase.Failed,
            uploadProgress = (phase as? ThreadLocalPhase.Uploading)?.progress,
            groupedWithPrev = groupedWithPrev,
            groupedWithNext = groupedWithNext,
            showAvatar = false,
            status = status,
            preview = preview,
            source = null,
        )
    }

    private fun replyUi(
        mine: Boolean,
        reply: MessagePreview,
        byId: Map<String, ChatMessage>,
        peerName: String,
    ): ThreadReplyUi {
        val original = byId[reply.id]
        val unavailable = original?.unsent == true
        val firstName = peerName.trim().substringBefore(' ').ifBlank { peerName }
        val toMe = reply.sender == me
        val label = when {
            mine && toMe -> "You replied to yourself"
            mine -> "You replied to $firstName"
            toMe -> "Replied to you"
            else -> "$firstName replied to themselves"
        }
        val preview = when {
            unavailable -> "Message unavailable"
            reply.preview.isNotBlank() -> reply.preview
            else -> previewText(reply.type, "")
        }
        val thumbnail = original
            ?.takeIf { !unavailable && it.type == MessageType.IMAGE }
            ?.imageUrl
            ?.takeIf { it.isNotBlank() }
            ?: original?.takeIf { !unavailable && it.type == MessageType.ARTWORK }?.artwork?.imageUrl?.takeIf { it.isNotBlank() }
            ?: original?.takeIf { !unavailable && it.type == MessageType.PROFILE }?.profile?.avatar?.takeIf { it.isNotBlank() }
        return ThreadReplyUi(
            targetId = reply.id,
            label = label,
            preview = preview,
            thumbnailUrl = thumbnail,
            unavailable = unavailable,
        )
    }

    private fun reactionsUi(reactions: Map<String, String>): ThreadReactionsUi? {
        if (reactions.isEmpty()) return null
        val valid = reactions.filter { (user, emoji) -> user.isNotBlank() && emoji.isNotBlank() }
        if (valid.isEmpty()) return null
        val emojis = valid.values
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(3)
        val byUser = valid.entries
            .sortedWith(compareBy({ it.key != me }, { it.key }))
            .map { ThreadReaction(it.key, it.value) }
        return ThreadReactionsUi(emojis = emojis, count = valid.size, mine = valid[me], byUser = byUser)
    }

    // A call row, worded for whoever is looking at it. It is built from the `call` map, never from the text
    // (which is written for the person who was called); the sender of a call message is always the caller.
    private fun callUi(m: ChatMessage, mine: Boolean): ThreadCallUi {
        val time = DateFormat.format("HH:mm", m.createdAt).toString()
        val call = m.call
            // Malformed document: a neutral row with whatever the backend wrote, and nothing to call back
            ?: return ThreadCallUi(
                video = false,
                missed = false,
                title = m.text.trim().ifEmpty { "Call" },
                detail = time,
                action = null,
            )
        val kind = if (call.video) "Video call" else "Audio call"
        return when {
            call.answered -> ThreadCallUi(
                video = call.video,
                missed = false,
                title = kind,
                detail = listOfNotNull(durationLabel(call.durationSec), time).joinToString(" · "),
                action = if (mine) "Call again" else "Call back",
            )
            // My own call that nobody took (rang out, they were busy, I gave up) or that they declined
            mine -> ThreadCallUi(
                video = call.video,
                missed = false,
                title = if (call.outcome == CallLog.OUTCOME_DECLINED) "Declined" else "No answer",
                detail = "$kind · $time",
                action = "Call again",
            )
            // A call I didn't take
            else -> ThreadCallUi(
                video = call.video,
                missed = true,
                title = (if (call.outcome == CallLog.OUTCOME_DECLINED) "Declined " else "Missed ") +
                    (if (call.video) "video call" else "audio call"),
                detail = time,
                action = "Call back",
            )
        }
    }

    // "45 sec", "2 min", "1 hr 5 min"; null when there is nothing to show
    private fun durationLabel(seconds: Int): String? = when {
        seconds <= 0 -> null
        seconds < 60 -> "$seconds sec"
        seconds < 3600 -> "${seconds / 60} min"
        else -> {
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            if (minutes == 0) "$hours hr" else "$hours hr $minutes min"
        }
    }

    private fun analyze(text: String): ThreadText.Info = textCache.getOrPut(text) { ThreadText.analyze(text) }

    companion object {
        const val STATUS_SENDING = "Sending…"
        const val STATUS_SENT = "Sent"
        const val STATUS_FAILED = "Not delivered · Tap to retry"

        private const val MINUTE = 60_000L
        private const val SEPARATOR_GAP_MS = 30 * MINUTE
        private const val GROUP_GAP_MS = 10 * MINUTE
        private const val MATCH_WINDOW_MS = 5 * MINUTE
        private const val TEXT_CACHE_LIMIT = 600

        // Wire-format previews (lastMessage / replyTo)
        fun previewText(type: MessageType, text: String): String = when (type) {
            MessageType.TEXT -> text.trim().take(120)
            MessageType.IMAGE -> "Sent a photo"
            MessageType.ARTWORK -> "Shared a post"
            MessageType.PROFILE -> "Shared a profile"
            MessageType.LIKE -> THREAD_HEART
            // The backend writes the wording ("Missed video call"); call rows are never quoted in a reply
            MessageType.CALL -> text.trim().take(120).ifEmpty { "Call" }
        }

        fun previewOf(message: ChatMessage): MessagePreview = MessagePreview(
            id = message.id,
            sender = message.sender,
            type = message.type,
            preview = previewText(message.type, message.text),
            createdAt = message.createdAt,
        )
    }
}
