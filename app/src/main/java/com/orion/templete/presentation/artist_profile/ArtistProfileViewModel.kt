package com.orion.templete.presentation.artist_profile

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orion.templete.data.model.artist_model.ArtistDTO
import com.orion.templete.data.model.artist_model.ArtistStatsResponse
import com.orion.templete.data.model.artwork_model.ArtworkDTO
import com.orion.templete.data.model.artwork_model.comments.CommentRequest
import com.orion.templete.data.model.artwork_model.comments.GetCommentsDTO
import com.orion.templete.data.model.favorits.favoritesDTO
import com.orion.templete.data.model.user_model.UserDTO
import com.orion.templete.domain.repository.UserRepository
import com.orion.templete.util.ResponseStates
import com.orion.templete.util.SecureStorage
import com.orion.templete.util.TrackEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtistProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val secureStorage: SecureStorage,
    private val trackEvents: TrackEvents // Add TrackEvents dependency
) : ViewModel() {

    // State variables remain the same
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

    var artWorksUiState by mutableStateOf<ArtWorksUiState>(ArtWorksUiState.Loading)
        private set

    // MVVM: Artist stats state
    var artistStatsUiState by mutableStateOf<ArtistStatsUiState>(ArtistStatsUiState.Loading)
        private set

    val currentUserId = secureStorage.getUserId()?:""

    fun getUserProfile(userId: String) {
        viewModelScope.launch {
            userRepository.getArtistByArtistId(userId, currentUserId).collect { response ->
                artistProfileScreenUiState = when (response) {
                    is ResponseStates.Loading -> ArtistProfileScreenUiState.Loading
                    is ResponseStates.Success -> {
                        // Track when artist profile is viewed successfully
                        trackEvents.trackArtistProfileViewed(userId)
                        ArtistProfileScreenUiState.Success(response.data)
                    }
                    is ResponseStates.Error -> ArtistProfileScreenUiState.Error(response.error)
                }
            }
        }
    }

    fun getArtistByArtworkId(artworkId: String) {
        viewModelScope.launch {
            userRepository.getArtistByArtworkId(artworkId, currentUserId).collect { response ->
                artistProfileScreenUiState = when (response) {
                    is ResponseStates.Loading -> ArtistProfileScreenUiState.Loading
                    is ResponseStates.Success -> {
                        // Track when viewing artist profile through artwork
                        trackEvents.trackArtistProfileViewed(response.data.id)
                        ArtistProfileScreenUiState.Success(response.data)
                    }
                    is ResponseStates.Error -> ArtistProfileScreenUiState.Error(response.error)
                }
            }
        }
    }

    // follow
    fun followUser(artistId: String) {
        viewModelScope.launch {
            userRepository.followUser(currentUserId, artistId).collect { response ->
                followArtistUiState = when (response) {
                    is ResponseStates.Loading -> FollowArtistUiState.Loading
                    is ResponseStates.Success -> {
                        // Track when artist is followed successfully
                        trackEvents.trackArtistFollowed(artistId)
                        FollowArtistUiState.Success(response.data)
                    }
                    is ResponseStates.Error -> FollowArtistUiState.Error(response.error)
                }
            }
        }
    }

    // unfollow
    fun unfollowArtist(artistId: String) {
        viewModelScope.launch {
            userRepository.unfollowArtist(currentUserId, artistId).collect { response ->
                unFollowArtistUiState = when (response) {
                    is ResponseStates.Loading -> UnFollowArtistUiState.Loading
                    is ResponseStates.Success -> {
                        // Track when artist is unfollowed
                        trackEvents.trackArtistUnfollowed(artistId)
                        UnFollowArtistUiState.Success(response.data)
                    }
                    is ResponseStates.Error -> UnFollowArtistUiState.Error(response.error)
                }
            }
        }
    }

    //like
    fun likeArtwork(artworkId: String) {
        viewModelScope.launch {
            userRepository.likeArtwork(currentUserId, artworkId).collect { response ->
                userLikeArtworkUiState = when (response) {
                    is ResponseStates.Loading -> UserLikeArtistUiState.Loading
                    is ResponseStates.Success -> {
                        // Track when artwork is liked
                        trackEvents.trackArtworkLiked(artworkId)
                        UserLikeArtistUiState.Success(response.data)
                    }
                    is ResponseStates.Error -> UserLikeArtistUiState.Error(response.error)
                }
            }
        }
    }

    //unlike
    fun unLikeArtwork(artworkId: String) {
        viewModelScope.launch {
            userRepository.unLikeArtwork(currentUserId, artworkId).collect { response ->
                userUnLikeArtworkUiState = when (response) {
                    is ResponseStates.Loading -> UserUnLikeArtistUiState.Loading
                    is ResponseStates.Success -> {
                        // Track when artwork is unliked
                        trackEvents.trackArtworkUnliked(artworkId)
                        UserUnLikeArtistUiState.Success(response.data)
                    }
                    is ResponseStates.Error -> UserUnLikeArtistUiState.Error(response.error)
                }
            }
        }
    }

    //comment
    fun commentOnArtwork(artworkId: String, comment: CommentRequest) {
        viewModelScope.launch {
            userRepository.commentOnArtwork(currentUserId, artworkId, comment).collect { response ->
                commentByArtistUiState = when (response) {
                    is ResponseStates.Loading -> CommentByArtistUiState.Loading
                    is ResponseStates.Success -> {
                        // Track when comment is posted
                        trackEvents.trackArtworkCommented(artworkId)
                        CommentByArtistUiState.Success(response.data)
                    }
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

    //get list of artwork
    fun getArtistArtworks(artistId: String) {
        viewModelScope.launch {
            userRepository.getArtistArtworks(currentUserId, artistId).collect { response ->
                artWorksUiState = when (response) {
                    is ResponseStates.Loading -> ArtWorksUiState.Loading
                    is ResponseStates.Success -> ArtWorksUiState.Success(response.data)
                    is ResponseStates.Error -> ArtWorksUiState.Error(response.error)
                }
            }
        }
    }

    // Track when specific artwork is viewed
    fun viewArtwork(artworkId: String) {
        trackEvents.trackArtworkViewed(artworkId)
    }

    fun refreshProfile(userId: String) {
        getUserProfile(userId)
    }

    fun refreshComments(artworkId: String) {
        getAllComments(artworkId)
    }

    // MVVM: Get artist statistics (followers, likes, total artworks)
    fun getArtistStats(artistId: String) {
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
sealed interface ArtistProfileScreenUiState {
    object Loading : ArtistProfileScreenUiState
    data class Success(val artist: ArtistDTO) : ArtistProfileScreenUiState
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

sealed interface ArtWorksUiState {
    object Loading : ArtWorksUiState
    data class Success(val artworks: List<ArtworkDTO>) : ArtWorksUiState
    data class Error(val message: String) : ArtWorksUiState
}

// MVVM: Artist statistics UI state
sealed interface ArtistStatsUiState {
    object Loading : ArtistStatsUiState
    data class Success(val stats: ArtistStatsResponse) : ArtistStatsUiState
    data class Error(val message: String) : ArtistStatsUiState
}

