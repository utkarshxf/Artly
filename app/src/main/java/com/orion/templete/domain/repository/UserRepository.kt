package com.orion.templete.domain.repository

import com.orion.templete.data.model.artist_model.ArtistDTO
import com.orion.templete.data.model.artwork_model.ArtworkDTO
import com.orion.templete.data.model.artwork_model.comments.CommentRequest
import com.orion.templete.data.model.artwork_model.comments.GetCommentsDTO
import com.orion.templete.data.model.favorits.favoritesDTO
import com.orion.templete.data.model.user_model.UserDTO
import com.orion.templete.data.model.user_model.UserDetails
import com.orion.templete.util.ResponseStates
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun getUserByUserId(
        userId: String,
    ): Flow<ResponseStates<UserDTO>>


    suspend fun getArtistByArtistId(
        artistId: String,
        currentUserId: String
    ): Flow<ResponseStates<ArtistDTO>>

    suspend fun getArtistByArtworkId(
        artworkId: String,
        currentUserId: String
    ): Flow<ResponseStates<ArtistDTO>>

    suspend fun createUser(userDetails: UserDetails): Flow<ResponseStates<UserDetails>>
    suspend fun followUser(userId: String, artistId: String): Flow<ResponseStates<Unit>>
    suspend fun unfollowArtist(userId: String, artistId: String): Flow<ResponseStates<Unit>>
    suspend fun likeArtwork(userId: String, artworkId: String): Flow<ResponseStates<Unit>>
    suspend fun unLikeArtwork(userId: String, artworkId: String): Flow<ResponseStates<Unit>>
    suspend fun commentOnArtwork(
        userId: String,
        artworkId: String,
        comment: CommentRequest
    ): Flow<ResponseStates<Unit>>

    suspend fun getCommentsOnArtwork(artworkId: String): Flow<ResponseStates<List<GetCommentsDTO>>>
    suspend fun getArtistArtworks(
        userId: String,
        artistId: String
    ): Flow<ResponseStates<List<ArtworkDTO>>>
}