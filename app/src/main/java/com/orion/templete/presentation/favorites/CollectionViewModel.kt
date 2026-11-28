package com.orion.templete.presentation.favorites

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orion.templete.data.local.entity.FavouriteEntity
import com.orion.templete.data.model.artwork_model.ArtworkDTO
import com.orion.templete.data.model.favorits.favoritesDTO
import com.orion.templete.data.repository.FavouritesOfflineStore
import com.orion.templete.domain.repository.ArtworkRepository
import com.orion.templete.util.ResponseStates
import com.orion.templete.util.SecureStorage
import com.orion.templete.util.TrackEvents
import com.orion.templete.work.FavouriteArtworkDownloadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val artworkRepository: ArtworkRepository,
    private val secureStorage: SecureStorage,
    private val trackEvents: TrackEvents,
    private val favouritesOfflineStore: FavouritesOfflineStore,
    @ApplicationContext private val appContext: Context
) : ViewModel() {
    var collectionScreenUiState by mutableStateOf<CollectionScreenUiState>(CollectionScreenUiState.Loading)
        private set

    var addToFavoritesUiState by mutableStateOf<AddToFavoritesUiState>(AddToFavoritesUiState.Ideal)
        private set

    var favoritesUiState by mutableStateOf<FavoritesUiState>(FavoritesUiState.Loading)
        private set

    var savedArtworksUiState by mutableStateOf<SavedArtworksUiState>(SavedArtworksUiState.Loading)
        private set

    var createFavoritesUiState by mutableStateOf<CreateFavoritesUiState>(CreateFavoritesUiState.Ideal)
        private set

    val currentUserId = secureStorage.getUserId()?:""

    init {
        // Guests have no collections; skip the call that would 401
        if (currentUserId.isNotBlank()) getFavoritesByUserId()
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
    fun saveOnFavorites(favoritesId: String, artworkId: String, artwork: ArtworkDTO? = null) {
        viewModelScope.launch {
            artworkRepository.saveOnFavorites(favoritesId, artworkId).collect { response ->
                addToFavoritesUiState = when (response) {
                    is ResponseStates.Loading -> AddToFavoritesUiState.Loading
                    is ResponseStates.Success -> {
                        Log.d("ErrorUser", "saveOnFavorites: ${response.data}")
                        mirrorFavouriteOffline(artwork, artworkId, favoritesId, synced = true)
                        AddToFavoritesUiState.Success(response.data)
                    }
                    is ResponseStates.Error -> {
                        Log.d("ErrorUser", "saveOnFavorites: ${response.error}")
                        // Offline or server down: keep the favourite locally and sync later
                        mirrorFavouriteOffline(artwork, artworkId, favoritesId, synced = false)
                        AddToFavoritesUiState.Error(response.error)
                    }
                }
            }
        }
    }

    // Mirror the save into the local favourites store and queue the offline image copy
    private fun mirrorFavouriteOffline(artwork: ArtworkDTO?, artworkId: String, collectionId: String, synced: Boolean) {
        viewModelScope.launch {
            val dto = artwork ?: ArtworkDTO(id = artworkId)
            try {
                favouritesOfflineStore.markFavourite(dto, collectionId = collectionId, synced = synced)
                FavouriteArtworkDownloadWorker.enqueue(appContext, artworkId)
            } catch (t: Throwable) {
                Log.w("OfflineFavourites", "mirror failed", t)
            }
        }
    }

    fun refreshArtwork(favoriteId: String) {
        getArtworkByFavoriteId(favoriteId)
    }

    // Flat view of every saved artwork across all of the user's collections (profile Favorites tab), newest first
    fun getAllSavedArtworks() {
        viewModelScope.launch {
            savedArtworksUiState = SavedArtworksUiState.Loading
            artworkRepository.getAllFavorites(currentUserId).collect { response ->
                when (response) {
                    is ResponseStates.Loading -> {}
                    is ResponseStates.Error -> savedArtworksUiState = offlineSavedArtworksFallback(response.error)
                    is ResponseStates.Success -> {
                        val collections = response.data
                        if (collections.isEmpty()) {
                            savedArtworksUiState = SavedArtworksUiState.Success(emptyList())
                            return@collect
                        }
                        val all = mutableListOf<ArtworkDTO>()
                        var failed: String? = null
                        for (favorite in collections) {
                            val favoriteId = favorite.id ?: continue
                            artworkRepository.getArtworkByFavoriteId(favoriteId).collect { artworks ->
                                when (artworks) {
                                    is ResponseStates.Loading -> {}
                                    is ResponseStates.Success -> all.addAll(artworks.data)
                                    is ResponseStates.Error -> failed = artworks.error
                                }
                            }
                        }
                        savedArtworksUiState = if (all.isEmpty() && failed != null) {
                            offlineSavedArtworksFallback(failed!!)
                        } else {
                            SavedArtworksUiState.Success(all.distinctBy { it.id }.asReversed())
                        }
                    }
                }
            }
        }
    }

    // Offline favourites: when the network fails, serve the local Room store
    private suspend fun offlineSavedArtworksFallback(error: String): SavedArtworksUiState {
        return try {
            val local = favouritesOfflineStore.observeFavourites().first().map {
                ArtworkDTO(
                    id = it.artworkId,
                    title = it.title,
                    artist = it.artist,
                    imageUrl = it.imageUrl,
                    image_url_compressed = it.imageUrlCompressed,
                    canSave = it.canSave,
                    canSetWallpaper = it.canSetWallpaper
                )
            }
            if (local.isEmpty()) SavedArtworksUiState.Error(error) else SavedArtworksUiState.Success(local)
        } catch (t: Throwable) {
            SavedArtworksUiState.Error(error)
        }
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

sealed interface SavedArtworksUiState {
    object Loading : SavedArtworksUiState
    data class Success(val artworks: List<ArtworkDTO>) : SavedArtworksUiState
    data class Error(val message: String) : SavedArtworksUiState
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
