package com.orion.templete.presentation.chat.inbox

import android.util.Log
import androidx.compose.runtime.Immutable
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

private const val TAG = "ChatNewMessage"
private const val KEY_QUERY = "new_message_query"
private const val MAX_SUGGESTIONS = 30
private const val MAX_SUGGESTION_RETRIES = 5

@Immutable
data class NewMessageUiState(
    val suggestionsLoading: Boolean = true,
    val suggestions: List<InboxPersonUi> = emptyList(), // recent chat peers
    val searchQuery: String = "", // normalized; empty = show suggestions
    val results: List<InboxPersonUi> = emptyList(), // matching recent peers first, then people from the backend
    val searching: Boolean = false, // debouncing or waiting for the backend
    val searchFailed: Boolean = false,
)

@HiltViewModel
class NewMessageViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val chatSession: ChatSession,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val me: String = chatRepository.me

    // Search text lives in Compose state so the text field never lags behind the IME; it survives process death
    var query by mutableStateOf(savedStateHandle.get<String>(KEY_QUERY).orEmpty())
        private set
    private val queryFlow = MutableStateFlow(query)
    private val searchRetry = MutableStateFlow(0)

    // Recent chat peers in inbox order; null while loading. Suggestions are optional on this screen: when the inbox
    // listener fails they are simply empty (people search still works - it goes to the backend) and the listener is
    // attached again once the chat session is (re)established.
    private val recentPeers: StateFlow<List<String>?> = chatRepository.observeConversations()
        .map<List<Conversation>, List<String>?> { conversations ->
            conversations.asSequence()
                // Same rule as the inbox: a chat nobody wrote in yet doesn't count as a recent chat
                .filter { it.lastMessage != null }
                .map { it.peer }
                .filter { it.isNotBlank() && it != me }
                .distinct()
                .toList()
        }
        .retryWhen { cause, attempt ->
            Log.w(TAG, "Recent chats unavailable (attempt ${attempt + 1})", cause)
            emit(emptyList())
            if (attempt >= MAX_SUGGESTION_RETRIES) return@retryWhen false
            delay((2_000L shl attempt.toInt()).coerceAtMost(30_000L))
            chatSession.authState.first { it is ChatAuthState.Ready }
            true
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val profiles: StateFlow<Map<String, ChatUser>> = peerProfilesFlow(
        repository = chatRepository,
        peers = recentPeers
            .map { it.orEmpty().take(MAX_SUGGESTIONS).toSet() }
            .distinctUntilChanged(),
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val remoteSearch: StateFlow<RemoteSearch> = remoteSearchFlow(chatRepository, queryFlow, searchRetry)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RemoteSearch())

    val state: StateFlow<NewMessageUiState> = combine(
        recentPeers,
        profiles,
        queryFlow,
        remoteSearch,
    ) { peers, users, rawQuery, remote ->
        val now = System.currentTimeMillis()
        val recent = peers.orEmpty().map { peer ->
            users[peer]?.toPersonUi(now) ?: InboxPersonUi(username = peer, name = peer, avatar = null, active = false)
        }
        val q = normalizeQuery(rawQuery)
        val results = if (q.isEmpty()) {
            emptyList()
        } else {
            val local = recent.filter { it.matches(q) }
            val shown = local.mapTo(HashSet()) { it.username }
            val fromServer = remote.users.asSequence()
                .filter { it.username.isNotBlank() && !it.username.equals(me, ignoreCase = true) }
                .filter { it.username !in shown }
                .distinctBy { it.username }
                .map { user -> users[user.username]?.toPersonUi(now) ?: user.toPersonUi(now) }
                .toList()
            local + fromServer
        }
        NewMessageUiState(
            suggestionsLoading = peers == null,
            suggestions = recent.take(MAX_SUGGESTIONS),
            searchQuery = q,
            results = results,
            searching = q.isNotEmpty() && (remote.loading || remote.query != q),
            searchFailed = q.isNotEmpty() && remote.failed && remote.query == q,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NewMessageUiState())

    init {
        // Opening a conversation from here needs the chat session; a session that failed earlier tries again now
        if (chatSession.authState.value is ChatAuthState.Error) chatSession.start()
    }

    fun onQueryChange(value: String) {
        query = value
        queryFlow.value = value
        savedStateHandle[KEY_QUERY] = value
    }

    fun retrySearch() {
        searchRetry.update { it + 1 }
    }
}
