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
    val language: ReelsLanguage = ReelsLanguage.forDevice(),
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
    HINDI("हिन्दी", Locale("hi", "IN"), "hi", true),
    TURKISH("Türkçe", Locale("tr", "TR"), "tr", true),
    PERSIAN("فارسی", Locale("fa", "IR"), "fa", true),
    ARABIC("العربية", Locale("ar", "EG"), "ar", true),
    INDONESIAN("Bahasa Indonesia", Locale("in", "ID"), "id", true),
    POLISH("Polski", Locale("pl", "PL"), "pl", true),
    HUNGARIAN("Magyar", Locale("hu", "HU"), "hu", true),
    GREEK("Ελληνικά", Locale("el", "GR"), "el", true);

    companion object {
        // Default story language: the device language when we support it, otherwise English.
        // (The audience country data is name-inferred and unverified, so it cannot set a default.)
        fun forDevice(): ReelsLanguage {
            val deviceLanguage = Locale.getDefault().language
            return entries.firstOrNull { it.apiCode == deviceLanguage || it.locale.language == deviceLanguage }
                ?: ENGLISH_IN
        }
    }
}

@HiltViewModel
class ReelsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val repository: ArtworkRepository,
    private val storyPrefetcher: StoryPrefetcher,
    secureStorage: SecureStorage
) : ViewModel() {

    private val userId = secureStorage.getUserId() ?: "guest"
    private val isGuest = secureStorage.getToken().isNullOrBlank()
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

    // Players prepared ahead of the swipe, Instagram-style: while reel N plays, N+1/N+2 are
    // decoded and ready, so landing on them is just start() - milliseconds, no prepare wait.
    private val preparedPlayers = java.util.concurrent.ConcurrentHashMap<String, MediaPlayer>()
    private val preparingPlayers = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

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
        // Mark the settled artwork as seen so the feed stops repeating it (view API is authed)
        if (!isGuest) {
            state.items.getOrNull(index)?.id?.let { viewedId ->
                viewModelScope.launch { repository.viewArtwork(viewedId, userId) }
            }
        }
        userPausedCurrent = false
        // Stop the previous artwork's audio the moment the page settles, before any fetching.
        // speakArtworkAt stops everything except this page's prepared player.
        playbackSession++
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
        val key = "$artworkId|${lang.apiCode}"

        stopAllAudio(keepPreparedKey = key)
        state = state.copy(speakingArtworkId = artworkId)

        // Instant path: this reel's player was prepared while the previous one played.
        preparedPlayers.remove(key)?.let { prepared ->
            preparingPlayers.remove(key)
            prepared.setOnCompletionListener {
                if (state.speakingArtworkId == artworkId) {
                    state = state.copy(speakingArtworkId = null)
                }
            }
            prepared.setOnErrorListener { _, _, _ ->
                if (state.speakingArtworkId == artworkId) {
                    state = state.copy(speakingArtworkId = null)
                }
                true
            }
            mediaPlayer = prepared
            prepared.start()
            return
        }

        viewModelScope.launch {
            // Cold path: audio download first (it does not need the text); the story text is
            // only fetched for the device-TTS fallback, so the common path saves a round trip.
            if (playBackendAudio(artworkId, lang.apiCode, session)) return@launch
            if (session != playbackSession || userPausedCurrent || state.lastSettledIndex != index) return@launch

            val text = storyPrefetcher.storyText(artworkId, lang.apiCode, deviceRegion)
                ?: artwork.description?.takeIf { it.isNotBlank() } ?: return@launch
            if (session != playbackSession || state.speakingArtworkId != artworkId) return@launch
            val voice = state.availableVoices.firstOrNull { it.name == state.selectedVoiceName }
            tts.speak(artworkId, text, lang.locale, voice)
        }
    }

    /** Creates prepared players for the reels at/just after [index] whose audio is already cached. */
    private fun preparePlayersAhead(index: Int) {
        val lang = state.language.apiCode
        for (i in index..(index + 2)) {
            val id = state.items.getOrNull(i)?.id ?: continue
            val key = "$id|$lang"
            if (preparedPlayers.containsKey(key) || !preparingPlayers.add(key)) continue
            val file = storyPrefetcher.audioFile(id, lang)
            if (!file.exists() || file.length() == 0L) {
                preparingPlayers.remove(key)
                continue
            }
            try {
                val player = MediaPlayer()
                player.setDataSource(file.absolutePath)
                player.setOnPreparedListener { ready ->
                    preparedPlayers[key] = ready
                    preparingPlayers.remove(key)
                }
                player.setOnErrorListener { _, _, _ ->
                    preparedPlayers.remove(key)
                    preparingPlayers.remove(key)
                    true
                }
                player.prepareAsync()
            } catch (e: Exception) {
                preparingPlayers.remove(key)
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
                // The file may have just landed: get its player ready for the swipe onto it
                preparePlayersAhead(state.lastSettledIndex)
            }
        }
        preparePlayersAhead(index)
    }

    private fun stopAllAudio(keepPreparedKey: String? = null) {
        tts.stop()
        mediaPlayer?.let {
            try {
                it.stop()
            } catch (_: Exception) {
            }
            it.release()
        }
        mediaPlayer = null
        val it = preparedPlayers.entries.iterator()
        while (it.hasNext()) {
            val entry = it.next()
            if (entry.key != keepPreparedKey) {
                try {
                    entry.value.release()
                } catch (_: Exception) {
                }
                it.remove()
            }
        }
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
