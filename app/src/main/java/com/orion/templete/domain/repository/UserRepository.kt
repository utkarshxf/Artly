package com.orion.templete.domain.repository

import com.orion.templete.data.model.artist_model.ArtistDTO
import com.orion.templete.data.model.artwork_model.ArtworkDTO
import com.orion.templete.data.model.artwork_model.comments.CommentRequest
import com.orion.templete.data.model.artwork_model.comments.GetCommentsDTO
import com.orion.templete.data.model.user_model.RegisterArtistRequest
import com.orion.templete.data.model.user_model.UserDTO
import com.orion.templete.data.model.user_model.UserDetails
import com.orion.templete.util.ResponseStates
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for user and artist related operations.
 */
interface UserRepository {
    /**
     * Retrieve a user by their unique ID.
     * @param userId The unique id of the user to fetch.
     * @return A [Flow] emitting [ResponseStates] wrapping a [UserDTO] on success or an error state.
     */
    suspend fun getUserByUserId(userId: String): Flow<ResponseStates<UserDTO>>

    /**
     * Retrieve artist details by the artist's id.
     * @param artistId The id of the artist to retrieve.
     * @param currentUserId The id of the current user (used to determine follow status, etc.).
     * @return A [Flow] emitting [ResponseStates] wrapping an [ArtistDTO].
     */
    suspend fun getArtistByArtistId(artistId: String, currentUserId: String): Flow<ResponseStates<ArtistDTO>>

    /**
     * Retrieve artist details by an artwork id.
     * @param artworkId The id of the artwork whose artist should be returned.
     * @param currentUserId The id of the current user (used to determine follow status, etc.).
     * @return A [Flow] emitting [ResponseStates] wrapping an [ArtistDTO].
     */
    suspend fun getArtistByArtworkId(artworkId: String, currentUserId: String): Flow<ResponseStates<ArtistDTO>>

    /**
     * Create a new user in the system.
     * @param userDetails The details required to create the user.
     * @return A [Flow] emitting [ResponseStates] wrapping the created [UserDetails] on success.
     */
    suspend fun createUser(userDetails: UserDetails): Flow<ResponseStates<UserDetails>>

    /**
     * Update an existing user's details.
     * @param userDetails The updated user details to persist.
     * @return A [Flow] emitting [ResponseStates] wrapping the updated [UserDetails] on success.
     */
    suspend fun updateUser(userDetails: UserDetails): Flow<ResponseStates<UserDetails>>

    /**
     * Follow an artist/user.
     * @param userId The id of the user who performs the follow.
     * @param artistId The id of the artist being followed.
     * @return A [Flow] emitting [ResponseStates] wrapping Unit on success.
     */
    suspend fun followUser(userId: String, artistId: String): Flow<ResponseStates<Unit>>

    /**
     * Unfollow an artist.
     * @param userId The id of the user who performs the unfollow.
     * @param artistId The id of the artist being unfollowed.
     * @return A [Flow] emitting [ResponseStates] wrapping Unit on success.
     */
    suspend fun unfollowArtist(userId: String, artistId: String): Flow<ResponseStates<Unit>>

    /**
     * Like an artwork.
     * @param userId The id of the user liking the artwork.
     * @param artworkId The id of the artwork to like.
     * @return A [Flow] emitting [ResponseStates] wrapping Unit on success.
     */
    suspend fun likeArtwork(userId: String, artworkId: String): Flow<ResponseStates<Unit>>

    /**
     * Remove a like from an artwork.
     * @param userId The id of the user unliking the artwork.
     * @param artworkId The id of the artwork to unlike.
     * @return A [Flow] emitting [ResponseStates] wrapping Unit on success.
     */
    suspend fun unLikeArtwork(userId: String, artworkId: String): Flow<ResponseStates<Unit>>

    /**
     * Post a comment on an artwork.
     * @param userId The id of the user posting the comment.
     * @param artworkId The id of the artwork to comment on.
     * @param comment The comment request payload.
     * @return A [Flow] emitting [ResponseStates] wrapping Unit on success.
     */
    suspend fun commentOnArtwork(userId: String, artworkId: String, comment: CommentRequest): Flow<ResponseStates<Unit>>

    /**
     * Get all comments for a given artwork.
     * @param artworkId The id of the artwork whose comments should be returned.
     * @return A [Flow] emitting [ResponseStates] wrapping a list of [GetCommentsDTO].
     */
    suspend fun getCommentsOnArtwork(artworkId: String): Flow<ResponseStates<List<GetCommentsDTO>>>

    /**
     * Get all artworks for a specific artist.
     * @param userId The id of the current user (may be used for contextual info like favorites).
     * @param artistId The id of the artist whose artworks should be returned.
     * @return A [Flow] emitting [ResponseStates] wrapping a list of [ArtworkDTO].
     */
    suspend fun getArtistArtworks(userId: String, artistId: String): Flow<ResponseStates<List<ArtworkDTO>>>

    /**
     * Register a user as an artist with a display name and profile image.
     * @param userId The id of the user registering as an artist.
     * @param artist The data for the artist profile.
     * @return A [Flow] emitting [ResponseStates] wrapping Unit on success.
     */
    suspend fun registerAsArtist (artistData:RegisterArtistRequest): Flow<ResponseStates<RegisterArtistRequest>>

    /**
     * Check whether a given user is registered as an artist.
     * @param userId The id of the user to check.
     * @return A [Flow] emitting [ResponseStates] wrapping a Boolean indicating artist status.
     */
    suspend fun isUserArtist(userId: String): Flow<ResponseStates<Boolean>>
}
