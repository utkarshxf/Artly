package com.orion.templete.presentation.home

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orion.templete.domain.tts.StoryPrefetcher
import com.orion.templete.domain.repository.ArtworkRepository
import com.orion.templete.domain.repository.UserRepository
import com.orion.templete.domain.repository.chat.ChatSession
import com.orion.templete.util.ResponseStates
import com.orion.templete.util.SecureStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val secureStorage: SecureStorage,
    private val chatSession: ChatSession,
    private val artworkRepository: ArtworkRepository,
    private val storyPrefetcher: StoryPrefetcher
) : ViewModel() {
    var isArtistState by mutableStateOf<IsArtistUiState>(IsArtistUiState.Loading)
        private set
    val currentUserId = secureStorage.getUserId()

    val IsArtist =  secureStorage.userIsAnArtist()

    init {
        currentUserId?.let {
            checkIfUserIsArtist(currentUserId)
            refreshUserDetails(currentUserId)
        }
        startChatSession()
        warmupFirstBatch()
    }

    // Preload the first feed batch (images + story text/audio) at launch so the reels tab opens with no loading.
    private fun warmupFirstBatch() {
        val userId = currentUserId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val region = Locale.getDefault().country ?: ""
                val first = artworkRepository.paginationArtwork(userId, 0, 10)
                storyPrefetcher.enqueueImages(first, 5)
                first.take(3).forEach { storyPrefetcher.warmupStory(it.id ?: return@forEach, "en", region) }
            } catch (e: Exception) {
                Log.w("HomeViewModel", "Warmup failed", e)
            }
        }
    }

    // Home is shown right after login and on every cold start while logged in: sign in to chat (Firebase custom
    // token), publish the profile, register the push token and start presence. Idempotent and never fatal.
    private fun startChatSession() {
        val loggedIn = !currentUserId.isNullOrBlank() && !secureStorage.getToken().isNullOrBlank()
        if (!loggedIn) return
        try {
            chatSession.start()
        } catch (e: Exception) {
            Log.w("HomeViewModel", "Couldn't start the chat session", e)
        }
    }

    // Keep the cached profile (name, picture, country...) that chat, upload and artist signup read up to date
    private fun refreshUserDetails(userId: String) {
        viewModelScope.launch {
            userRepository.getUserByUserId(userId).collect { response ->
                if (response is ResponseStates.Success) secureStorage.saveUserDto(response.data)
            }
        }
    }

    private fun checkIfUserIsArtist(userId: String) {
        viewModelScope.launch {
            userRepository.isUserArtist(userId).collect { response ->
                    when (response) {
                        is ResponseStates.Loading -> IsArtistUiState.Loading
                        is ResponseStates.Success -> {
                            secureStorage.setUserIsAnArtist(response.data)
                            IsArtistUiState.Success(response.data)
                        }
                        is ResponseStates.Error -> IsArtistUiState.Error(response.error)
                    }
                }
        }
    }

}

sealed interface IsArtistUiState {
    object Loading : IsArtistUiState
    data class Success(val isArtist: Boolean) : IsArtistUiState
    data class Error(val message: String) : IsArtistUiState
}
