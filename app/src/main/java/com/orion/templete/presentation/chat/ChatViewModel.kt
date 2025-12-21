package com.orion.templete.presentation.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orion.templete.data.model.chat.Message
import com.orion.templete.domain.repository.chat.ChatRepository
import com.orion.templete.util.SecureStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val currentUsername: String = "",
    val peerUsername: String = "",
    val conversationId: String? = null,
    val messages: List<Message> = emptyList(),
    val input: String = "",
    val sending: Boolean = false,
    val error: String? = null,
    val peerName: String? = null,
    val peerAvatar: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val secureStorage: SecureStorage,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state

    init {
        // Use user ID as the unique identifier for chat operations (name can be empty)
        val me = (secureStorage.getUserDetails()?.id ?: "").trim()
        val peer = (savedStateHandle.get<String>("peerUsername") ?: "").trim()
        _state.value = _state.value.copy(currentUsername = me, peerUsername = peer)

        // Only initialize chat if both user identifiers are valid
        if (me.isNotBlank() && peer.isNotBlank()) {
            viewModelScope.launch {
                val convId = repository.ensureConversation(me, peer)
                _state.value = _state.value.copy(conversationId = convId)
                observeMessages(convId)
                repository.markConversationRead(convId, me)
                // Fetch peer details for header (name/avatar)
                fetchPeerHeader(peer)
            }
        }
    }

    private fun observeMessages(conversationId: String) {
        viewModelScope.launch {
            repository.observeMessages(conversationId).collectLatest {
                _state.value = _state.value.copy(messages = it)
            }
        }
    }

    fun onInputChange(text: String) {
        _state.value = _state.value.copy(input = text)
    }

    fun send() {
        val conv = _state.value.conversationId ?: return
        val text = _state.value.input.trim()
        if (text.isBlank()) return
        val me = _state.value.currentUsername
        viewModelScope.launch {
            // Optimistically clear input immediately for snappier UX
            _state.value = _state.value.copy(sending = true, input = "")
            try {
                repository.sendTextMessage(conv, me, text)
                repository.markConversationRead(conv, me)
                _state.value = _state.value.copy(sending = false)
            } catch (t: Throwable) {
                // Restore the text on failure so user can retry
                _state.value = _state.value.copy(error = t.message, sending = false, input = text)
            }
        }
    }

    private suspend fun fetchPeerHeader(username: String) {
        if (username.isBlank()) return
        val res = repository.searchUsers(username, limit = 5)
        if (res.isSuccess) {
            val u = res.getOrNull()?.firstOrNull { it.username.equals(username, ignoreCase = true) }
                ?: res.getOrNull()?.firstOrNull()
            _state.value = _state.value.copy(peerName = u?.name, peerAvatar = u?.profilePic)
        }
    }
}
