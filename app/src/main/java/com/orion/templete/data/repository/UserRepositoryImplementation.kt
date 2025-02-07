package com.orion.templete.data.repository

import android.content.Context
import android.util.Log
import com.flashcall.me.data.local.dao.UserDao
import com.orion.templete.data.model.artwork_model.ArtworkDTO
import com.orion.templete.data.model.artwork_model.comments.CommentRequest
import com.orion.templete.data.model.artwork_model.comments.GetCommentsDTO
import com.orion.templete.data.model.favorits.favoritesDTO
import com.orion.templete.data.model.user_model.UserDTO
import com.orion.templete.data.model.user_model.UserDetails
import com.orion.templete.data.network.ApiService
import com.orion.templete.domain.repository.UserRepository
import com.orion.templete.util.ResponseStates
import com.orion.templete.util.SafeApiRequest
import com.orion.templete.util.isNetworkAvailable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class UserRepositoryImplementation @Inject constructor(
    private val apiService: ApiService,
    private val userDao: UserDao,
    private val context: Context
) : UserRepository, SafeApiRequest() {
    override suspend fun getUserByUserId(userId: String , currentUserId:String): Flow<ResponseStates<UserDTO>> {
        return flow {
            emit(ResponseStates.Loading)
            try {
//                userDao.getUserById(userId)?.let { cachedUser ->
//                    val cachedResponse = cachedUser.toDto()
//                    emit(ResponseStates.Success(cachedResponse))
//                }
                if (isNetworkAvailable(context)) {
                    val response = safeApiRequest { apiService.getUserByUserId(userId , currentUserId) }
//                    withContext(Dispatchers.IO) {
//                        userDao.insertUser(response.toEntity())
//                    }
                    emit(ResponseStates.Success(response))
                } else if (!isNetworkAvailable(context) && userDao.getUserById(userId) == null) {
                    emit(ResponseStates.Error("No internet connection and no cached data available"))
                }
            } catch (e: Exception) {
                Log.d("Exception" , "Exception")
                emit(ResponseStates.Error(e.message ?: "Unknown Error Occurred"))
            }
        }.catch { e ->
            Log.d("Exception" , "Exception")
            emit(ResponseStates.Error(e.message ?: "Error in Flow"))
        }
    }

    override suspend fun createUser(request: UserDetails): Flow<ResponseStates<UserDetails>> = flow {
        emit(ResponseStates.Loading)
        try {
            val temp = UserDetails(
                artist = true,
                id = "string1",
                name = "string1",
                profilePicture = "string1",
                dob = "2024-10-30",
                gender = "string",
                language = "string",
                countryIso2 = "string"
            )
            val response = safeApiRequest { apiService.createUser(temp) }
            emit(ResponseStates.Success(response))
        } catch (e: Exception) {
            emit(ResponseStates.Error(e.message ?: "Unknown error occurred"))
        }
    }

    override suspend fun followUser(
        userId: String,
        artistId: String
    ): Flow<ResponseStates<Unit>>  = flow  {
        emit(ResponseStates.Loading)
        try {
            val response = safeApiRequest { apiService.followUser(userId ,artistId ) }
            emit(ResponseStates.Success(response))
        } catch (e: Exception) {
            emit(ResponseStates.Error(e.message ?: "Unknown error occurred"))
        }
    }

    override suspend fun unfollowArtist(
        userId: String,
        artistId: String
    ): Flow<ResponseStates<Unit>> = flow  {
        emit(ResponseStates.Loading)
        try {
            val response = safeApiRequest { apiService.unfollowArtist(userId , artistId)}
            emit(ResponseStates.Success(response))
        } catch (e: Exception) {
            Log.d("Exception" , e.message.toString())
            emit(ResponseStates.Error(e.message ?: "Unknown error occurred"))
        }
    }

    override suspend fun likeArtwork(
        userId: String,
        artworkId: String
    ): Flow<ResponseStates<Unit>>  = flow  {
        emit(ResponseStates.Loading)
        try {
            val response = safeApiRequest { apiService.likeArtwork(artworkId , userId)}
            emit(ResponseStates.Success(response))
        } catch (e: Exception) {
            emit(ResponseStates.Error(e.message ?: "Unknown error occurred"))
        }
    }

    override suspend fun unLikeArtwork(
        userId: String,
        artworkId: String
    ): Flow<ResponseStates<Unit>> = flow  {
        emit(ResponseStates.Loading)
        try {
            val response = safeApiRequest { apiService.unLikeArtwork(artworkId , userId)}
            emit(ResponseStates.Success(response))
        } catch (e: Exception) {
            emit(ResponseStates.Error(e.message ?: "Unknown error occurred"))
        }
    }

    override suspend fun commentOnArtwork(
        userId: String,
        artworkId: String,
        comment:CommentRequest
    ): Flow<ResponseStates<Unit>>  = flow  {
        emit(ResponseStates.Loading)
        try {
            val response = safeApiRequest { apiService.commentOnArtwork(userId , artworkId , comment ) }
            emit(ResponseStates.Success(response))
        } catch (e: Exception) {
            Log.d("Exception" , e.message.toString())
            emit(ResponseStates.Error(e.message ?: "Unknown error occurred"))
        }
    }

    override suspend fun getCommentsOnArtwork(
        artworkId: String,
    ): Flow<ResponseStates<List<GetCommentsDTO>>>  = flow  {
        emit(ResponseStates.Loading)
        try {
            val response = safeApiRequest { apiService.getCommentOnArtwork( artworkId ) }
            emit(ResponseStates.Success(response))
        } catch (e: Exception) {
            Log.d("Exception" , e.message.toString())
            emit(ResponseStates.Error(e.message ?: "Unknown error occurred"))
        }
    }

    override suspend fun saveOnFavorites(
        favoritesId: String,
        artworkId: String
    ): Flow<ResponseStates<Unit>>  = flow  {
        emit(ResponseStates.Loading)
        try {
            Log.d("Exception" , favoritesId)
            Log.d("Exception" , artworkId)
            val response = safeApiRequest { apiService.saveOnFavorites(favoritesId , artworkId) }
            emit(ResponseStates.Success(response))
        } catch (e: Exception) {
            Log.d("Exception" , e.message.toString())
            emit(ResponseStates.Error(e.message ?: "Unknown error occurred"))
        }
    }

    override suspend fun getArtistArtworks( userId: String , artistId:String): Flow<ResponseStates<List<ArtworkDTO>>>  = flow  {
        emit(ResponseStates.Loading)
        try {
            val response = safeApiRequest { apiService.getArtistArtworks(userId , artistId) }
            emit(ResponseStates.Success(response))
        } catch (e: Exception) {
            emit(ResponseStates.Error(e.message ?: "Unknown error occurred"))
        }
    }

    override suspend fun getAllFavorites(userId: String): Flow<ResponseStates<List<favoritesDTO>>> = flow{
        emit(ResponseStates.Loading)
        try {
            val response = safeApiRequest { apiService.getFavoritesByUserId(userId) }
            emit(ResponseStates.Success(response))
        } catch (e: Exception) {
            emit(ResponseStates.Error(e.message ?: "Unknown error occurred"))
        }
    }

    override suspend fun createNewFavorites(
        userId: String,
        favorites: favoritesDTO
    ): Flow<ResponseStates<favoritesDTO>> = flow {
        emit(ResponseStates.Loading)
        try {
            val response = safeApiRequest { apiService.createNewFavorites(userId , favorites) }
            emit(ResponseStates.Success(response))
        } catch (e: Exception) {
            emit(ResponseStates.Error(e.message ?: "Unknown error occurred"))
        }
    }

}