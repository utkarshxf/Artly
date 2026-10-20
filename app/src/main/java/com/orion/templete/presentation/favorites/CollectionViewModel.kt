package com.orion.templete.presentation.favorites

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orion.templete.data.model.artwork_model.ArtworkDTO
import com.orion.templete.data.model.favorits.favoritesDTO
import com.orion.templete.domain.repository.ArtworkRepository
import com.orion.templete.util.ResponseStates
import com.orion.templete.util.SecureStorage
import com.orion.templete.util.TrackEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val artworkRepository: ArtworkRepository,
    private val secureStorage: SecureStorage,
    private val trackEvents: TrackEvents
) : ViewModel() {
    var collectionScreenUiState by mutableStateOf<CollectionScreenUiState>(CollectionScreenUiState.Loading)
        private set

    var addToFavoritesUiState by mutableStateOf<AddToFavoritesUiState>(AddToFavoritesUiState.Ideal)
        private set

    var favoritesUiState by mutableStateOf<FavoritesUiState>(FavoritesUiState.Loading)
        private set

    var createFavoritesUiState by mutableStateOf<CreateFavoritesUiState>(CreateFavoritesUiState.Ideal)
        private set

    val currentUserId = secureStorage.getUserId()?:""

    init {
        getFavoritesByUserId()
    }


    fun getArtworkByFavoriteId(favoriteId: String) {
        viewModelScope.launch {
            artworkRepository.getArtworkByFavoriteId(favoriteId).collect { response ->
                collectionScreenUiState = when (response) {
                    is ResponseStates.Loading -> CollectionScreenUiState.Loading
                    is ResponseStates.Success -> {
                        trackEvents.trackFavoritesViewed(favoriteId)
                        CollectionScreenUiState.Success(response.data)
                    }
                    is ResponseStates.Error -> CollectionScreenUiState.Error(response.error)
                }
            }
        }
    }

    fun createNewFavorites(favorites: favoritesDTO) {
        viewModelScope.launch {
            artworkRepository.createNewFavorites(currentUserId, favorites).collect { response ->
                createFavoritesUiState = when (response) {
                    is ResponseStates.Loading -> {
                        CreateFavoritesUiState.Loading
                    }
                    is ResponseStates.Success -> {
                        CreateFavoritesUiState.Success(response.data)
                    }
                    is ResponseStates.Error -> {
                        CreateFavoritesUiState.Error(response.error)
                    }
                }
            }
        }
    }

    fun resetState(){
        createFavoritesUiState = CreateFavoritesUiState.Ideal
        addToFavoritesUiState = AddToFavoritesUiState.Ideal
    }

    fun getFavoritesByUserId(){
        viewModelScope.launch {
            artworkRepository.getAllFavorites(currentUserId).collect { response ->
                favoritesUiState = when (response) {
                    is ResponseStates.Loading -> FavoritesUiState.Loading
                    is ResponseStates.Success -> FavoritesUiState.Success(response.data)
                    is ResponseStates.Error -> FavoritesUiState.Error(response.error)
                }
            }
        }
    }

    //save to fav
    fun saveOnFavorites(favoritesId: String, artworkId: String) {
        viewModelScope.launch {
            artworkRepository.saveOnFavorites(favoritesId, artworkId).collect { response ->
                addToFavoritesUiState = when (response) {
                    is ResponseStates.Loading -> AddToFavoritesUiState.Loading
                    is ResponseStates.Success -> {
                        Log.d("ErrorUser", "saveOnFavorites: ${response.data}")
                        AddToFavoritesUiState.Success(response.data)
                    }
                    is ResponseStates.Error -> {
                        Log.d("ErrorUser", "saveOnFavorites: ${response.error}")
                        AddToFavoritesUiState.Error(response.error)
                    }
                }
            }
        }
    }

    fun refreshArtwork(favoriteId: String) {
        getArtworkByFavoriteId(favoriteId)
    }
}

sealed interface CollectionScreenUiState {
    object Loading : CollectionScreenUiState
    data class Success(val artwork: List<ArtworkDTO>) : CollectionScreenUiState
    data class Error(val message: String) : CollectionScreenUiState
}
sealed interface AddToFavoritesUiState {
    object Loading : AddToFavoritesUiState
    object Ideal : AddToFavoritesUiState
    data class Success(val unit: Unit) : AddToFavoritesUiState
    data class Error(val message: String) : AddToFavoritesUiState
}
sealed interface FavoritesUiState {
    object Loading : FavoritesUiState
    data class Success(val favorites: List<favoritesDTO>) : FavoritesUiState
    data class Error(val message: String) : FavoritesUiState
}

sealed interface CreateFavoritesUiState {
    object Loading : CreateFavoritesUiState
    object Ideal : CreateFavoritesUiState
    data class Success(val favorite: favoritesDTO) : CreateFavoritesUiState
    data class Error(val message: String) : CreateFavoritesUiState
}
data class CollectionDetail(
    val id: String,
    val name: String,
    val artworkCount: Int,
    val artworks: List<ArtworkDTO>
)
