package com.orion.templete.presentation.artist_register

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orion.templete.data.model.user_model.RegisterArtistRequest
import com.orion.templete.data.model.user_model.UserDetails
import com.orion.templete.domain.repository.UserRepository
import com.orion.templete.presentation.profile.ProfileScreenUiState
import com.orion.templete.util.ResponseStates
import com.orion.templete.util.SecureStorage
import com.orion.templete.util.TrackEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtistRegisterScreenViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val secureStorage: SecureStorage,
    private val trackEvents: TrackEvents
) : ViewModel() {

    var registerArtistState by mutableStateOf<RegisterArtistUiState>(
        RegisterArtistUiState(
            isLoading = false,
            data = null,
            error = null
        )
    )
        private set
    val currentUserId = secureStorage.getUserId()

    fun registerAsArtist(
        artist: RegisterArtistRequest
    ) {
        val data1 = artist.copy(id = currentUserId)
        currentUserId?.let { userId ->
            viewModelScope.launch {
                userRepository.registerAsArtist(
                      data1
                ).collect { response ->
                    registerArtistState = when (response) {
                        is ResponseStates.Loading -> RegisterArtistUiState(isLoading = true)
                        is ResponseStates.Success -> {
                            trackEvents.tackUserBecomeArtist(userId)
                            secureStorage.setUserIsAnArtist(true)
                            RegisterArtistUiState(data = response.data)
                        }
                        is ResponseStates.Error -> RegisterArtistUiState(error = response.error)
                    }
                }
            }
        }
    }
}

data class RegisterArtistUiState(
    val isLoading: Boolean = false,
    val data: RegisterArtistRequest? = null,
    val error: String? = null
)