package com.orion.templete.presentation.reels

import android.content.Context
import android.speech.tts.Voice
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orion.templete.data.model.artwork_model.ArtworkDTO
import com.orion.templete.domain.paginator.DefaultPaginator
import com.orion.templete.domain.repository.ArtworkRepository
import com.orion.templete.domain.tts.StoryTtsManager
import com.orion.templete.util.SecureStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class ReelsUiState(
    val isLoading: Boolean = false,
    val items: List<ArtworkDTO> = emptyList(),
    val error: String? = null,
    val endReached: Boolean = false,
    val page: Int = 0,
    val speakingArtworkId: String? = null,
    val audioPaused: Boolean = false,
    val language: ReelsLanguage = ReelsLanguage.ENGLISH_IN,
    val selectedVoiceName: String? = null,
    val availableVoices: List<Voice> = emptyList(),
    val lastSettledIndex: Int = 0
)

/**
 * Display + spoken language for the story. Only languages whose text the app
 * actually has are enabled; Hindi arrives with the backend translation work.
 */
enum class ReelsLanguage(val label: String, val locale: Locale, val enabled: Boolean) {
    ENGLISH_IN("English (India)", Locale("en", "IN"), true),
    HINDI("हिन्दी (soon)", Locale("hi", "IN"), false)
}

@HiltViewModel
class ReelsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val repository: ArtworkRepository,
    secureStorage: SecureStorage
) : ViewModel() {

    private val userId = secureStorage.getUserId() ?: ""
    private val tts = StoryTtsManager(context)

    var state by mutableStateOf(ReelsUiState())
        private set

    private var userPausedCurrent = false

    private val pagination = DefaultPaginator<Int, ArtworkDTO>(
        initialKey = 0,
        onLoadUpdated = { isLoading ->
            state = state.copy(isLoading = isLoading)
        },
        onRequest = { nextPage ->
            repository.paginationArtwork(userId = userId, nextPage, 10)
        },
        getNextKey = { state.page + 1 },
        onError = { throwable ->
            state = state.copy(error = throwable?.localizedMessage)
        },
        onSuccess = { items, newKey ->
            state = state.copy(
                items = state.items + items,
                page = newKey,
                endReached = items.isEmpty()
            )
        }
    )

    init {
        tts.onUtteranceDone = { utteranceId ->
            if (state.speakingArtworkId == utteranceId) {
                state = state.copy(speakingArtworkId = null)
            }
        }
        loadNextItems()
        refreshVoices()
    }

    fun loadNextItems() {
        viewModelScope.launch {
            pagination.loadNextItems()
        }
    }

    /** Called when the pager settles on a page: preload more near the end, then auto-play the story. */
    fun onPageSettled(index: Int) {
        if (index >= state.items.size - 3 && !state.endReached && !state.isLoading) {
            loadNextItems()
        }
        userPausedCurrent = false
        state = state.copy(audioPaused = false, lastSettledIndex = index)
        speakArtworkAt(index)
    }

    fun toggleAudio() {
        if (state.speakingArtworkId != null) {
            tts.stop()
            userPausedCurrent = true
            state = state.copy(speakingArtworkId = null, audioPaused = true)
        } else {
            userPausedCurrent = false
            state = state.copy(audioPaused = false)
            val index = state.lastSettledIndex
            speakArtworkAt(index)
        }
    }

    fun onLanguageSelected(language: ReelsLanguage) {
        if (!language.enabled) return
        state = state.copy(language = language, selectedVoiceName = null)
        refreshVoices()
        // Re-read the current story in the new language/voice if audio was on.
        if (state.speakingArtworkId != null) speakArtworkAt(state.lastSettledIndex)
    }

    fun onVoiceSelected(voiceName: String?) {
        state = state.copy(selectedVoiceName = voiceName)
        if (state.speakingArtworkId != null) speakArtworkAt(state.lastSettledIndex)
    }

    fun stopAudio() {
        tts.stop()
        state = state.copy(speakingArtworkId = null)
    }

    private fun speakArtworkAt(index: Int) {
        if (userPausedCurrent) return
        val artwork = state.items.getOrNull(index) ?: return
        val story = artwork.description?.takeIf { it.isNotBlank() } ?: return
        val artworkId = artwork.id ?: return
        val voice = state.availableVoices.firstOrNull { it.name == state.selectedVoiceName }
        state = state.copy(speakingArtworkId = artworkId)
        tts.speak(artworkId, story, state.language.locale, voice)
    }

    private fun refreshVoices() {
        // The engine may still be initialising; retry briefly until voices are available.
        viewModelScope.launch {
            repeat(10) { attempt ->
                val voices = tts.deviceVoicesFor(state.language.locale)
                if (voices.isNotEmpty() || attempt == 9) {
                    state = state.copy(availableVoices = voices)
                    return@launch
                }
                kotlinx.coroutines.delay(500)
            }
        }
    }

    override fun onCleared() {
        tts.shutdown()
        super.onCleared()
    }
}


