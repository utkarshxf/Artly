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
import com.orion.templete.data.model.favorits.favoritesDTO
import com.orion.templete.domain.paginator.DefaultPaginator
import com.orion.templete.domain.repository.ArtworkRepository
import com.orion.templete.domain.tts.StoryPrefetcher
import com.orion.templete.domain.tts.StoryTtsManager
import com.orion.templete.util.ResponseStates
import com.orion.templete.util.SecureStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
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
    val lastSettledIndex: Int = 0,
    val toastMessage: String? = null
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
    private val storyPrefetcher: StoryPrefetcher,
    secureStorage: SecureStorage
) : ViewModel() {

    private val userId = secureStorage.getUserId() ?: "guest"
    private val tts = StoryTtsManager(context)

    var state by mutableStateOf(ReelsUiState())
        private set

    // "feed" (default) pages the main feed; "similar" is the reel of one artwork followed by its
    // similar artworks; "artist" is one artist's artworks. Set once from the hosting screen.
    private var sourceMode: String = "feed"
    private var sourceRefId: String? = null
    private var sourceSet = false

    private var userPausedCurrent = false
    private var mediaPlayer: MediaPlayer? = null

    // Incremented on every stop/settle; an in-flight fetch from an older page checks it before
    // touching state or starting audio, so a previous artwork's narration can never overlap.
    private var playbackSession = 0

    // Device country, sent with story requests: the backend serves Gemini narration only to
    // regions the free-tier terms allow; everywhere else falls back to on-device TTS.
    private val deviceRegion: String = Locale.getDefault().country ?: ""

    private val pagination = DefaultPaginator<Int, ArtworkDTO>(
        initialKey = 0,
        onLoadUpdated = { isLoading ->
            state = state.copy(isLoading = isLoading)
        },
        onRequest = { nextPage ->
            when {
                sourceMode == "similar" && sourceRefId != null ->
                    if (nextPage == 0) similarFirstPage(sourceRefId!!) else emptyList()
                sourceMode == "artist" && sourceRefId != null ->
                    if (nextPage == 0) artistFirstPage(sourceRefId!!) else emptyList()
                else -> repository.paginationArtwork(userId = userId, nextPage, 10)
            }
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
        refreshVoices()
    }

    fun setSource(mode: String?, refId: String?) {
        if (sourceSet) return
        sourceSet = true
        sourceMode = mode ?: "feed"
        sourceRefId = refId
        loadNextItems()
    }

    private suspend fun similarFirstPage(artworkId: String): List<ArtworkDTO> {
        val current = repository.getArtworkById(userId, artworkId)
            .first { it is ResponseStates.Success }
            .let { (it as ResponseStates.Success).data }
        val similar = repository.getSimilarArtwork(userId, artworkId)
            .first { it is ResponseStates.Success }
            .let { (it as ResponseStates.Success).data }
        return listOf(current) + similar.filter { it.id != current.id }
    }

    private suspend fun artistFirstPage(artistId: String): List<ArtworkDTO> =
        repository.getArtworkByArtistId(userId, artistId, "")
            .first { it is ResponseStates.Success }
            .let { (it as ResponseStates.Success).data }

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
        // Stop the previous artwork's audio the moment the page settles, before any fetching.
        playbackSession++
        stopAllAudio()
        state = state.copy(audioPaused = false, lastSettledIndex = index, speakingArtworkId = null)
        speakArtworkAt(index)
        // Generate + download the next stories now, so their audio starts instantly on landing.
        prefetchAround(index)
    }

    fun toggleAudio() {
        if (state.speakingArtworkId != null) {
            playbackSession++
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
        playbackSession++
        stopAllAudio()
        refreshVoices()
        prefetchAround(state.lastSettledIndex)
        // Re-read the current story in the new language/voice if audio was on.
        if (state.speakingArtworkId != null) speakArtworkAt(state.lastSettledIndex)
    }

    fun onVoiceSelected(voiceName: String?) {
        state = state.copy(selectedVoiceName = voiceName)
        if (state.speakingArtworkId != null) {
            playbackSession++
            stopAllAudio()
            speakArtworkAt(state.lastSettledIndex)
        }
    }

    fun stopAudio() {
        playbackSession++
        stopAllAudio()
        state = state.copy(speakingArtworkId = null)
    }

    fun toggleLike(artworkId: String) {
        val index = state.items.indexOfFirst { it.id == artworkId }
        if (index < 0) return
        val artwork = state.items[index]
        val nowLiked = artwork.liked != true
        // Apply locally right away; roll back if the call fails.
        state = state.copy(items = state.items.toMutableList().also {
            it[index] = artwork.copy(liked = nowLiked)
        })
        viewModelScope.launch {
            val ok = try {
                if (nowLiked) repository.likeArtwork(artworkId, userId)
                else repository.disLikeArtwork(artworkId, userId)
            } catch (e: Exception) {
                false
            }
            if (!ok) {
                state = state.copy(items = state.items.toMutableList().also {
                    it[index] = artwork
                }, toastMessage = "Couldn't update like")
            }
        }
    }

    fun saveArtwork(artworkId: String) {
        viewModelScope.launch {
            try {
                repository.getAllFavorites(userId).collect { favoritesState ->
                    if (favoritesState is ResponseStates.Success) {
                        val collections = favoritesState.data
                        if (collections.isEmpty()) {
                            createCollectionAndSave(artworkId)
                        } else {
                            val id = collections.first().id
                            if (id != null) saveToCollection(id, artworkId)
                        }
                        return@collect
                    }
                }
            } catch (e: Exception) {
                state = state.copy(toastMessage = "Couldn't save")
            }
        }
    }

    private suspend fun createCollectionAndSave(artworkId: String) {
        repository.createNewFavorites(
            userId,
            favoritesDTO(description = "", title = "Favorites")
        ).collect { created ->
            if (created is ResponseStates.Success) {
                val id = created.data.id
                if (id != null) saveToCollection(id, artworkId)
            }
        }
    }

    private suspend fun saveToCollection(favoritesId: String, artworkId: String) {
        repository.saveOnFavorites(favoritesId, artworkId).collect { result ->
            when (result) {
                is ResponseStates.Success ->
                    state = state.copy(toastMessage = "Saved to Favorites")
                is ResponseStates.Error ->
                    state = state.copy(toastMessage = "Couldn't save")
                else -> {}
            }
        }
    }

    fun clearToast() {
        if (state.toastMessage != null) state = state.copy(toastMessage = null)
    }

    private fun speakArtworkAt(index: Int) {
        if (userPausedCurrent) return
        val artwork = state.items.getOrNull(index) ?: return
        val artworkId = artwork.id ?: return
        val lang = state.language
        val session = ++playbackSession
        viewModelScope.launch {
            // 1. Story text: backend translation (per artwork + language, cached), else the app's own text.
            val text = storyPrefetcher.storyText(artworkId, lang.apiCode, deviceRegion)
                ?: artwork.description?.takeIf { it.isNotBlank() } ?: return@launch

            // The user swiped away, paused or left while the text was loading.
            if (session != playbackSession || userPausedCurrent || state.lastSettledIndex != index) return@launch

            stopAllAudio()
            state = state.copy(speakingArtworkId = artworkId)

            // 2. Narration: backend audio first, device TTS when that fails (503 quota or offline).
            if (!playBackendAudio(artworkId, lang.apiCode, session) &&
                session == playbackSession && state.speakingArtworkId == artworkId
            ) {
                val voice = state.availableVoices.firstOrNull { it.name == state.selectedVoiceName }
                tts.speak(artworkId, text, lang.locale, voice)
            }
        }
    }

    /** Plays the narrated WAV, using the prefetched cache when present. False = caller should use device TTS. */
    private suspend fun playBackendAudio(artworkId: String, lang: String, session: Int): Boolean {
        return try {
            if (!storyPrefetcher.downloadAudio(artworkId, lang, deviceRegion)) return false
            val file = storyPrefetcher.audioFile(artworkId, lang)
            if (session != playbackSession || state.speakingArtworkId != artworkId) return true // moved on mid-download
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
            if (session != playbackSession || state.speakingArtworkId != artworkId) {
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

    /**
     * Generates and downloads story text + audio for the artworks just ahead, so narration
     * starts the moment the user lands on them.
     */
    private fun prefetchAround(index: Int) {
        val lang = state.language.apiCode
        for (i in (index + 1)..(index + 3)) {
            val id = state.items.getOrNull(i)?.id ?: continue
            viewModelScope.launch {
                storyPrefetcher.warmupStory(id, lang, deviceRegion)
            }
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
