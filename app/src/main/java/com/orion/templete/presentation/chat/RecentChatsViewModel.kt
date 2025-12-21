package com.orion.templete.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orion.templete.data.model.chat.ChatUser
import com.orion.templete.data.model.chat.Conversation
import com.orion.templete.domain.repository.chat.ChatRepository
import com.orion.templete.util.SecureStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecentChatsUiState(
    val currentUsername: String = "",
    val query: String = "",
    val recent: List<Conversation> = emptyList(),
    val results: List<ChatUser> = emptyList(),
    val peerInfo: Map<String, ChatUser> = emptyMap(), // username -> details
    val loading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class RecentChatsViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val secureStorage: SecureStorage
) : ViewModel() {
    private val _state = MutableStateFlow(RecentChatsUiState())
    val state: StateFlow<RecentChatsUiState> = _state

    private var searchJob: Job? = null
    private val fetchedPeers = mutableSetOf<String>()

    init {
        // Use user ID as the unique identifier (name can be empty)
        val username = (secureStorage.getUserDetails()?.id ?: "").trim()
        _state.value = _state.value.copy(currentUsername = username)
        if (username.isNotBlank()) {
            observeRecents(username)
        }
    }

    private fun observeRecents(username: String) {
        viewModelScope.launch {
            chatRepository.observeRecentConversations(username).collectLatest {
                _state.value = _state.value.copy(recent = it)
                // Fetch peer details for nicer UI (avatar/name)
                it.forEach { conv ->
                    val peer = conv.usernames.firstOrNull { u -> u != username }?.trim().orEmpty()
                    if (peer.isNotBlank() && !fetchedPeers.contains(peer)) {
                        fetchedPeers.add(peer)
                        launch { fetchPeer(peer) }
                    }
                }
            }
        }
    }

    private suspend fun fetchPeer(username: String) {
        val res = chatRepository.searchUsers(username, limit = 5)
        if (res.isSuccess) {
            val match = res.getOrNull()?.firstOrNull { it.username.equals(username, ignoreCase = true) }
                ?: res.getOrNull()?.firstOrNull()
            match?.let { user ->
                val newMap = _state.value.peerInfo.toMutableMap()
                newMap[username] = user
                _state.value = _state.value.copy(peerInfo = newMap)
            }
        }
    }

    fun onQueryChange(new: String) {
        _state.value = _state.value.copy(query = new)
        searchJob?.cancel()
        if (new.isBlank()) {
            _state.value = _state.value.copy(results = emptyList(), loading = false, error = null)
            return
        }
        searchJob = viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            delay(300)
            val r = chatRepository.searchUsers(new.trim())
            _state.value = if (r.isSuccess) {
                _state.value.copy(results = r.getOrDefault(emptyList()), loading = false)
            } else {
                _state.value.copy(error = r.exceptionOrNull()?.message, loading = false)
            }
        }
    }
}
