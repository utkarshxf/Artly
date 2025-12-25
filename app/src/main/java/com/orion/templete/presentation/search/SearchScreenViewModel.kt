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
import kotlinx.coroutines.flow.MutableStateFlow
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

