package com.orion.templete.presentation.artist_profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orion.templete.data.model.user_model.UserDTO
import com.orion.templete.domain.repository.UserRepository
import com.orion.templete.util.ResponseStates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtistProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {

    // Initialize with Loading state instead of null
    var artistProfileData by mutableStateOf<ArtistProfileScreenUiState>(ArtistProfileScreenUiState.Loading)
        private set

    fun getUserProfile(userId: String) {
        viewModelScope.launch {
            userRepository.getUserByUserId(userId)
                .collect { response ->
                    artistProfileData = when (response) {
                        is ResponseStates.Loading -> ArtistProfileScreenUiState.Loading
                        is ResponseStates.Success -> ArtistProfileScreenUiState.Success(response.data)
                        is ResponseStates.Error -> ArtistProfileScreenUiState.Error(response.error)
                    }
                }
        }
    }

    fun refreshProfile(userId: String) {
        getUserProfile(userId)
    }
}

sealed interface ArtistProfileScreenUiState {
    object Loading : ArtistProfileScreenUiState
    data class Success(val user: UserDTO) : ArtistProfileScreenUiState
    data class Error(val message: String) : ArtistProfileScreenUiState
}