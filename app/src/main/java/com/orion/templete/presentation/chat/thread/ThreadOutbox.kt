package com.orion.templete.presentation.chat.thread

import android.util.Log
import com.orion.templete.data.model.chat.MessagePreview
import com.orion.templete.data.model.chat.OutgoingMessage
import com.orion.templete.domain.repository.chat.ChatRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Collections
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

// Messages this device is sending, until the server has them. App-wide so a photo keeps uploading (and a failed
// message stays retryable) after the user leaves the conversation.
//
// Text / like / artwork messages show up in the Firestore listener right away (latency compensation, flagged
// pending), so a local row only covers the moment before the write, an upload, or a failure. The thread hides a
// local row as soon as its server copy is in the listener.
@Singleton
class ThreadOutbox @Inject constructor(
    private val repository: ChatRepository,
) {
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate + CoroutineExceptionHandler { _, error ->
            Log.w(TAG, "Outbox task failed", error)
        }
    )

    private val items = MutableStateFlow<List<ThreadOutgoing>>(emptyList())
    private val jobs = ConcurrentHashMap<String, Job>()
    private val existingConversations: MutableSet<String> = Collections.newSetFromMap(ConcurrentHashMap())
    private val creationLocks = ConcurrentHashMap<String, Mutex>()

    // Uploaded photo URL -> memory-cache key of the local preview that was on screen while it uploaded
    private val previews = ConcurrentHashMap<String, String>()

    fun observe(owner: String, conversationId: String): Flow<List<ThreadOutgoing>> =
        items
            .map { all -> all.filter { it.owner == owner && it.conversationId == conversationId } }
            .distinctUntilChanged()

    fun previewKeyFor(url: String?): String? = url?.let { previews[it] }

    fun markConversationExists(conversationId: String) {
        existingConversations += conversationId
    }

    fun enqueue(
        owner: String,
        peer: String,
        conversationId: String,
        content: ThreadLocalContent,
        replyTo: MessagePreview?,
        baseline: Set<String>,
    ): String {
        val localId = "local-" + UUID.randomUUID().toString()
        val item = ThreadOutgoing(
            localId = localId,
            owner = owner,
            peer = peer,
            conversationId = conversationId,
            content = content,
            replyTo = replyTo,
            createdAt = System.currentTimeMillis(),
            phase = if (content is ThreadLocalContent.Image) ThreadLocalPhase.Uploading(0f) else ThreadLocalPhase.Sending,
            baseline = baseline,
            previewKey = "chat-preview-$localId",
        )
        items.update { it + item }
        start(localId)
        return localId
    }

    fun retry(localId: String) {
        val item = find(localId) ?: return
        if (!item.failed) return
        val phase = if (item.content is ThreadLocalContent.Image && item.uploaded == null) {
            ThreadLocalPhase.Uploading(0f)
        } else {
            ThreadLocalPhase.Sending
        }
        update(localId) { it.copy(phase = phase, createdAt = System.currentTimeMillis()) }
        start(localId)
    }

    fun discard(localId: String) {
        jobs.remove(localId)?.cancel()
        items.update { list -> list.filterNot { it.localId == localId } }
    }

    // Fire-and-forget work that must outlive the screen (e.g. clearing "typing…" when leaving the chat)
    fun launchDetached(what: String, block: suspend () -> Unit) {
        scope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "$what failed", e)
            }
        }
    }

    private fun start(localId: String) {
        jobs.remove(localId)?.cancel()
        val job = scope.launch {
            val item = find(localId) ?: return@launch
            try {
                val message = buildMessage(item)
                ensureConversation(item)
                repository.send(item.conversationId, message)
                // The Firestore listener has the message now (it was written locally before the server confirmed)
                items.update { list -> list.filterNot { it.localId == localId } }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Sending a message failed", e)
                update(localId) { it.copy(phase = ThreadLocalPhase.Failed(e.message)) }
            } finally {
                jobs.remove(localId, coroutineContext[Job])
            }
        }
        jobs[localId] = job
        if (job.isCompleted) jobs.remove(localId, job)
    }

    private suspend fun buildMessage(item: ThreadOutgoing): OutgoingMessage = when (val content = item.content) {
        is ThreadLocalContent.Text -> OutgoingMessage.Text(content.text, item.replyTo)
        is ThreadLocalContent.Like -> OutgoingMessage.Like(item.replyTo)
        is ThreadLocalContent.Artwork -> OutgoingMessage.Artwork(content.artwork, item.replyTo)
        is ThreadLocalContent.Image -> {
            val uploaded = item.uploaded ?: upload(item, content)
            OutgoingMessage.Image(
                url = uploaded.url,
                width = uploaded.width.takeIf { it > 0 },
                height = uploaded.height.takeIf { it > 0 },
                replyTo = item.replyTo,
            )
        }
    }

    private suspend fun upload(item: ThreadOutgoing, content: ThreadLocalContent.Image): ThreadUploadedImage {
        update(item.localId) { it.copy(phase = ThreadLocalPhase.Uploading(0f)) }
        val (url, width, height) = repository.uploadImage(item.conversationId, content.uri) { progress ->
            val value = progress.coerceIn(0f, 1f)
            update(item.localId) { current ->
                val phase = current.phase
                if (phase is ThreadLocalPhase.Uploading && value > phase.progress) {
                    current.copy(phase = ThreadLocalPhase.Uploading(value))
                } else {
                    current
                }
            }
        }
        val uploaded = ThreadUploadedImage(url, width, height)
        previews[url] = item.previewKey
        update(item.localId) { it.copy(uploaded = uploaded, phase = ThreadLocalPhase.Sending) }
        return uploaded
    }

    // The first message creates the conversation; concurrent sends must not both try to create it
    private suspend fun ensureConversation(item: ThreadOutgoing) {
        if (item.conversationId in existingConversations) return
        val lock = creationLocks.getOrPut(item.conversationId) { Mutex() }
        lock.withLock {
            if (item.conversationId in existingConversations) return
            repository.ensureConversation(item.peer)
            existingConversations += item.conversationId
        }
    }

    private fun find(localId: String): ThreadOutgoing? = items.value.firstOrNull { it.localId == localId }

    private inline fun update(localId: String, crossinline transform: (ThreadOutgoing) -> ThreadOutgoing) {
        items.update { list ->
            if (list.none { it.localId == localId }) list
            else list.map { if (it.localId == localId) transform(it) else it }
        }
    }

    private companion object {
        const val TAG = "ChatOutbox"
    }
}
