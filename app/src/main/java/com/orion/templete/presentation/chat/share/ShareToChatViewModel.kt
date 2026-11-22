package com.orion.templete.presentation.chat.share

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orion.templete.data.model.chat.ChatUser
import com.orion.templete.data.model.chat.OutgoingMessage
import com.orion.templete.domain.repository.chat.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ShareToChatUiState(
    val query: String = "",
    val note: String = "",
    // Peers of the user's most recent conversations, newest first
    val recent: List<ChatUser> = emptyList(),
    val loadingRecent: Boolean = true,
    val recentError: String? = null,
    // People found by the backend search for [query]
    val results: List<ChatUser> = emptyList(),
    val searching: Boolean = false,
    val searchError: String? = null,
    // Who the artwork will be sent to, in the order they were picked
    val selected: List<ChatUser> = emptyList(),
) {
    fun isSelected(user: ChatUser): Boolean = selected.any { it.username.equals(user.username, ignoreCase = true) }
}

// Instagram's share sheet: tap people to select them, then one "Send" delivers the artwork (post card) or profile
// (profile card), plus the optional note as a separate text message, to each of them.
@HiltViewModel
class ShareToChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val savedStateHandle: SavedStateHandle,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(ShareToChatUiState())
    val state: StateFlow<ShareToChatUiState> = _state.asStateFlow()

    private val me: String by lazy {
        try {
            chatRepository.me
        } catch (e: Exception) {
            ""
        }
    }

    private var payload: SharePayload? = null
    private var session: String? = null
    private var recentJob: Job? = null
    private var searchJob: Job? = null

    // Profiles resolved for recent / restored peers (only touched on the main thread)
    private val profiles = HashMap<String, ChatUser>()
    private val resolving = HashSet<String>()

    /**
     * Called every time the sheet is shown. A new [sessionId] (the sheet was closed and opened again) starts clean;
     * the same one (rotation, process death) keeps the search, the note and who was selected.
     */
    fun open(sessionId: String, payload: SharePayload) {
        this.payload = payload
        if (session == sessionId) return
        val restoring = savedStateHandle.get<String>(KEY_SESSION) == sessionId
        session = sessionId
        savedStateHandle[KEY_SESSION] = sessionId
        val query = if (restoring) savedStateHandle.get<String>(KEY_QUERY).orEmpty() else ""
        val note = if (restoring) savedStateHandle.get<String>(KEY_NOTE).orEmpty() else ""
        val selected = if (restoring) savedStateHandle.get<ArrayList<String>>(KEY_SELECTED).orEmpty() else emptyList()
        savedStateHandle[KEY_QUERY] = query
        savedStateHandle[KEY_NOTE] = note
        savedStateHandle[KEY_SELECTED] = ArrayList(selected)
        searchJob?.cancel()
        _state.update { old ->
            // Recent chats are kept (the listener stays attached) so reopening the sheet doesn't flicker
            ShareToChatUiState(
                query = query,
                note = note,
                recent = old.recent,
                loadingRecent = old.recent.isEmpty(),
                selected = selected.map { profiles[it] ?: ChatUser(username = it) },
            )
        }
        selected.forEach { resolveProfile(it) }
        if (recentJob?.isActive != true) loadRecent()
        if (query.isNotBlank()) search(query)
    }

    // Sheet dismissed: stop listening to the inbox (the last list is kept so reopening shows it instantly)
    fun close() {
        recentJob?.cancel()
        recentJob = null
        searchJob?.cancel()
        _state.update { it.copy(searching = false) }
    }

    fun retry() {
        loadRecent()
        val query = _state.value.query
        if (query.isNotBlank()) search(query)
    }

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value) }
        savedStateHandle[KEY_QUERY] = value
        search(value)
    }

    fun onNoteChange(value: String) {
        val note = value.take(MAX_NOTE_LENGTH)
        _state.update { it.copy(note = note) }
        savedStateHandle[KEY_NOTE] = note
    }

    // Tap on a person: select / unselect (Instagram caps a share at a handful of people)
    fun toggle(user: ChatUser) {
        val peer = user.username.trim()
        if (peer.isEmpty() || peer.equals(me, ignoreCase = true)) return
        val current = _state.value.selected
        val next = if (current.any { it.username.equals(peer, ignoreCase = true) }) {
            current.filterNot { it.username.equals(peer, ignoreCase = true) }
        } else {
            if (current.size >= MAX_SELECTED) {
                Toast.makeText(appContext, "You can send to up to $MAX_SELECTED people at once", Toast.LENGTH_SHORT).show()
                return
            }
            current + user
        }
        _state.update { it.copy(selected = next) }
        savedStateHandle[KEY_SELECTED] = ArrayList(next.map { it.username })
    }

    /**
     * Sends the artwork (and the note) to everyone selected and resets the sheet. Returns how many chats it was
     * sent to so the sheet can close right away, like Instagram; sending finishes in the background and a failure
     * is reported with a toast even after the sheet (or the artwork screen) is gone.
     */
    fun sendToSelected(): Int {
        val shared = payload ?: return 0
        val recipients = _state.value.selected
        if (recipients.isEmpty()) return 0
        val note = _state.value.note.trim()
        recipients.forEach { user -> sendTo(user, shared, note) }
        _state.update { it.copy(selected = emptyList(), note = "") }
        savedStateHandle[KEY_SELECTED] = ArrayList<String>()
        savedStateHandle[KEY_NOTE] = ""
        return recipients.size
    }

    private fun sendTo(user: ChatUser, shared: SharePayload, note: String) {
        val peer = user.username.trim()
        val name = user.displayName
        viewModelScope.launch {
            // Finish (and report) the send even if the user leaves the artwork screen right after tapping "Send"
            withContext(NonCancellable) {
                var artworkSent = false
                try {
                    val conversationId = chatRepository.ensureConversation(peer)
                    chatRepository.send(conversationId, shared.toOutgoing())
                    artworkSent = true
                    if (note.isNotEmpty()) {
                        chatRepository.send(conversationId, OutgoingMessage.Text(note))
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Couldn't share with $peer", e)
                    val message = if (artworkSent) {
                        "Sent to $name, but your message couldn't be sent"
                    } else {
                        "Couldn't send to $name. Check your connection and try again."
                    }
                    Toast.makeText(appContext, message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun loadRecent() {
        recentJob?.cancel()
        _state.update { it.copy(loadingRecent = it.recent.isEmpty(), recentError = null) }
        recentJob = viewModelScope.launch {
            chatRepository.observeConversations()
                .map { conversations ->
                    conversations.asSequence()
                        .map { it.peer.trim() }
                        .filter { it.isNotEmpty() && !it.equals(me, ignoreCase = true) }
                        .distinct()
                        .take(MAX_RECENT)
                        .toList()
                }
                .distinctUntilChanged()
                .catch { e ->
                    Log.w(TAG, "Couldn't load recent chats", e)
                    _state.update { it.copy(loadingRecent = false, recentError = friendlyError(e)) }
                }
                .collect { peers ->
                    _state.update { state ->
                        state.copy(
                            recent = peers.map { profiles[it] ?: ChatUser(username = it) },
                            loadingRecent = false,
                            recentError = null,
                        )
                    }
                    peers.forEach { resolveProfile(it) }
                }
        }
    }

    // Name + avatar + presence for a peer; the cell shows the username until it arrives
    private fun resolveProfile(username: String) {
        if (profiles.containsKey(username) || !resolving.add(username)) return
        viewModelScope.launch {
            val user = try {
                chatRepository.getUser(username)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Couldn't load the profile of $username", e)
                null
            } finally {
                resolving.remove(username)
            }
            if (user != null) {
                profiles[username] = user
                _state.update { state ->
                    state.copy(
                        recent = state.recent.map { if (it.username == username) user else it },
                        selected = state.selected.map { if (it.username == username) user else it },
                    )
                }
            }
        }
    }

    private fun search(value: String) {
        searchJob?.cancel()
        val term = value.trim()
        if (term.isEmpty()) {
            _state.update { it.copy(results = emptyList(), searching = false, searchError = null) }
            return
        }
        searchJob = viewModelScope.launch {
            _state.update { it.copy(searching = true, searchError = null) }
            delay(SEARCH_DEBOUNCE_MS)
            val result: Result<List<ChatUser>> = try {
                chatRepository.searchUsers(term)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
            ensureActive()
            result.fold(
                onSuccess = { found ->
                    val people = found
                        .filter { it.username.isNotBlank() && !it.username.equals(me, ignoreCase = true) }
                        .distinctBy { it.username }
                    _state.update { it.copy(results = people, searching = false, searchError = null) }
                },
                onFailure = { e ->
                    Log.w(TAG, "People search failed", e)
                    _state.update {
                        it.copy(results = emptyList(), searching = false, searchError = "Couldn't search right now")
                    }
                }
            )
        }
    }

    // Repository errors are either user-facing (chat sign-in) or technical (Firestore); only show the first kind
    private fun friendlyError(e: Throwable): String {
        val message = e.message?.trim().orEmpty()
        val technical = message.isEmpty() || message.length > 140 ||
            message.contains("PERMISSION_DENIED") || message.contains("Exception") ||
            message.startsWith("error code")
        return if (technical) "Couldn't load your chats. Check your connection and try again." else message
    }

    private companion object {
        const val TAG = "ShareToChat"
        const val KEY_SESSION = "share_session"
        const val KEY_QUERY = "share_query"
        const val KEY_NOTE = "share_note"
        const val KEY_SELECTED = "share_selected"
        const val MAX_RECENT = 30
        const val MAX_SELECTED = 15
        const val MAX_NOTE_LENGTH = 4000
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
