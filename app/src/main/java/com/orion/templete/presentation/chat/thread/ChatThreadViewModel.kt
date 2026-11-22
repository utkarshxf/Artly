package com.orion.templete.presentation.chat.thread

import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orion.templete.data.model.chat.ChatAuthState
import com.orion.templete.data.model.chat.ChatUser
import com.orion.templete.data.model.chat.Conversation
import com.orion.templete.data.model.chat.MessagePage
import com.orion.templete.data.model.chat.MessagePreview
import com.orion.templete.data.model.chat.MessageType
import com.orion.templete.domain.call.CallManager
import com.orion.templete.domain.repository.chat.ChatRepository
import com.orion.templete.domain.repository.chat.ChatSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.math.abs

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatThreadViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val session: ChatSession,
    private val outbox: ThreadOutbox,
    private val callManager: CallManager,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // Raw thread listener state
    private sealed interface PageLoad {
        data object Loading : PageLoad
        data class Loaded(val page: MessagePage, val limit: Int) : PageLoad
        data class Failed(val error: Throwable) : PageLoad
    }

    val peer: String = savedStateHandle.get<String>(ARG_PEER)?.let(::decodeArg)?.trim().orEmpty()
    val me: String = try {
        repository.me.trim()
    } catch (e: Exception) {
        Log.w(TAG, "No signed-in user", e)
        ""
    }

    private val setupError: ThreadContent.Error? = when {
        me.isEmpty() -> ThreadContent.Error(
            title = "You're signed out",
            message = "Log in to use messages.",
            retryable = false,
        )
        peer.isEmpty() -> ThreadContent.Error(
            title = "Chat unavailable",
            message = "This conversation couldn't be opened.",
            retryable = false,
        )
        peer == me -> ThreadContent.Error(
            title = "Chat unavailable",
            message = "You can't send messages to yourself.",
            retryable = false,
        )
        else -> null
    }

    val conversationId: String = if (setupError != null) "" else try {
        repository.conversationIdWith(peer)
    } catch (e: Exception) {
        Log.w(TAG, "conversationIdWith failed", e)
        listOf(me, peer).sorted().joinToString("__")
    }

    private val active: Boolean get() = setupError == null

    // ---- Composer (Compose state so the text field never lags; both survive process death) ----

    var draft by mutableStateOf(savedStateHandle.get<String>(KEY_DRAFT).orEmpty())
        private set

    var replyTarget by mutableStateOf(restoreReply())
        private set

    private val _events = Channel<ThreadEvent>(Channel.BUFFERED)
    val events: Flow<ThreadEvent> = _events.receiveAsFlow()

    // ---- Sources ----

    private val reload = MutableStateFlow(0)
    private val limit = MutableStateFlow((savedStateHandle.get<Int>(KEY_LIMIT) ?: THREAD_PAGE_SIZE).coerceAtLeast(THREAD_PAGE_SIZE))
    private val resumed = MutableStateFlow(false)
    private var lastLoad: PageLoad = PageLoad.Loading

    private val pageLoad: StateFlow<PageLoad> = if (!active) {
        MutableStateFlow(PageLoad.Loading)
    } else {
        reload
            .flatMapLatest {
                limit
                    .flatMapLatest { n ->
                        repository.observeMessages(conversationId, n).map<MessagePage, PageLoad> { PageLoad.Loaded(it, n) }
                    }
                    .onStart {
                        // Only a retry after a failure shows the spinner again
                        if (lastLoad is PageLoad.Failed) {
                            emit(PageLoad.Loading)
                            withTimeoutOrNull(1_500L) { session.authState.first { it !is ChatAuthState.Error } }
                        }
                    }
                    .catch { e ->
                        if (e is CancellationException) throw e
                        Log.w(TAG, "Thread listener failed", e)
                        emit(PageLoad.Failed(e))
                    }
            }
            .onEach { lastLoad = it }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PageLoad.Loading)
    }

    // null until it exists (the first message creates it). Failures are not fatal: messages still load and the
    // first send creates / re-reads it.
    private val conversation: StateFlow<Conversation?> = if (!active) {
        MutableStateFlow(null)
    } else {
        reload
            .flatMapLatest {
                repository.observeConversation(conversationId).retryWhen { cause, attempt -> retryListener(cause, attempt) }.catch { e ->
                    if (e is CancellationException) throw e
                    Log.w(TAG, "Conversation listener failed", e)
                    emit(null)
                }
            }
            .onEach { if (it != null) outbox.markConversationExists(conversationId) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    }

    private val peerUser: StateFlow<ChatUser?> = if (!active) {
        MutableStateFlow(null)
    } else {
        reload
            .flatMapLatest {
                repository.observeUser(peer).retryWhen { cause, attempt -> retryListener(cause, attempt) }.catch { e ->
                    if (e is CancellationException) throw e
                    Log.w(TAG, "Peer profile listener failed", e)
                    emit(null)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    }

    private val local: Flow<List<ThreadOutgoing>> = if (!active) flowOf(emptyList()) else outbox.observe(me, conversationId)

    // Peer's newest message time (typing only shows when it is newer)
    private val lastPeerMessageAt: Flow<Long> = pageLoad
        .map { load ->
            (load as? PageLoad.Loaded)?.page?.messages?.lastOrNull { it.sender == peer }?.createdAt ?: 0L
        }
        .distinctUntilChanged()

    // "typing" = the peer's typing timestamp changed within the last 6 s and is newer than their last message.
    // The change is timed with the local clock (device clocks drift); stale timestamps (> 1 min) are ignored.
    private val peerTyping: Flow<Boolean> = combine(
        conversation.map { it?.peerTypingAt ?: 0L }.distinctUntilChanged(),
        lastPeerMessageAt,
    ) { typingAt, lastMessageAt -> typingAt to lastMessageAt }
        .flatMapLatest { (typingAt, lastMessageAt) ->
            flow {
                val now = System.currentTimeMillis()
                val fresh = typingAt > 0 && typingAt > lastMessageAt && abs(now - typingAt) < TYPING_STALE_MS
                if (fresh) {
                    emit(true)
                    delay(TYPING_VISIBLE_MS)
                }
                emit(false)
            }
        }
        .distinctUntilChanged()

    // Relative labels ("Seen 5m ago", "Today 10:42") refresh twice a minute
    private val clock: Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(30_000L)
        }
    }

    private val builder = ThreadListBuilder(me = me, previewKeyFor = outbox::previewKeyFor)

    val list: StateFlow<ThreadListUi> = if (!active) {
        MutableStateFlow(ThreadListUi())
    } else {
        combine(
            combine(pageLoad, limit) { load, requested -> load to requested },
            local,
            conversation,
            peerTyping,
            combine(clock, peerUser) { now, user -> now to (user?.displayName ?: peer) },
        ) { (load, requested), localItems, conv, typing, (now, peerName) ->
            val loaded = load as? PageLoad.Loaded
            ThreadBuildInput(
                page = loaded?.page,
                loadingOlder = loaded != null && loaded.page.hasOlder && requested > loaded.limit,
                local = localItems,
                conversation = conv,
                peerTyping = typing,
                peerName = peerName,
                now = now,
            )
        }
            .map { builder.build(it) }
            .flowOn(Dispatchers.Default)
            .onEach { ui -> ui.deliveredLocalIds.forEach(outbox::discard) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThreadListUi())
    }

    val header: StateFlow<ThreadHeaderUi> = peerUser
        .map { user -> headerOf(user) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), headerOf(null))

    val content: StateFlow<ThreadContent> = if (setupError != null) {
        MutableStateFlow(setupError)
    } else {
        combine(pageLoad, session.authState) { load, auth ->
            when (load) {
                is PageLoad.Loaded -> ThreadContent.Ready
                is PageLoad.Failed -> errorFor(auth)
                PageLoad.Loading -> if (auth is ChatAuthState.Error) errorFor(auth) else ThreadContent.Loading
            }
        }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThreadContent.Loading)
    }

    // ---- Calls ----

    // The audio / video buttons and "Call back" on call rows. Hidden only once the server says calls are off:
    // while that isn't known yet (null) they are offered, so they don't pop in a moment after the chat opens.
    // Never for a chat that can't exist (no peer, or yourself).
    val canCall: StateFlow<Boolean> = if (!active) {
        MutableStateFlow(false)
    } else {
        callManager.callsEnabled
            .map { enabled -> enabled != false }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), callManager.callsEnabled.value != false)
    }

    // ---- Typing ----
    private var typingSent = false
    private var lastTypingWrite = 0L
    private var typingIdleJob: Job? = null

    // ---- Read receipts ----
    private var lastReadKey: String? = null

    init {
        if (active) {
            // Asks whether calls are set up; it also wakes a sleeping backend before the first call is placed
            callManager.refreshConfig()
            // The session may have failed earlier (e.g. the backend was asleep); opening a chat tries again
            if (session.authState.value is ChatAuthState.Error) session.start()
            viewModelScope.launch {
                session.authState.collect { auth ->
                    if (auth is ChatAuthState.Ready && lastLoad is PageLoad.Failed) reload.update { it + 1 }
                }
            }
            viewModelScope.launch { markReadWhileVisible() }
        }
    }

    // ---- Lifecycle (from the screen) ----

    fun onResume() {
        if (!active) return
        session.activeConversationId = conversationId
        resumed.value = true
    }

    fun onPause() {
        resumed.value = false
    }

    fun onStop() {
        stopTyping()
    }

    // The screen left composition (navigated away)
    fun onLeave() {
        resumed.value = false
        stopTyping()
        if (active && session.activeConversationId == conversationId) session.activeConversationId = null
    }

    override fun onCleared() {
        onLeave()
        super.onCleared()
    }

    fun retry() {
        if (!active) return
        if (session.authState.value is ChatAuthState.Error) session.start()
        reload.update { it + 1 }
    }

    // Grows the window by one page; false when there is nothing older (or a page is already loading)
    fun loadOlder(): Boolean {
        val load = pageLoad.value as? PageLoad.Loaded ?: return false
        if (!load.page.hasOlder || limit.value > load.limit) return false
        val next = load.limit + THREAD_PAGE_SIZE
        limit.value = next
        savedStateHandle[KEY_LIMIT] = next
        return true
    }

    // ---- Composer ----

    fun onDraftChange(value: String) {
        val text = if (value.length > THREAD_MAX_TEXT) value.take(THREAD_MAX_TEXT) else value
        draft = text
        savedStateHandle[KEY_DRAFT] = text
        if (text.isBlank()) {
            stopTyping()
        } else {
            onTyping()
        }
    }

    fun sendText() {
        val text = draft.trim()
        if (text.isEmpty() || !active) return
        draft = ""
        savedStateHandle[KEY_DRAFT] = ""
        enqueue(ThreadLocalContent.Text(text))
    }

    fun sendLike() {
        if (!active) return
        enqueue(ThreadLocalContent.Like)
    }

    fun sendImage(uri: Uri) {
        if (!active) return
        enqueue(ThreadLocalContent.Image(uri))
    }

    fun startReply(message: ThreadMessageUi) {
        if (!message.canReact) return
        setReply(message.preview)
    }

    fun cancelReply() {
        setReply(null)
    }

    private fun enqueue(content: ThreadLocalContent) {
        val reply = replyTarget
        setReply(null)
        val baseline = (pageLoad.value as? PageLoad.Loaded)?.page?.messages?.mapTo(HashSet()) { it.id }.orEmpty()
        outbox.enqueue(
            owner = me,
            peer = peer,
            conversationId = conversationId,
            content = content,
            replyTo = reply,
            baseline = baseline,
        )
        // A send clears typing.<me> in the same write
        typingIdleJob?.cancel()
        typingSent = false
        lastTypingWrite = 0L
        _events.trySend(ThreadEvent.ScrollToBottom)
    }

    fun retrySend(message: ThreadMessageUi) {
        message.localId?.let(outbox::retry)
    }

    fun deleteFailed(message: ThreadMessageUi) {
        message.localId?.let(outbox::discard)
    }

    // ---- Message actions ----

    fun react(message: ThreadMessageUi, emoji: String?) {
        if (!message.canReact || !active) return
        viewModelScope.launch {
            try {
                repository.setReaction(conversationId, message.id, emoji)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Reaction failed", e)
                _events.trySend(ThreadEvent.Message("Couldn't react to this message. Try again."))
            }
        }
    }

    // Double tap: toggles ❤️. Returns true when a heart was added (the screen plays the pop animation).
    fun toggleHeart(message: ThreadMessageUi): Boolean {
        if (!message.canReact) return false
        val adding = message.reactions?.mine != THREAD_HEART
        react(message, if (adding) THREAD_HEART else null)
        return adding
    }

    fun unsend(message: ThreadMessageUi) {
        val source = message.source ?: return
        // canReact is false for call rows: the backend's call log can't be unsent
        if (!message.mine || !message.canReact || !active) return
        if (replyTarget?.id == message.id) setReply(null)
        viewModelScope.launch {
            try {
                repository.unsend(conversationId, source)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Unsend failed", e)
                _events.trySend(ThreadEvent.Message("Couldn't unsend this message. Try again."))
            }
        }
    }

    fun showMessage(text: String) {
        _events.trySend(ThreadEvent.Message(text))
    }

    // ---- Typing ----

    private fun onTyping() {
        if (!active) return
        val exists = conversation.value != null
        val now = SystemClock.elapsedRealtime()
        if (exists && (!typingSent || now - lastTypingWrite >= TYPING_THROTTLE_MS)) {
            typingSent = true
            lastTypingWrite = now
            viewModelScope.launch {
                try {
                    repository.setTyping(conversationId, true)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Typing update failed", e)
                }
            }
        }
        typingIdleJob?.cancel()
        typingIdleJob = viewModelScope.launch {
            delay(TYPING_IDLE_MS)
            stopTyping()
        }
    }

    private fun stopTyping() {
        typingIdleJob?.cancel()
        typingIdleJob = null
        if (!typingSent) return
        typingSent = false
        lastTypingWrite = 0L
        val id = conversationId
        // Outlives the screen: leaving the chat must still clear the indicator
        outbox.launchDetached("Clearing typing") { repository.setTyping(id, false) }
    }

    // ---- Read receipts: mark read on open and on every new peer message while the screen is resumed ----

    private suspend fun markReadWhileVisible() {
        resumed
            .flatMapLatest { visible ->
                if (!visible) {
                    emptyFlow<String?>()
                } else {
                    combine(conversation, pageLoad) { conv, load -> readKey(conv, load) }
                }
            }
            .distinctUntilChanged()
            .collect { key ->
                if (key == null || key == lastReadKey) return@collect
                lastReadKey = key
                viewModelScope.launch {
                    try {
                        repository.markRead(conversationId)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "Mark read failed", e)
                        if (lastReadKey == key) lastReadKey = null
                    }
                }
            }
    }

    // Something to mark read -> a key that changes once per new peer message; null = nothing to do
    private fun readKey(conv: Conversation?, load: PageLoad): String? {
        if (conv == null) return null
        val latestPeer = (load as? PageLoad.Loaded)?.page?.messages?.lastOrNull { it.sender == peer }
        val needsRead = conv.unreadCount > 0 ||
            conv.markedUnread ||
            (latestPeer != null && latestPeer.createdAt > conv.myLastRead)
        if (!needsRead) return null
        return "${latestPeer?.id}|${conv.markedUnread}"
    }

    // ---- Helpers ----

    // Secondary listeners (conversation, profile) retry a few times with backoff before giving up until Retry
    private suspend fun retryListener(cause: Throwable, attempt: Long): Boolean {
        if (cause is CancellationException || attempt >= LISTENER_RETRIES) return false
        delay(2_000L shl attempt.toInt())
        return true
    }

    private fun setReply(preview: MessagePreview?) {
        replyTarget = preview
        savedStateHandle[KEY_REPLY_ID] = preview?.id
        savedStateHandle[KEY_REPLY_SENDER] = preview?.sender
        savedStateHandle[KEY_REPLY_TYPE] = preview?.type?.wire
        savedStateHandle[KEY_REPLY_PREVIEW] = preview?.preview
        savedStateHandle[KEY_REPLY_AT] = preview?.createdAt
    }

    private fun restoreReply(): MessagePreview? {
        val id = savedStateHandle.get<String>(KEY_REPLY_ID) ?: return null
        val sender = savedStateHandle.get<String>(KEY_REPLY_SENDER) ?: return null
        return MessagePreview(
            id = id,
            sender = sender,
            type = MessageType.fromWire(savedStateHandle.get<String>(KEY_REPLY_TYPE)),
            preview = savedStateHandle.get<String>(KEY_REPLY_PREVIEW).orEmpty(),
            createdAt = savedStateHandle.get<Long>(KEY_REPLY_AT) ?: 0L,
        )
    }

    private fun headerOf(user: ChatUser?): ThreadHeaderUi = ThreadHeaderUi(
        username = peer,
        name = user?.displayName ?: peer,
        avatar = user?.avatar?.takeIf { it.isNotBlank() },
        lastActive = user?.lastActive,
    )

    private fun errorFor(auth: ChatAuthState): ThreadContent.Error = when (auth) {
        is ChatAuthState.Error -> ThreadContent.Error(
            title = if (auth.notConfigured) "Messages are coming soon" else "Couldn't connect to messages",
            message = auth.message.ifBlank { GENERIC_ERROR },
            retryable = true,
            notConfigured = auth.notConfigured,
        )
        else -> ThreadContent.Error(
            title = "Couldn't load this chat",
            message = GENERIC_ERROR,
            retryable = true,
        )
    }

    companion object {
        const val ARG_PEER = "peerUsername"

        private const val TAG = "ChatThread"
        private const val KEY_DRAFT = "thread_draft"
        private const val KEY_LIMIT = "thread_limit"
        private const val KEY_REPLY_ID = "thread_reply_id"
        private const val KEY_REPLY_SENDER = "thread_reply_sender"
        private const val KEY_REPLY_TYPE = "thread_reply_type"
        private const val KEY_REPLY_PREVIEW = "thread_reply_preview"
        private const val KEY_REPLY_AT = "thread_reply_at"

        private const val TYPING_THROTTLE_MS = 3_000L
        private const val TYPING_IDLE_MS = 5_000L
        private const val TYPING_VISIBLE_MS = 6_000L
        private const val TYPING_STALE_MS = 60_000L
        private const val GENERIC_ERROR = "Check your connection and try again."
        private const val LISTENER_RETRIES = 3L

        // Navigation already decodes path arguments; decoding again only touches %-escapes, never '+'
        private fun decodeArg(raw: String): String = try {
            if (raw.contains('%')) Uri.decode(raw) else raw
        } catch (e: Exception) {
            raw
        }
    }
}
