package com.orion.templete.presentation.swipe

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orion.templete.data.model.artwork_model.ArtworkDTO
import com.orion.templete.domain.paginator.DefaultPaginator
import com.orion.templete.domain.repository.ArtworkRepository
import com.orion.templete.usecase.GetArtworkUseCase
import com.orion.templete.util.ResponseStates
import com.orion.templete.util.SecureStorage
import com.orion.templete.util.TrackEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class SwipeScreenViewModel @Inject constructor(
    private val repository: ArtworkRepository,
    private val likeArtworkUseCase: GetArtworkUseCase,
    private val secureStorage: SecureStorage,
    private val trackEvents: TrackEvents // Add TrackEvents dependency
) : ViewModel() {
    val userId = secureStorage.getUserId()?:""
    var state by mutableStateOf(ScreenState())
        private set
    var likeArtworkState by mutableStateOf<ResponseStates<Boolean>>(ResponseStates.Loading)
        private set
    var disLikeArtworkState by mutableStateOf<ResponseStates<Boolean>>(ResponseStates.Loading)
        private set

    private val pagination: DefaultPaginator<Int, ArtworkDTO> = DefaultPaginator(
        initialKey = state.page,
        onLoadUpdated = { isLoading ->
            state = state.copy(isLoading = isLoading)
        },
        onRequest = { nextPage ->
            repository.paginationArtwork(userId = userId, nextPage, 10)
        },
        getNextKey = {
            state.page + 1
        },
        onError = { throwable ->
            state = state.copy(error = throwable?.localizedMessage)
        },
        onSuccess = { items, newKey ->
            // Track when new artworks are loaded for recommendation views
            if (items.isNotEmpty()) {
                trackEvents.trackScreenViewed("Swipe Screen - Page ${newKey}")
            }

            state = state.copy(
                items = items,
                page = newKey,
                endReached = items.isEmpty()
            )
        }
    )

    init {
        loadNextItems()
        // Track when app swipe screen is opened
        trackEvents.trackAppOpened()
    }

    fun loadNextItems() {
        viewModelScope.launch {
            try {
                pagination.loadNextItems()
            } catch (e: Exception) {
                Log.e("MyViewModel", "Error loading items", e)
            }
        }
    }

    fun resetPagination() {
        pagination.reset()
        state = ScreenState()
        loadNextItems()
    }

    fun likeArtwork(artworkId: String) {
        viewModelScope.launch {
            likeArtworkUseCase.likeArtwork(artworkId, userId).collect { resource ->
                likeArtworkState = resource

                // Track when artwork is liked successfully
                if (resource is ResponseStates.Success && resource.data) {
                    trackEvents.trackArtworkLiked(artworkId)
                }
            }
        }
    }

    fun disLikeArtwork(artworkId: String) {
        viewModelScope.launch {
            likeArtworkUseCase.disLikeArtwork(artworkId, userId).collect { resource ->
                disLikeArtworkState = resource

                // Track when artwork is disliked successfully
                if (resource is ResponseStates.Success && resource.data) {
                    trackEvents.trackArtworkDisliked(artworkId)
                }
            }
        }
    }

    // Additional tracking methods for specific user interactions
    fun trackArtworkView(artworkId: String) {
        trackEvents.trackArtworkViewed(artworkId)
    }

    fun trackRecommendedArtworkView(artworkId: String) {
        trackEvents.trackRecommendedArtworkViewed(artworkId, "Swipe Card")
    }

    fun trackArtworkSavedToFavorites(artworkId: String) {
        trackEvents.trackArtworkSavedToFavorites(artworkId)
    }

    fun trackArtistProfileView(artistId: String) {
        trackEvents.trackArtistProfileViewed(artistId)
    }

    override fun onCleared() {
        super.onCleared()
        // Track when user leaves the swipe screen
        trackEvents.trackAppClosed()
    }
}

data class ScreenState(
    val isLoading: Boolean = false,
    val items: List<ArtworkDTO> = emptyList(),
    val error: String? = null,
    val endReached: Boolean = false,
    val page: Int = 0
)
