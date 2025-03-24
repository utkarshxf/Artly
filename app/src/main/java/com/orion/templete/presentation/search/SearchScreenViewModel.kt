package com.orion.templete.presentation.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orion.templete.data.model.artist_model.SearchArtistResponse
import com.orion.templete.data.model.artwork_model.ArtworkDTO
import com.orion.templete.data.model.favorits.favoritesDTO
import com.orion.templete.domain.repository.ArtistRepository
import com.orion.templete.util.SecureStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

@HiltViewModel
class SearchScreenViewModel @Inject constructor(
    private val artistRepository: ArtistRepository,
    private val secureStorage: SecureStorage
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
    init {
        val currentUserId = secureStorage.getUserDetails()?.id
        currentUserId?.let {
            getPopularArtworks(it)
            getNewArtworks(it)
            getRecommendedForToday(it)
            getTodayBiggestHit()
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
    fun getPopularArtworks(userId :String) {
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
    fun getNewArtworks(userId :String) {
        viewModelScope.launch {
            artistRepository.getNewArtworks(userId)
                .catch { e ->
                    _newArtworks.value = NewArtworksUiState.Error(e.message ?: "Unknown error")
                }.collect { result ->
                    _newArtworks.value = NewArtworksUiState.Success(result)
                }
        }
    }
    fun getRecommendedForToday(userId :String) {
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
    fun getTodayBiggestHit(){
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

