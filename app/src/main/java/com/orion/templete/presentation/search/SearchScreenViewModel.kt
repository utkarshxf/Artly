package com.orion.templete.presentation.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orion.templete.data.model.artist_model.SearchArtistResponse
import com.orion.templete.data.model.artist_model.TopArtistProjection
import com.orion.templete.data.model.artwork_model.ArtworkDTO
import com.orion.templete.data.model.user_model.TopUserProjection
import com.orion.templete.data.model.user_model.TopCreatorProjection
import com.orion.templete.domain.repository.ArtistRepository
import com.orion.templete.domain.repository.UserRepository
import com.orion.templete.util.ResponseStates
import com.orion.templete.util.SecureStorage
import com.orion.templete.util.TrackEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import com.orion.templete.data.model.search.SearchArtistHit
import com.orion.templete.data.model.search.SearchArtworkHit
import com.orion.templete.data.model.search.SearchPersonHit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchScreenViewModel @Inject constructor(
    private val artistRepository: ArtistRepository,
    private val userRepository: UserRepository,
    private val secureStorage: SecureStorage,
    private val trackEvents: TrackEvents
) : ViewModel() {

    // Flow to hold search results
    private val _searchResults = MutableStateFlow<List<SearchArtistResponse>>(emptyList())
    val searchResults: StateFlow<List<SearchArtistResponse>> = _searchResults

    private val _popularArtworks = MutableStateFlow<PopularArtworksUiState>(PopularArtworksUiState.Loading)
    val popularArtworks: StateFlow<PopularArtworksUiState> = _popularArtworks

    private val _newArtworks = MutableStateFlow<NewArtworksUiState>(NewArtworksUiState.Loading)
    val newArtworks: StateFlow<NewArtworksUiState> = _newArtworks

    private val _recommendedForToday = MutableStateFlow<RecommendedForTodayUiState>(RecommendedForTodayUiState.Loading)
    val recommendedForToday: StateFlow<RecommendedForTodayUiState> = _recommendedForToday

    private val _todayBiggestHit = MutableStateFlow<TodayBiggestHitUiState>(TodayBiggestHitUiState.Loading)
    val todayBiggestHit: StateFlow<TodayBiggestHitUiState> = _todayBiggestHit

    private val _topUsers = MutableStateFlow<TopUsersUiState>(TopUsersUiState.Loading)
    val topUsers: StateFlow<TopUsersUiState> = _topUsers

    private val _topArtists = MutableStateFlow<TopArtistsUiState>(TopArtistsUiState.Loading)
    val topArtists: StateFlow<TopArtistsUiState> = _topArtists

    private val _topCreators = MutableStateFlow<TopCreatorsUiState>(TopCreatorsUiState.Loading)
    val topCreators: StateFlow<TopCreatorsUiState> = _topCreators

    init {
        val currentUserId = secureStorage.getUserId()
        currentUserId?.let {
            getPopularArtworks(it)
            getNewArtworks(it)
            getRecommendedForToday(it)
            getTodayBiggestHit()
            getTopUsers()
            getTopArtists()
            getTopCreators()
        }
    }

    // Function to search for artists
    fun searchArtist(query: String) {
        viewModelScope.launch {
            artistRepository.searchArtist(query)
                .catch { e ->
                    _searchResults.value = emptyList()
                }
                .collect { result ->
                    Log.e("query", result.toString())
                    _searchResults.value = result // Emit the results
                }
        }
    }

    fun getPopularArtworks(userId: String) {
        viewModelScope.launch {
            artistRepository.getPopularArtworks(userId)
                .catch { e ->
                    _popularArtworks.value = PopularArtworksUiState.Error(e.message ?: "Unknown error")
                }
                .collect { result ->
                    _popularArtworks.value = PopularArtworksUiState.Success(result)
                }
        }
    }

    fun getNewArtworks(userId: String) {
        viewModelScope.launch {
            artistRepository.getNewArtworks(userId)
                .catch { e ->
                    _newArtworks.value = NewArtworksUiState.Error(e.message ?: "Unknown error")
                }.collect { result ->
                    _newArtworks.value = NewArtworksUiState.Success(result)
                }
        }
    }

    fun getRecommendedForToday(userId: String) {
        viewModelScope.launch {
            artistRepository.getRecommendedForToday(userId)
                .catch { e ->
                    _recommendedForToday.value =
                        RecommendedForTodayUiState.Error(e.message ?: "Unknown error")
                }.collect { result ->
                    _recommendedForToday.value = RecommendedForTodayUiState.Success(result)
                }
        }
    }

    fun getTodayBiggestHit() {
        viewModelScope.launch {
            artistRepository.getTodayBiggestHit()
                .catch { e ->
                    _todayBiggestHit.value =
                        TodayBiggestHitUiState.Error(e.message ?: "Unknown error")
                }.collect { result ->
                    _todayBiggestHit.value = TodayBiggestHitUiState.Success(result)
                }
        }
    }

    fun getTopUsers() {
        viewModelScope.launch {
            userRepository.getTopViewers()
                .collect { result ->
                    when (result) {
                        is ResponseStates.Loading -> _topUsers.value = TopUsersUiState.Loading
                        is ResponseStates.Success -> _topUsers.value = TopUsersUiState.Success(result.data)
                        is ResponseStates.Error -> _topUsers.value = TopUsersUiState.Error(result.error)
                    }
                }
        }
    }

    fun getTopArtists() {
        viewModelScope.launch {
            artistRepository.getTopArtists()
                .collect { result ->
                    when (result) {
                        is ResponseStates.Loading -> _topArtists.value = TopArtistsUiState.Loading
                        is ResponseStates.Success -> _topArtists.value = TopArtistsUiState.Success(result.data)
                        is ResponseStates.Error -> _topArtists.value = TopArtistsUiState.Error(result.error)
                    }
                }
        }
    }

    fun getTopCreators() {
        viewModelScope.launch {
            userRepository.getTopCreators()
                .collect { result ->
                    when (result) {
                        is ResponseStates.Loading -> _topCreators.value = TopCreatorsUiState.Loading
                        is ResponseStates.Success -> _topCreators.value = TopCreatorsUiState.Success(result.data)
                        is ResponseStates.Error -> _topCreators.value = TopCreatorsUiState.Error(result.error)
                    }
                }
        }
    }

    fun artistSearched(name: String?) {
        if (name != null) {
            trackEvents.trackArtistSearched(name)
        }
    }

    // ------------------------------------------------------------------ search (artworks / artists / people)

    private val _search = MutableStateFlow(SearchUiState())
    val search: StateFlow<SearchUiState> = _search
    private var debounceJob: Job? = null
    private val loadJobs = HashMap<SearchTab, Job>()

    // Results follow the text as it is typed (after a short pause)
    fun onSearchQueryChange(value: String) {
        _search.update { it.copy(query = value) }
        debounceJob?.cancel()
        loadJobs.values.forEach { it.cancel() }
        loadJobs.clear()
        val term = value.trim()
        if (term.isEmpty()) {
            _search.update { it.copy(results = emptyMap()) }
            return
        }
        debounceJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            // other tabs load again when opened
            _search.update { state -> state.copy(results = state.results.filterKeys { it == state.tab }) }
            load(_search.value.tab, term, reset = true)
        }
    }

    fun onSearchTabSelected(tab: SearchTab) {
        _search.update { it.copy(tab = tab) }
        val term = _search.value.query.trim()
        val loaded = _search.value.results[tab]
        if (term.isNotEmpty() && debounceJob?.isActive != true && (loaded == null || loaded.query != term)) {
            load(tab, term, reset = true)
        }
    }

    fun retrySearch() {
        val state = _search.value
        val term = state.query.trim()
        if (term.isNotEmpty()) load(state.tab, term, reset = true)
    }

    // Next page of the open list tab (the "Top" tab is a single page)
    fun loadMoreSearchResults() {
        val state = _search.value
        val tab = state.tab
        val current = state.results[tab] ?: return
        if (tab == SearchTab.TOP || current.loading || current.loadingMore || current.endReached || current.error != null) return
        load(tab, current.query, reset = false)
    }

    private fun load(tab: SearchTab, term: String, reset: Boolean) {
        loadJobs[tab]?.cancel()
        val previous = _search.value.results[tab]?.takeIf { it.query == term }
        val skip = if (reset || previous == null) 0 else when (tab) {
            SearchTab.ARTWORKS -> previous.artworks.size
            SearchTab.ARTISTS -> previous.artists.size
            SearchTab.PEOPLE -> previous.people.size
            SearchTab.TOP -> 0
        }
        updateTab(tab) { old ->
            // old results stay on screen (with a progress bar) until the new ones arrive
            if (reset) (old ?: SearchTabResults()).copy(query = term, loading = true, error = null)
            else (old ?: SearchTabResults(query = term)).copy(loadingMore = true)
        }
        loadJobs[tab] = viewModelScope.launch {
            try {
                val response = artistRepository.search(term, tab.type, skip, SEARCH_PAGE_SIZE)
                val artworks = response.artworks.orEmpty().filter { !it.id.isNullOrBlank() }
                val artists = response.artists.orEmpty().filter { !it.id.isNullOrBlank() }
                val people = response.people.orEmpty().filter { !it.username.isNullOrBlank() }
                val fetched = when (tab) {
                    SearchTab.ARTWORKS -> artworks.size
                    SearchTab.ARTISTS -> artists.size
                    SearchTab.PEOPLE -> people.size
                    SearchTab.TOP -> 0
                }
                updateTab(tab) { old ->
                    val base = if (reset || old == null || old.query != term) SearchTabResults(query = term) else old
                    base.copy(
                        artworks = (base.artworks + artworks).distinctBy { it.id },
                        artists = (base.artists + artists).distinctBy { it.id },
                        people = (base.people + people).distinctBy { it.username },
                        loading = false,
                        loadingMore = false,
                        error = null,
                        endReached = tab == SearchTab.TOP || fetched < SEARCH_PAGE_SIZE,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("SearchScreenViewModel", "Search failed", e)
                updateTab(tab) { old ->
                    (old ?: SearchTabResults(query = term)).copy(
                        loading = false,
                        loadingMore = false,
                        error = "Couldn't search right now. Check your connection and try again."
                    )
                }
            }
        }
    }

    private fun updateTab(tab: SearchTab, change: (SearchTabResults?) -> SearchTabResults) {
        _search.update { state -> state.copy(results = state.results + (tab to change(state.results[tab]))) }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
        const val SEARCH_PAGE_SIZE = 20
    }
}

enum class SearchTab(val label: String, val type: String) {
    TOP("Top", "all"),
    ARTWORKS("Artworks", "artworks"),
    ARTISTS("Artists", "artists"),
    PEOPLE("People", "people"),
}

data class SearchTabResults(
    val query: String = "",
    val artworks: List<SearchArtworkHit> = emptyList(),
    val artists: List<SearchArtistHit> = emptyList(),
    val people: List<SearchPersonHit> = emptyList(),
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val endReached: Boolean = false,
    val error: String? = null,
) {
    val isEmpty: Boolean get() = artworks.isEmpty() && artists.isEmpty() && people.isEmpty()
}

data class SearchUiState(
    val query: String = "",
    val tab: SearchTab = SearchTab.TOP,
    val results: Map<SearchTab, SearchTabResults> = emptyMap(),
) {
    val current: SearchTabResults get() = results[tab] ?: SearchTabResults()
}

sealed interface PopularArtworksUiState {
    object Loading : PopularArtworksUiState
    data class Success(val artworks: List<ArtworkDTO>) : PopularArtworksUiState
    data class Error(val message: String) : PopularArtworksUiState
}

sealed interface NewArtworksUiState {
    object Loading : NewArtworksUiState
    data class Success(val listArtwork: List<ArtworkDTO>) : NewArtworksUiState
    data class Error(val message: String) : NewArtworksUiState
}

sealed interface RecommendedForTodayUiState {
    object Loading : RecommendedForTodayUiState
    data class Success(val listArtwork: List<ArtworkDTO>) : RecommendedForTodayUiState
    data class Error(val message: String) : RecommendedForTodayUiState
}

sealed interface TodayBiggestHitUiState {
    object Loading : TodayBiggestHitUiState
    data class Success(val artwork: ArtworkDTO) : TodayBiggestHitUiState
    data class Error(val message: String) : TodayBiggestHitUiState
}

sealed interface TopUsersUiState {
    object Loading : TopUsersUiState
    data class Success(val users: List<TopUserProjection>) : TopUsersUiState
    data class Error(val message: String) : TopUsersUiState
}

sealed interface TopArtistsUiState {
    object Loading : TopArtistsUiState
    data class Success(val artists: List<TopArtistProjection>) : TopArtistsUiState
    data class Error(val message: String) : TopArtistsUiState
}

sealed interface TopCreatorsUiState {
    object Loading : TopCreatorsUiState
    data class Success(val creators: List<TopCreatorProjection>) : TopCreatorsUiState
    data class Error(val message: String) : TopCreatorsUiState
}

