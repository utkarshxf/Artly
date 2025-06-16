package com.orion.templete.presentation.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orion.templete.data.model.artist_model.ArtistDTO
import com.orion.templete.data.model.user_model.UserDTO
import com.orion.templete.domain.repository.UserRepository
import com.orion.templete.util.ResponseStates
import com.orion.templete.util.SecureStorage
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val currentUserId = secureStorage.getUserId()

    // State for artist registration
    var artistRegistrationState by mutableStateOf<ArtistRegistrationState>(ArtistRegistrationState.Initial)
        private set

    init {
        currentUserId?.let {
            getUserProfile(currentUserId)
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

    fun refreshProfile() {
        if (currentUserId != null) {
            getUserProfile(currentUserId)
        }
    }

    fun checkIfUserIsArtist(userId: String) {
        viewModelScope.launch {
            userRepository.isUserArtist(userId)
                .collect { response ->
                    when (response) {
                        is ResponseStates.Loading -> {
                            // Do nothing, we're already in the initial state
                        }
                        is ResponseStates.Success -> {
                            // If the user is an artist, refresh the profile to show the artist status
                            if (response.data) {
                                refreshProfile()
                            }
                        }
                        is ResponseStates.Error -> {
                            // Do nothing, we're already in the initial state
                        }
                    }
                }
        }
    }

    fun registerAsArtist(artist: ArtistDTO) {
        artistRegistrationState = ArtistRegistrationState.Loading
        viewModelScope.launch {
            userRepository.registerAsArtist(artist)
                .collect { response ->
                    artistRegistrationState = when (response) {
                        is ResponseStates.Loading -> ArtistRegistrationState.Loading
                        is ResponseStates.Success -> {
                            refreshProfile() // Refresh the profile to show the artist status
                            ArtistRegistrationState.Success
                        }
                        is ResponseStates.Error -> ArtistRegistrationState.Error(response.error)
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

sealed interface ArtistRegistrationState {
    object Initial : ArtistRegistrationState
    object Loading : ArtistRegistrationState
    object Success : ArtistRegistrationState
    data class Error(val message: String) : ArtistRegistrationState
}
