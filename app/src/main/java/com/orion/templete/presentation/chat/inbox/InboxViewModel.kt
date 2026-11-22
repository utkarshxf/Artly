package com.orion.templete.presentation.chat.inbox

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
import com.orion.templete.domain.repository.chat.ChatRepository
import com.orion.templete.domain.repository.chat.ChatSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

private const val TAG = "ChatInbox"
private const val KEY_QUERY = "inbox_query"
private const val MAX_ACTIVE_NOW = 25
private const val GENERIC_ERROR = "We couldn't load your messages. Check your connection and try again."

// Raw inbox listener state
private sealed interface ConversationsLoad {
    data object Loading : ConversationsLoad
    data class Loaded(val conversations: List<Conversation>) : ConversationsLoad
    data class Failed(val error: Throwable) : ConversationsLoad
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InboxViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val chatSession: ChatSession,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val me: String = chatRepository.me

    // Search text lives in Compose state so the text field never lags behind the IME; it survives process death
    var query by mutableStateOf(savedStateHandle.get<String>(KEY_QUERY).orEmpty())
        private set
    private val queryFlow = MutableStateFlow(query)

    private val reload = MutableStateFlow(0)
    private val searchRetry = MutableStateFlow(0)

    // conversationId -> updatedAt at the moment the user deleted it; hidden until a newer message arrives
    private val hidden = MutableStateFlow<Map<String, Long>>(emptyMap())

    private val _events = Channel<String>(Channel.BUFFERED)
    val events: Flow<String> = _events.receiveAsFlow()

    private var lastLoad: ConversationsLoad = ConversationsLoad.Loading

    private val conversationsLoad: StateFlow<ConversationsLoad> = reload
        .flatMapLatest {
            chatRepository.observeConversations()
                .map<List<Conversation>, ConversationsLoad> { ConversationsLoad.Loaded(it) }
                .onStart {
                    // Only a retry after a failure shows the spinner again; re-subscribing keeps the last list
                    if (lastLoad is ConversationsLoad.Failed) {
                        emit(ConversationsLoad.Loading)
                        // Give a sign-in retry started by retry() a moment to leave the error state
                        withTimeoutOrNull(1_500L) { chatSession.authState.first { it !is ChatAuthState.Error } }
                    }
                }
                .catch { e ->
                    Log.w(TAG, "Inbox listener failed", e)
                    emit(ConversationsLoad.Failed(e))
                }
        }
        .onEach { lastLoad = it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConversationsLoad.Loading)

    private val profiles: StateFlow<Map<String, ChatUser>> = peerProfilesFlow(
        repository = chatRepository,
        peers = conversationsLoad
            .mapNotNull { load ->
                (load as? ConversationsLoad.Loaded)?.conversations
                    ?.filter { it.lastMessage != null }
                    ?.mapTo(LinkedHashSet()) { it.peer }
            }
            .distinctUntilChanged(),
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    // Wall clock for "Typing…", relative times and active dots: every second only while some row could be
    // typing, otherwise every 30 s (restarts on every inbox change)
    private val clock: Flow<Long> = conversationsLoad
        .map { (it as? ConversationsLoad.Loaded)?.conversations.orEmpty() }
        .flatMapLatest { conversations ->
            flow {
                while (true) {
                    val now = System.currentTimeMillis()
                    emit(now)
                    delay(if (conversations.any { it.isPeerTyping(now) }) 1_000L else 30_000L)
                }
            }
        }

    private val remoteSearch: StateFlow<RemoteSearch> = remoteSearchFlow(chatRepository, queryFlow, searchRetry)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RemoteSearch())

    val state: StateFlow<InboxUiState> = combine(
        combine(conversationsLoad, profiles, clock) { load, users, now -> Triple(load, users, now) },
        queryFlow,
        remoteSearch,
        chatSession.authState,
        hidden,
    ) { (load, users, now), rawQuery, remote, auth, hiddenIds ->
        InboxUiState(me = me, content = buildContent(load, users, now, rawQuery, remote, auth, hiddenIds))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InboxUiState(me = me))

