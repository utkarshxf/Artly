package com.orion.templete.presentation.artwork_upload

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orion.templete.data.model.artwork_model.ArtworkUploadDTO
import com.orion.templete.domain.repository.uploadRepository
import com.orion.templete.util.ResponseStates
import com.orion.templete.util.SecureStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UploadViewModel @Inject constructor(
    private val uploadRepository: uploadRepository,
    private val secureStorage: SecureStorage
) : ViewModel() {

    // UI state for the upload screen
    var uploadUiState by mutableStateOf<UploadUiState>(UploadUiState.Initial)
        private set

    // State for the artwork details form
    var title by mutableStateOf("")
        private set
    var medium by mutableStateOf("")
        private set
    var description by mutableStateOf("")
        private set
    var dimensions by mutableStateOf("")
        private set
    var currentLocation by mutableStateOf("")
        private set
    var periodStyle by mutableStateOf("")
        private set
    var artMovement by mutableStateOf("")
        private set
    var licenseInfo by mutableStateOf("")
        private set
    var sourceUrl by mutableStateOf("")
        private set
    var genreName by mutableStateOf("")
        private set
    var showLicenseInfo by mutableStateOf(false)
        private set

    // Image URIs
    private val _originalImageUri = MutableStateFlow<Uri?>(null)
    val originalImageUri: StateFlow<Uri?> = _originalImageUri.asStateFlow()

    private val _croppedImageUri = MutableStateFlow<Uri?>(null)
    val croppedImageUri: StateFlow<Uri?> = _croppedImageUri.asStateFlow()

    // Image URLs (after upload)
    private var _originalImageUrl: String = ""
    private var _compressedImageUrl: String = ""

    // Upload progress
    private val _uploadProgress = MutableStateFlow(0f)
    val uploadProgress: StateFlow<Float> = _uploadProgress.asStateFlow()

    // Genre search results
    private val _genreSearchResults = MutableStateFlow<List<GenreItem>>(emptyList())
    val genreSearchResults: StateFlow<List<GenreItem>> = _genreSearchResults.asStateFlow()

    var selectedGenre by mutableStateOf<GenreItem?>(null)
        private set

    // Current user ID
    private val currentUserId = secureStorage.getUserId() ?: ""

    // Set the original image URI and start uploading in the background
    fun setOriginalImageUri(uri: Uri) {
        _originalImageUri.value = uri
        uploadUiState = UploadUiState.ImageSelected
        // Start uploading the image in the background
        uploadImage(uri, false)
    }

    // Set the cropped image URI and update the original image upload
    fun setCroppedImageUri(uri: Uri) {
        _croppedImageUri.value = uri
        // Upload the cropped image
        uploadImage(uri, true)
    }

    // Upload an image to the server
    private fun uploadImage(uri: Uri, isCompressed: Boolean) {
        viewModelScope.launch {
            // Start progress at 0
            _uploadProgress.value = 0f

            // Simulate progress updates while waiting for the repository
            val progressJob = viewModelScope.launch {
                for (i in 1..100) {
                    _uploadProgress.value = i / 100f
                    delay(20) // Update every 20ms
                }
            }

            // Collect the repository response
            uploadRepository.uploadImage(uri, isCompressed).collect { response ->
                when (response) {
                    is ResponseStates.Loading -> {
                        // Progress is being simulated by the job above
                    }
                    is ResponseStates.Success -> {
                        // Cancel the progress simulation
                        progressJob.cancel()

                        // Set progress to 100%
                        _uploadProgress.value = 1f

                        val imageUrl = response.data
                        Log.d("UploadViewModel", "Image upload completed successfully: $imageUrl")

                        // Store the URL for later use when submitting the artwork
                        if (isCompressed) {
                            _compressedImageUrl = imageUrl
                        } else {
                            _originalImageUrl = imageUrl
                        }
                    }
                    is ResponseStates.Error -> {
                        // Cancel the progress simulation
                        progressJob.cancel()

                        Log.e("UploadViewModel", "Error uploading image: ${response.error}")
                        uploadUiState = UploadUiState.Error("Failed to upload image: ${response.error}")
                    }
                }
            }
        }
    }

    // Update form fields
    fun updateTitle(newTitle: String) {
        title = newTitle
    }

    fun updateMedium(newMedium: String) {
        medium = newMedium
    }

    fun updateDescription(newDescription: String) {
        description = newDescription
    }

    fun updateDimensions(newDimensions: String) {
        dimensions = newDimensions
    }

    fun updateCurrentLocation(newLocation: String) {
        currentLocation = newLocation
    }

    fun updatePeriodStyle(newStyle: String) {
        periodStyle = newStyle
    }

    fun updateArtMovement(newMovement: String) {
        artMovement = newMovement
    }

    fun updateLicenseInfo(newLicense: String) {
        licenseInfo = newLicense
    }

    fun updateSourceUrl(newUrl: String) {
        sourceUrl = newUrl
    }

    fun updateGenreName(newGenre: String) {
        genreName = newGenre
        searchGenres(newGenre)
    }

    fun toggleLicenseInfoVisibility() {
        showLicenseInfo = !showLicenseInfo
    }

    fun selectGenre(genre: GenreItem?) {
        selectedGenre = genre
    }


    // Search for genres
    private fun searchGenres(query: String) {
        if (query.isBlank()) {
            _genreSearchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            uploadRepository.searchGenres(query).collect { response ->
                when (response) {
                    is ResponseStates.Loading -> {
                        // Show loading state if needed
                    }
                    is ResponseStates.Success -> {
                        _genreSearchResults.value = response.data
                        Log.d("UploadViewModel", "Genres search successful: ${response.data}")
                    }
                    is ResponseStates.Error -> {
                        Log.e("UploadViewModel", "Error searching genres: ${response.error}")
                        _genreSearchResults.value = emptyList()
                    }
                }
            }
        }
    }

    // Create a new genre
    fun createNewGenre() {
        if (genreName.isBlank()) return

        viewModelScope.launch {
            uploadRepository.createGenre(genreName).collect { response ->
                when (response) {
                    is ResponseStates.Loading -> {
                        // Show loading state if needed
                    }
                    is ResponseStates.Success -> {
                        val newGenre = response.data
                        selectedGenre = newGenre
                        _genreSearchResults.value = listOf(newGenre)
                        Log.d("UploadViewModel", "Created new genre: $newGenre")
                    }
                    is ResponseStates.Error -> {
                        Log.e("UploadViewModel", "Error creating genre: ${response.error}")
                        uploadUiState = UploadUiState.Error("Failed to create genre: ${response.error}")
                    }
                }
            }
        }
    }

    // Submit the artwork
    fun submitArtwork() {
        if (title.isBlank()) {
            uploadUiState = UploadUiState.Error("Title is required")
            return
        }

        if (_croppedImageUri.value == null && _originalImageUri.value == null) {
            uploadUiState = UploadUiState.Error("Image is required")
            return
        }

        if (selectedGenre == null) {
            uploadUiState = UploadUiState.Error("Genre is required")
            return
        }

        uploadUiState = UploadUiState.Loading

        viewModelScope.launch {
            // Create the artwork DTO
            val artwork = ArtworkUploadDTO(
                title = title,
                imageUrl = _originalImageUrl.ifEmpty { _originalImageUri.value?.toString() ?: "" },
                imageUrlCompressed = _compressedImageUrl.ifEmpty { _croppedImageUri.value?.toString() ?: _originalImageUri.value?.toString() ?: "" },
                storageType = "Firebase",
                medium = medium,
                artist = secureStorage.getUserDetails()?.name ?: currentUserId,
                artType = "IMAGE",
                description = description,
                releasedDate = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(java.util.Date()),
                dimensions = dimensions,
                currentLocation = currentLocation,
                periodStyle = periodStyle,
                artMovement = artMovement,
                licenseInfo = if (showLicenseInfo) licenseInfo else null,
                sourceUrl = sourceUrl,
                genreId = selectedGenre?.id,
            )

            // Upload the artwork using the repository
            uploadRepository.uploadArtwork(currentUserId, artwork).collect { response ->
                when (response) {
                    is ResponseStates.Loading -> {
                        // Already in loading state
                    }
                    is ResponseStates.Success -> {
                        uploadUiState = UploadUiState.Success
                        Log.d("UploadViewModel", "Artwork submitted successfully: ${response.data}")
                    }
                    is ResponseStates.Error -> {
                        Log.e("UploadViewModel", "Error submitting artwork: ${response.error}")
                        uploadUiState = UploadUiState.Error("Failed to submit artwork: ${response.error}")
                    }
                }
            }
        }
    }

    // Reset the form
    fun resetForm() {
        title = ""
        medium = ""
        description = ""
        dimensions = ""
        currentLocation = ""
        periodStyle = ""
        artMovement = ""
        licenseInfo = ""
        sourceUrl = ""
        genreName = ""
        showLicenseInfo = false
        selectedGenre = null
        _originalImageUri.value = null
        _croppedImageUri.value = null
        _uploadProgress.value = 0f
        uploadUiState = UploadUiState.Initial
    }
}

// UI state for the upload screen
sealed class UploadUiState {
    object Initial : UploadUiState()
    object ImageSelected : UploadUiState()
    object Loading : UploadUiState()
    object Success : UploadUiState()
    data class Error(val message: String) : UploadUiState()
}

// Genre item model
data class GenreItem(
    val id: String,
    val key: String,
    val name: String
)
