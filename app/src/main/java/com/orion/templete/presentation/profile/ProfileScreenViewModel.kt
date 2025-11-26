package com.orion.templete.presentation.profile

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orion.templete.data.model.artwork_model.ArtworkDTO
import com.orion.templete.data.model.user_model.UserDTO
import com.orion.templete.domain.repository.UserRepository
import com.orion.templete.util.ResponseStates
import com.orion.templete.util.SecureStorage
import dagger.hilt.android.lifecycle.HiltViewModel
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

    val currentUserId = secureStorage.getUserId()
    val userIsArtist = secureStorage.userIsAnArtist()

    init {
        currentUserId?.let { userId ->
            getUserProfile(userId)

            // Collect from userIsArtist Flow
            viewModelScope.launch {
                secureStorage.userIsAnArtist().collect { isArtist ->
                    isUserArtist = isArtist
                    if (isArtist) {
                        getArtistArtworks(userId)
                    }
                }
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
        if (currentUserId != null) {
            getUserProfile(currentUserId)
            if (isUserArtist) {
                getArtistArtworks(currentUserId)
            }
        }
    }


    fun canRegisterAsArtist(): Boolean {
       return true
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
