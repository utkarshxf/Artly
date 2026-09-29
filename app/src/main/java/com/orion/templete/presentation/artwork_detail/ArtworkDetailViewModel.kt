package com.orion.templete.presentation.artwork_detail

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orion.templete.data.model.artwork_model.ArtworkDTO
import com.orion.templete.data.model.artwork_model.ArtworkStatsResponse
import com.orion.templete.domain.repository.ArtworkRepository
import com.orion.templete.util.ResponseStates
import com.orion.templete.util.SecureStorage
import com.orion.templete.util.TrackEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtworkDetailViewModel @Inject constructor(
    private val artworkRepository: ArtworkRepository,
    private val secureStorage: SecureStorage,
    private val trackEvents: TrackEvents // Add TrackEvents dependency
) : ViewModel() {
    var artworkDetailScreenUiState by mutableStateOf<ArtworkDetailScreenUiState>(ArtworkDetailScreenUiState.Loading)
        private set

    var artworksScreenUiState by mutableStateOf<ArtworksScreenUiState>(ArtworksScreenUiState.Loading)
        private set

    var similarArtworks by mutableStateOf<ArtworksScreenUiState>(ArtworksScreenUiState.Loading)
        private set

    // MVVM: Artwork stats state
    private val _artworkStatsUiState = MutableStateFlow<ArtworkStatsUiState>(ArtworkStatsUiState.Loading)
    val artworkStatsUiState: StateFlow<ArtworkStatsUiState> = _artworkStatsUiState

    private val currentUserId = secureStorage.getUserId()

    fun getArtworkById(artworkId: String) {
        viewModelScope.launch {
            if (currentUserId != null) {
                artworkRepository.getArtworkById(currentUserId, artworkId).collect { response ->
                    artworkDetailScreenUiState = when (response) {
                        is ResponseStates.Loading -> ArtworkDetailScreenUiState.Loading
                        is ResponseStates.Success -> {
                            // Track when artwork is viewed
                            trackEvents.trackArtworkViewed(artworkId)
                            ArtworkDetailScreenUiState.Success(response.data)
                        }
                        is ResponseStates.Error -> ArtworkDetailScreenUiState.Error(response.error)
                    }
                }
            }
        }
    }

    fun getArtworkByArtistId(artistId: String, currentArtworkId: String) {
        viewModelScope.launch {
            if (currentUserId != null) {
                artworkRepository.getArtworkByArtistId(currentUserId, artistId, currentArtworkId).collect { response ->
                    artworksScreenUiState = when (response) {
                        is ResponseStates.Loading -> ArtworksScreenUiState.Loading
                        is ResponseStates.Success -> {
                            // Track artist profile view indirectly through artwork
                            trackEvents.trackArtistProfileViewed(artistId)
                            ArtworksScreenUiState.Success(response.data)
                        }
                        is ResponseStates.Error -> ArtworksScreenUiState.Error(response.error)
                    }
                }
            }
        }
    }

    fun getSimilarArtwork(artworkId: String) {
        viewModelScope.launch {
            if (currentUserId != null) {
                artworkRepository.getSimilarArtwork(currentUserId, artworkId).collect { response ->
                    similarArtworks = when (response) {
                        is ResponseStates.Loading -> ArtworksScreenUiState.Loading
                        is ResponseStates.Success -> {
                            // Track similar artwork recommendations
                            response.data.forEach { similarArtwork ->
                                similarArtwork.id?.let {
                                    trackEvents.trackSimilarArtworkViewed(artworkId,
                                        it
                                    )
                                }
                            }
                            ArtworksScreenUiState.Success(response.data)
                        }
                        is ResponseStates.Error -> ArtworksScreenUiState.Error(response.error)
                    }
                }
            }
        }
    }

    // Additional functions to track specific user interactions
    fun likeArtwork(artworkId: String) {
        trackEvents.trackArtworkLiked(artworkId)
    }

    fun unlikeArtwork(artworkId: String) {
        trackEvents.trackArtworkUnliked(artworkId)
    }

    fun viewArtistProfile(artistId: String) {
        trackEvents.trackArtistProfileViewed(artistId)
    }

    fun saveArtworkToFavorites(artworkId: String) {
        trackEvents.trackArtworkSavedToFavorites(artworkId)
    }

    fun commentOnArtwork(artworkId: String) {
        trackEvents.trackArtworkCommented(artworkId)
    }

    fun refreshArtwork(artworkId: String) {
        getArtworkById(artworkId)
    }

    // Track when a recommended artwork is viewed from a specific source
    fun viewRecommendedArtwork(artworkId: String, source: String) {
        trackEvents.trackRecommendedArtworkViewed(artworkId, source)
    }

    // MVVM: Get artwork statistics (likes and comments count)
    fun getArtworkStats(artworkId: String) {
        viewModelScope.launch {
            artworkRepository.getArtworkStats(artworkId).collect { response ->
                _artworkStatsUiState.value = when (response) {
                    is ResponseStates.Loading -> ArtworkStatsUiState.Loading
                    is ResponseStates.Success -> ArtworkStatsUiState.Success(response.data)
                    is ResponseStates.Error -> ArtworkStatsUiState.Error(response.error)
                }
            }
        }
    }


    fun resetState(id: String?) {
        id?.let {
            getArtworkStats(id)
        }
    }

    fun showLikes(likes: String, update: MutableState<Int>): String {
        if(likes.toIntOrNull() != null){
            return likes.toInt().plus(update.value).toString()
        }else return likes
    }
}

sealed interface ArtworkDetailScreenUiState {
    object Loading : ArtworkDetailScreenUiState
    data class Success(val artwork: ArtworkDTO) : ArtworkDetailScreenUiState
    data class Error(val message: String) : ArtworkDetailScreenUiState
}

sealed interface ArtworksScreenUiState {
    object Loading : ArtworksScreenUiState
    data class Success(val artwork: List<ArtworkDTO>) : ArtworksScreenUiState
    data class Error(val message: String) : ArtworksScreenUiState
}

// MVVM: Artwork statistics UI state
sealed interface ArtworkStatsUiState {
    object Loading : ArtworkStatsUiState
    data class Success(val stats: ArtworkStatsResponse) : ArtworkStatsUiState
    data class Error(val message: String) : ArtworkStatsUiState
}