    init {
        // The session may have failed earlier (e.g. backend asleep); opening the inbox tries again
        if (chatSession.authState.value is ChatAuthState.Error) chatSession.start()
        // Recover by itself once the session becomes ready after a failure
        viewModelScope.launch {
            chatSession.authState.collect { auth ->
                if (auth is ChatAuthState.Ready && conversationsLoad.value is ConversationsLoad.Failed) {
                    reload.update { it + 1 }
                }
            }
        }
    }

    fun onQueryChange(value: String) {
        query = value
        queryFlow.value = value
        savedStateHandle[KEY_QUERY] = value
    }

    fun retry() {
        if (chatSession.authState.value is ChatAuthState.Error) chatSession.start()
        reload.update { it + 1 }
    }

    fun retrySearch() {
        searchRetry.update { it + 1 }
    }

    fun setMuted(conversationId: String, muted: Boolean) {
        runAction(if (muted) "Couldn't mute this chat." else "Couldn't unmute this chat.") {
            chatRepository.setMuted(conversationId, muted)
        }
    }

    fun setUnread(conversationId: String, unread: Boolean) {
        runAction(if (unread) "Couldn't mark as unread." else "Couldn't mark as read.") {
            if (unread) chatRepository.setMarkedUnread(conversationId, true) else chatRepository.markRead(conversationId)
        }
    }

    fun deleteChat(conversationId: String) {
        val updatedAt = (conversationsLoad.value as? ConversationsLoad.Loaded)
            ?.conversations?.firstOrNull { it.id == conversationId }?.updatedAt
        if (updatedAt != null) hidden.update { it + (conversationId to updatedAt) }
        viewModelScope.launch {
            try {
                chatRepository.deleteForMe(conversationId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Delete chat failed", e)
                hidden.update { it - conversationId }
                _events.trySend("Couldn't delete this chat. Try again.")
            }
        }
    }

    private fun runAction(errorMessage: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, errorMessage, e)
                _events.trySend(errorMessage)
            }
        }
    }

    private fun buildContent(
        load: ConversationsLoad,
        users: Map<String, ChatUser>,
        now: Long,
        rawQuery: String,
        remote: RemoteSearch,
        auth: ChatAuthState,
        hiddenIds: Map<String, Long>,
    ): InboxContent = when (load) {
        ConversationsLoad.Loading -> InboxContent.Loading
        is ConversationsLoad.Failed -> {
            val authError = auth as? ChatAuthState.Error
            InboxContent.Error(
                message = authError?.message?.takeIf { it.isNotBlank() } ?: GENERIC_ERROR,
                notConfigured = authError?.notConfigured == true,
            )
        }
        is ConversationsLoad.Loaded -> {
            val rows = load.conversations
                .asSequence()
                .filter { it.peer.isNotBlank() && it.peer != me }
                // Like Instagram, a chat someone opened but never wrote in isn't listed (for either side)
                .filter { it.lastMessage != null }
                .filter { conversation -> hiddenIds[conversation.id]?.let { it != conversation.updatedAt } ?: true }
                .map { buildInboxRow(it, users[it.peer], me, now) }
                .toList()
            val activeNow = rows.asSequence()
                .filter { it.active }
                .distinctBy { it.peer }
                .take(MAX_ACTIVE_NOW)
                .map { InboxPersonUi(username = it.peer, name = it.title, avatar = it.avatar, active = true) }
                .toList()
            val q = normalizeQuery(rawQuery)
            val matches = if (q.isEmpty()) emptyList() else rows.filter { it.matches(q) }
            val shown = matches.mapTo(HashSet()) { it.peer }
            val people = if (q.isEmpty()) {
                emptyList()
            } else {
                remote.users.asSequence()
                    .filter { it.username.isNotBlank() && !it.username.equals(me, ignoreCase = true) }
                    .filter { it.username !in shown }
                    .distinctBy { it.username }
                    .map { user -> users[user.username]?.toPersonUi(now) ?: user.toPersonUi(now) }
                    .toList()
            }
            InboxContent.Ready(
                rows = rows,
                activeNow = activeNow,
                searchQuery = q,
                searchMatches = matches,
                people = people,
                peopleLoading = q.isNotEmpty() && (remote.loading || remote.query != q),
                peopleFailed = q.isNotEmpty() && remote.failed && remote.query == q,
            )
        }
    }
}
