package com.orion.templete.presentation.home

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orion.templete.domain.repository.UserRepository
import com.orion.templete.util.ResponseStates
import com.orion.templete.util.SecureStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val secureStorage: SecureStorage
) : ViewModel() {
    var isArtistState by mutableStateOf<IsArtistUiState>(IsArtistUiState.Loading)
        private set
    val currentUserId = secureStorage.getUserId()

    val IsArtist =  secureStorage.userIsAnArtist()

    init {
        currentUserId?.let {
            checkIfUserIsArtist(currentUserId)
            refreshUserDetails(currentUserId)
        }
    }

    // Keep the cached profile (name, picture, country...) that chat, upload and artist signup read up to date
    private fun refreshUserDetails(userId: String) {
        viewModelScope.launch {
            userRepository.getUserByUserId(userId).collect { response ->
                if (response is ResponseStates.Success) secureStorage.saveUserDto(response.data)
            }
        }
    }

    private fun checkIfUserIsArtist(userId: String) {
        viewModelScope.launch {
            userRepository.isUserArtist(userId).collect { response ->
                    when (response) {
                        is ResponseStates.Loading -> IsArtistUiState.Loading
                        is ResponseStates.Success -> {
                            secureStorage.setUserIsAnArtist(response.data)
                            IsArtistUiState.Success(response.data)
                        }
                        is ResponseStates.Error -> IsArtistUiState.Error(response.error)
                    }
                }
        }
    }

}

sealed interface IsArtistUiState {
    object Loading : IsArtistUiState
    data class Success(val isArtist: Boolean) : IsArtistUiState
    data class Error(val message: String) : IsArtistUiState
}
