package com.orion.templete.presentation.artist_register

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orion.templete.data.model.user_model.RegisterArtistRequest
import com.orion.templete.domain.repository.UserRepository
import com.orion.templete.util.ResponseStates
import com.orion.templete.util.SecureStorage
import com.orion.templete.util.TrackEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditArtistViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val secureStorage: SecureStorage,
    private val trackEvents: TrackEvents
) : ViewModel() {

    // MVVM: Separate StateFlow for loading initial artist data
    private val _loadArtistDataState = MutableStateFlow<LoadArtistDataState>(LoadArtistDataState.Idle)
    val loadArtistDataState: StateFlow<LoadArtistDataState> = _loadArtistDataState

    // MVVM: Separate StateFlow for updating artist data
    private val _updateArtistState = MutableStateFlow<UpdateArtistState>(UpdateArtistState.Idle)
    val updateArtistState: StateFlow<UpdateArtistState> = _updateArtistState

    // MVVM: Store loaded artist data separately
    private val _currentArtistData = MutableStateFlow<RegisterArtistRequest?>(null)
    val currentArtistData: StateFlow<RegisterArtistRequest?> = _currentArtistData

    // State for UI loading (initial load)
    var initialLoadState by mutableStateOf<InitialLoadState>(InitialLoadState.Idle)
        private set

    val currentUserId = secureStorage.getUserId()

    // Business Logic: Fetch artist data from saved user data
    fun loadArtistData() {
        initialLoadState = InitialLoadState.Loading
        _loadArtistDataState.value = LoadArtistDataState.Loading
        try {
            // MVVM: First check if we have cached artist details from ProfileScreenViewModel
            val cachedArtistData = secureStorage.getCurrentArtistDetails()
            if (cachedArtistData != null) {
                _currentArtistData.value = cachedArtistData
                _loadArtistDataState.value = LoadArtistDataState.Success(cachedArtistData)
                initialLoadState = InitialLoadState.Success
            } else {
                // Fallback to UserDTO if no cached artist data
                val userDto = secureStorage.getUserDetails()
                if (userDto != null) {
                    // Map UserDTO to RegisterArtistRequest for editing
                    val artistData = RegisterArtistRequest(
                        id = userDto.id,
                        name = userDto.name,
                        birth_date = userDto.dob ?: "",
                        death_date = "",
                        nationality = userDto.countryIso2 ?: "",
                        notable_works = "",
                        art_movement = "",
                        education = "",
                        awards = "",
                        image_url = userDto.profilePicture ?: "",
                        wikipedia_url = "",
                        description = ""
                    )
                    _currentArtistData.value = artistData
                    _loadArtistDataState.value = LoadArtistDataState.Success(artistData)
                    initialLoadState = InitialLoadState.Success
                } else {
                    _loadArtistDataState.value = LoadArtistDataState.Error("No artist data found")
                    initialLoadState = InitialLoadState.Error("No artist data found")
                }
            }
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Error loading data"
            _loadArtistDataState.value = LoadArtistDataState.Error(errorMsg)
            initialLoadState = InitialLoadState.Error(errorMsg)
        }
    }

    // Business Logic: Update artist information
    fun updateArtist(artist: RegisterArtistRequest) {
        currentUserId?.let { userId ->
            viewModelScope.launch {
                _updateArtistState.value = UpdateArtistState.Loading
                userRepository.updateArtist(userId, artist).collect { response ->
                    _updateArtistState.value = when (response) {
                        is ResponseStates.Loading -> {
                            UpdateArtistState.Loading
                        }
                        is ResponseStates.Success -> {
                            // Update cached data after successful update
                            _currentArtistData.value = response.data
                            secureStorage.saveCurrentArtistDetails(response.data)
                            trackEvents.trackUserProfileUpdated(userId)
                            UpdateArtistState.Success(response.data)
                        }
                        is ResponseStates.Error -> {
                            UpdateArtistState.Error(response.error)
                        }
                    }
                }
            }
        }
    }
}

// MVVM: UI State for loading artist data
sealed class LoadArtistDataState {
    object Idle : LoadArtistDataState()
    object Loading : LoadArtistDataState()
    data class Success(val data: RegisterArtistRequest) : LoadArtistDataState()
    data class Error(val message: String) : LoadArtistDataState()
}

// MVVM: UI State for updating artist data
sealed class UpdateArtistState {
    object Idle : UpdateArtistState()
    object Loading : UpdateArtistState()
    data class Success(val data: RegisterArtistRequest) : UpdateArtistState()
    data class Error(val message: String) : UpdateArtistState()
}

// MVVM: UI State for initial data loading
sealed class InitialLoadState {
    object Idle : InitialLoadState()
    object Loading : InitialLoadState()
    object Success : InitialLoadState()
    data class Error(val message: String) : InitialLoadState()
}

