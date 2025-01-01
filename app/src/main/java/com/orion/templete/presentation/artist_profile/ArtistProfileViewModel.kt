package com.orion.templete.presentation.artist_profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orion.templete.data.model.artwork_model.ArtworkDTO
import com.orion.templete.data.model.artwork_model.comments.CommentRequest
import com.orion.templete.data.model.artwork_model.comments.GetCommentsDTO
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
    var artistProfileScreenUiState by mutableStateOf<ArtistProfileScreenUiState>(
        ArtistProfileScreenUiState.Loading
    )
        private set

    var followArtistUiState by mutableStateOf<FollowArtistUiState>(FollowArtistUiState.Ideal)
        private set

    var unFollowArtistUiState by mutableStateOf<UnFollowArtistUiState>(UnFollowArtistUiState.Ideal)
        private set

    var userLikeArtworkUiState by mutableStateOf<UserLikeArtistUiState>(UserLikeArtistUiState.Loading)
        private set

    var userUnLikeArtworkUiState by mutableStateOf<UserUnLikeArtistUiState>(UserUnLikeArtistUiState.Loading)
        private set

    var commentByArtistUiState by mutableStateOf<CommentByArtistUiState>(CommentByArtistUiState.Loading)
        private set

    var getCommentsOnArtworkUiState by mutableStateOf<GetCommentsOnArtworkUiState>(
        GetCommentsOnArtworkUiState.Loading
    )
        private set

    var addToFavoritesUiState by mutableStateOf<AddToFavoritesUiState>(AddToFavoritesUiState.Loading)
        private set

    var artWorksUiState by mutableStateOf<ArtWorksUiState>(ArtWorksUiState.Loading)
        private set


    fun getUserProfile(userId: String, currentUserId: String) {
        viewModelScope.launch {
            userRepository.getUserByUserId(userId, currentUserId).collect { response ->
                artistProfileScreenUiState = when (response) {
                    is ResponseStates.Loading -> ArtistProfileScreenUiState.Loading
                    is ResponseStates.Success -> ArtistProfileScreenUiState.Success(response.data)
                    is ResponseStates.Error -> ArtistProfileScreenUiState.Error(response.error)
                }
            }
        }
    }

    // follow
    fun followUser(userId: String, artistId: String) {
        viewModelScope.launch {
            userRepository.followUser(userId, artistId).collect { response ->
                followArtistUiState = when (response) {
                    is ResponseStates.Loading -> FollowArtistUiState.Loading
                    is ResponseStates.Success -> FollowArtistUiState.Success(response.data)
                    is ResponseStates.Error -> FollowArtistUiState.Error(response.error)
                }
            }
        }
    }

    // unfollow
    fun unfollowArtist(userId: String, artistId: String) {
        viewModelScope.launch {
            userRepository.unfollowArtist(userId, artistId).collect { response ->
                unFollowArtistUiState = when (response) {
                    is ResponseStates.Loading -> UnFollowArtistUiState.Loading
                    is ResponseStates.Success -> UnFollowArtistUiState.Success(response.data)
                    is ResponseStates.Error -> UnFollowArtistUiState.Error(response.error)
                }
            }
        }
    }

    //like
    fun likeArtwork(userId: String, artworkId: String) {
        viewModelScope.launch {
            userRepository.likeArtwork(userId, artworkId).collect { response ->
                userLikeArtworkUiState = when (response) {
                    is ResponseStates.Loading -> UserLikeArtistUiState.Loading
                    is ResponseStates.Success -> UserLikeArtistUiState.Success(response.data)
                    is ResponseStates.Error -> UserLikeArtistUiState.Error(response.error)
                }
            }
        }
    }

    //unlike
    fun unLikeArtwork(userId: String, artworkId: String) {
        viewModelScope.launch {
            userRepository.unLikeArtwork(userId, artworkId).collect { response ->
                userUnLikeArtworkUiState = when (response) {
                    is ResponseStates.Loading -> UserUnLikeArtistUiState.Loading
                    is ResponseStates.Success -> UserUnLikeArtistUiState.Success(response.data)
                    is ResponseStates.Error -> UserUnLikeArtistUiState.Error(response.error)
                }
            }
        }
    }

    //comment
    fun commentOnArtwork(userId: String, artworkId: String, comment: CommentRequest) {
        viewModelScope.launch {
            userRepository.commentOnArtwork(userId, artworkId, comment).collect { response ->
                commentByArtistUiState = when (response) {
                    is ResponseStates.Loading -> CommentByArtistUiState.Loading
                    is ResponseStates.Success -> CommentByArtistUiState.Success(response.data)
                    is ResponseStates.Error -> CommentByArtistUiState.Error(response.error)
                }
            }
        }
    }

    //getAllComments
    fun getAllComments(artworkId: String) {
        viewModelScope.launch {
            userRepository.getCommentsOnArtwork(artworkId).collect { response ->
                getCommentsOnArtworkUiState = when (response) {
                    is ResponseStates.Loading -> GetCommentsOnArtworkUiState.Loading
                    is ResponseStates.Success -> GetCommentsOnArtworkUiState.Success(response.data)
                    is ResponseStates.Error -> GetCommentsOnArtworkUiState.Error(response.error)
                }
            }
        }
    }

    //save to fav
    fun saveOnFavorites(userId: String, artworkId: String) {
        viewModelScope.launch {
            userRepository.saveOnFavorites(userId, artworkId).collect { response ->
                addToFavoritesUiState = when (response) {
                    is ResponseStates.Loading -> AddToFavoritesUiState.Loading
                    is ResponseStates.Success -> AddToFavoritesUiState.Success(response.data)
                    is ResponseStates.Error -> AddToFavoritesUiState.Error(response.error)
                }
            }
        }
    }

    //get list of artwork
    fun getArtistArtworks(userId: String, artistId: String) {
        viewModelScope.launch {
            userRepository.getArtistArtworks(userId, artistId).collect { response ->
                artWorksUiState = when (response) {
                    is ResponseStates.Loading -> ArtWorksUiState.Loading
                    is ResponseStates.Success -> ArtWorksUiState.Success(response.data)
                    is ResponseStates.Error -> ArtWorksUiState.Error(response.error)
                }
            }
        }
    }

    fun refreshProfile(userId: String, currentUserId: String) {
        getUserProfile(userId, currentUserId)
    }

    fun refreshComments(artworkId: String) {
        getAllComments(artworkId)
    }
}
sealed interface ArtistProfileScreenUiState {
    object Loading : ArtistProfileScreenUiState
    data class Success(val user: UserDTO) : ArtistProfileScreenUiState
    data class Error(val message: String) : ArtistProfileScreenUiState
}

