package com.orion.templete.presentation.reels

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.Voice
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orion.templete.data.model.artwork_model.ArtworkDTO
import com.orion.templete.data.network.ApiService
import com.orion.templete.domain.paginator.DefaultPaginator
import com.orion.templete.domain.repository.ArtworkRepository
import com.orion.templete.domain.tts.StoryTtsManager
import com.orion.templete.util.SecureStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import java.io.File
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
 * Display + spoken language for the story. [apiCode] is the backend's lang
 * parameter; the backend translates and narrates, the device TTS is the
 * fallback when the story service has no quota left (503).
 */
enum class ReelsLanguage(val label: String, val locale: Locale, val apiCode: String, val enabled: Boolean) {
    ENGLISH_IN("English (India)", Locale("en", "IN"), "en", true),
    HINDI("हिन्दी", Locale("hi", "IN"), "hi", true)
}

@HiltViewModel
class ReelsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val repository: ArtworkRepository,
    private val apiService: ApiService,
    secureStorage: SecureStorage
) : ViewModel() {

    private val appContext = context.applicationContext
    private val userId = secureStorage.getUserId() ?: ""
    private val tts = StoryTtsManager(context)

    var state by mutableStateOf(ReelsUiState())
        private set

    private var userPausedCurrent = false
    private var mediaPlayer: MediaPlayer? = null
    private val storyTextCache = mutableMapOf<String, String>()

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
            stopAllAudio()
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
        stopAllAudio()
        state = state.copy(speakingArtworkId = null)
    }

    private fun speakArtworkAt(index: Int) {
        if (userPausedCurrent) return
        val artwork = state.items.getOrNull(index) ?: return
        val artworkId = artwork.id ?: return
        val lang = state.language
        viewModelScope.launch {
            // 1. Story text: backend translation (per artwork + language, cached), else the app's own text.
            val cacheKey = "$artworkId:${lang.apiCode}"
            val text = storyTextCache[cacheKey] ?: run {
                val fetched = try {
                    val response = apiService.getStoryText(artworkId, lang.apiCode)
                    if (response.isSuccessful) response.body()?.text else null
                } catch (e: Exception) {
                    Log.w(TAG, "story text fetch failed, using local description", e)
                    null
                }
                fetched?.takeIf { it.isNotBlank() }?.also { storyTextCache[cacheKey] = it }
            } ?: artwork.description?.takeIf { it.isNotBlank() } ?: return@launch

            // The user swiped away or paused while the text was loading.
            if (userPausedCurrent || state.lastSettledIndex != index) return@launch

            stopAllAudio()
            state = state.copy(speakingArtworkId = artworkId)

            // 2. Narration: backend audio first, device TTS when that fails (503 quota or offline).
            if (!playBackendAudio(artworkId, lang.apiCode) && state.speakingArtworkId == artworkId) {
                val voice = state.availableVoices.firstOrNull { it.name == state.selectedVoiceName }
                tts.speak(artworkId, text, lang.locale, voice)
            }
        }
    }

    /** Downloads the narrated WAV to the cache and plays it. False = caller should use device TTS. */
    private suspend fun playBackendAudio(artworkId: String, lang: String): Boolean {
        return try {
            val response = apiService.getStoryAudio(artworkId, lang)
            if (!response.isSuccessful) return false
            val bytes = response.body()?.bytes() ?: return false
            if (bytes.isEmpty()) return false
            val file = File(appContext.cacheDir, "story_${artworkId}_$lang.wav")
            file.writeBytes(bytes)
            if (state.speakingArtworkId != artworkId) return true // paused or swiped away mid-download
            val player = MediaPlayer()
            player.setDataSource(file.absolutePath)
            player.setOnCompletionListener {
                if (state.speakingArtworkId == artworkId) {
                    state = state.copy(speakingArtworkId = null)
                }
            }
            player.setOnErrorListener { _, _, _ ->
                if (state.speakingArtworkId == artworkId) {
                    state = state.copy(speakingArtworkId = null)
                }
                true
            }
            player.prepare()
            if (state.speakingArtworkId != artworkId) {
                player.release()
                return true
            }
            mediaPlayer = player
            player.start()
            true
        } catch (e: Exception) {
            Log.w(TAG, "backend story audio failed, falling back to device TTS", e)
            false
        }
    }

    private fun stopAllAudio() {
        tts.stop()
        mediaPlayer?.let {
            try {
                it.stop()
            } catch (_: Exception) {
            }
            it.release()
        }
        mediaPlayer = null
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
        stopAllAudio()
        tts.shutdown()
        super.onCleared()
    }

    private companion object {
        const val TAG = "ReelsViewModel"
    }
}
