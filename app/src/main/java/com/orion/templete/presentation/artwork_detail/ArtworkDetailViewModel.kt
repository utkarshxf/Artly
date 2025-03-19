package com.orion.templete.presentation.artwork_detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orion.templete.data.model.artwork_model.ArtworkDTO
import com.orion.templete.domain.repository.ArtworkRepository
import com.orion.templete.util.ResponseStates
import com.orion.templete.util.SecureStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtworkDetailViewModel @Inject constructor(
    private val artworkRepository: ArtworkRepository,
    private val secureStorage: SecureStorage
) : ViewModel() {
    var artworkDetailScreenUiState by mutableStateOf<ArtworkDetailScreenUiState>(ArtworkDetailScreenUiState.Loading)
        private set

    private val currentUserId = secureStorage.getUserDetails()?.id

    fun getArtworkById(artworkId: String) {
        viewModelScope.launch {
            if (currentUserId != null) {
                artworkRepository.getArtworkById(currentUserId , artworkId).collect { response ->
                    artworkDetailScreenUiState = when (response) {
                        is ResponseStates.Loading -> ArtworkDetailScreenUiState.Loading
                        is ResponseStates.Success -> ArtworkDetailScreenUiState.Success(response.data)
                        is ResponseStates.Error -> ArtworkDetailScreenUiState.Error(response.error)
                    }
                }
            }
        }
    }
    fun refreshArtwork( artworkId: String) {
        getArtworkById(artworkId)
    }
}

sealed interface ArtworkDetailScreenUiState {
    object Loading : ArtworkDetailScreenUiState
    data class Success(val artwork: ArtworkDTO) : ArtworkDetailScreenUiState
    data class Error(val message: String) : ArtworkDetailScreenUiState
}