sealed interface UserLikeArtistUiState {
    object Loading : UserLikeArtistUiState
    data class Success(val unit: Unit) : UserLikeArtistUiState
    data class Error(val message: String) : UserLikeArtistUiState
}

sealed interface UserUnLikeArtistUiState {
    object Loading : UserUnLikeArtistUiState
    data class Success(val unit: Unit) : UserUnLikeArtistUiState
    data class Error(val message: String) : UserUnLikeArtistUiState
}

sealed interface FollowArtistUiState {
    object Ideal : FollowArtistUiState
    object Loading : FollowArtistUiState
    data class Success(val unit: Unit) : FollowArtistUiState
    data class Error(val message: String) : FollowArtistUiState
}

sealed interface UnFollowArtistUiState {
    object Ideal : UnFollowArtistUiState
    object Loading : UnFollowArtistUiState
    data class Success(val unit: Unit) : UnFollowArtistUiState
    data class Error(val message: String) : UnFollowArtistUiState
}

sealed interface CommentByArtistUiState {
    object Loading : CommentByArtistUiState
    data class Success(val unit: Unit) : CommentByArtistUiState
    data class Error(val message: String) : CommentByArtistUiState
}

sealed interface GetCommentsOnArtworkUiState {
    object Loading : GetCommentsOnArtworkUiState
    data class Success(val data: List<GetCommentsDTO>) : GetCommentsOnArtworkUiState
    data class Error(val message: String) : GetCommentsOnArtworkUiState
}

sealed interface AddToFavoritesUiState {
    object Loading : AddToFavoritesUiState
    data class Success(val unit: Unit) : AddToFavoritesUiState
    data class Error(val message: String) : AddToFavoritesUiState
}

sealed interface ArtWorksUiState {
    object Loading : ArtWorksUiState
    data class Success(val artworks: List<ArtworkDTO>) : ArtWorksUiState
    data class Error(val message: String) : ArtWorksUiState
}

