package com.orion.templete.presentation.profile

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orion.templete.data.model.artist_model.ArtistStatsResponse
import com.orion.templete.data.model.artwork_model.ArtworkDTO
import com.orion.templete.data.model.user_model.RegisterArtistRequest
import com.orion.templete.data.model.user_model.UserDTO
import com.orion.templete.domain.repository.UserRepository
import com.orion.templete.presentation.artist_profile.ArtistStatsUiState
import com.orion.templete.util.ResponseStates
import com.orion.templete.util.SecureStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileScreenViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val secureStorage: SecureStorage
) : ViewModel() {

    // Initialize with Loading state instead of null
    var userData by mutableStateOf<ProfileScreenUiState>(ProfileScreenUiState.Loading)
        private set

    var artWorksUiState by mutableStateOf<ArtWorksUiState>(ArtWorksUiState.Loading)
        private set

    var isUserArtist by mutableStateOf(false)
        private set

    // MVVM: StateFlow for artist details
    private val _artistDetails = MutableStateFlow<RegisterArtistRequest?>(null)
    val artistDetails: StateFlow<RegisterArtistRequest?> = _artistDetails

    // MVVM: Artist stats state for current user
    var artistStatsUiState by mutableStateOf<ArtistStatsUiState>(ArtistStatsUiState.Loading)
        private set

    val currentUserId = secureStorage.getUserId()
    val userIsArtist = secureStorage.userIsAnArtist()

    init {
        viewModelScope.launch {
            userIsArtist.collect { isArtist ->
                isUserArtist = isArtist
            }
        }
    }


    private fun getUserProfile(userId: String) {
        viewModelScope.launch {
            userRepository.getUserByUserId(userId)
                .collect { response ->
                    userData = when (response) {
                        is ResponseStates.Loading -> ProfileScreenUiState.Loading
                        is ResponseStates.Success -> {
                            secureStorage.saveUserDto(response.data)
                            ProfileScreenUiState.Success(response.data)
                        }
                        is ResponseStates.Error -> ProfileScreenUiState.Error(response.error)
                    }
                }
        }
    }

    // MVVM: Fetch artist details from API and cache in SecureStorage
    private fun getCurrentArtistDetails(artistId: String) {
        viewModelScope.launch {
            userRepository.getCurrentArtistDetails(artistId).collect { response ->
                when (response) {
                    is ResponseStates.Success -> {
                        // Save to SecureStorage for persistence
                        secureStorage.saveCurrentArtistDetails(response.data)
                        // Update StateFlow for UI
                        _artistDetails.value = response.data
                    }
                    is ResponseStates.Error -> {
                        // Try to load from cache if API fails
                        val cachedDetails = secureStorage.getCurrentArtistDetails()
                        _artistDetails.value = cachedDetails
                    }
                    is ResponseStates.Loading -> {
                        // Loading state - try cached data while loading
                        val cachedDetails = secureStorage.getCurrentArtistDetails()
                        if (cachedDetails != null) {
                            _artistDetails.value = cachedDetails
                        }
                    }
                }
            }
        }
    }

    fun getArtistArtworks(artistId: String) {
        viewModelScope.launch {
            userRepository.getArtistArtworks(currentUserId ?: "", artistId).collect { response ->
                artWorksUiState = when (response) {
                    is ResponseStates.Loading -> ArtWorksUiState.Loading
                    is ResponseStates.Success -> ArtWorksUiState.Success(response.data)
                    is ResponseStates.Error -> ArtWorksUiState.Error(response.error)
                }
            }
        }
    }

    fun refreshProfile() {
        viewModelScope.launch {
            if (currentUserId != null) {
                launch {  getUserProfile(currentUserId) }
                if (isUserArtist) {
                    launch {  getCurrentArtistDetails(currentUserId) }
                    launch {  getArtistArtworks(currentUserId) }
                    launch {  getArtistStats(currentUserId) }
                }
            }
        }
    }


    fun canRegisterAsArtist(): Boolean {
       return true
    }

    // MVVM: Fetch artist statistics for current user
    private fun getArtistStats(artistId: String) {
        viewModelScope.launch {
            userRepository.getArtistStats(artistId).collect { response ->
                artistStatsUiState = when (response) {
                    is ResponseStates.Loading -> ArtistStatsUiState.Loading
                    is ResponseStates.Success -> ArtistStatsUiState.Success(response.data)
                    is ResponseStates.Error -> ArtistStatsUiState.Error(response.error)
                }
            }
        }
    }
}

sealed interface ProfileScreenUiState {
    object Loading : ProfileScreenUiState
    data class Success(val user: UserDTO) : ProfileScreenUiState
    data class Error(val message: String) : ProfileScreenUiState
}

sealed interface ArtWorksUiState {
    object Loading : ArtWorksUiState
    data class Success(val artworks: List<ArtworkDTO>) : ArtWorksUiState
    data class Error(val message: String) : ArtWorksUiState
}
