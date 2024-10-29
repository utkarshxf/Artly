package com.orion.templete.data.repository

import android.content.Context
import android.util.Log
import com.flashcall.me.data.local.dao.UserDao
import com.orion.templete.data.model.user_model.UserDTO
import com.orion.templete.data.network.ApiService
import com.orion.templete.domain.repository.LoginRepository
import com.orion.templete.domain.repository.UserRepository
import com.orion.templete.util.ResponseStates
import com.orion.templete.util.SafeApiRequest
import com.orion.templete.util.isNetworkAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import toDto
import toEntity
import javax.inject.Inject

class UserRepositoryImplementation @Inject constructor(
    private val apiService: ApiService,
    private val userDao: UserDao,
    private val context: Context
) : UserRepository, SafeApiRequest() {
    override suspend fun getUserByUserId(userId: String): Flow<ResponseStates<UserDTO>> {
        return flow {

            emit(ResponseStates.Loading)
            try {
                userDao.getUserById(userId)?.let { cachedUser ->
                    val cachedResponse = cachedUser.toDto()
                    emit(ResponseStates.Success(cachedResponse))
                }
                val response = safeApiRequest { apiService.getUserByUserId(userId) }
                if (isNetworkAvailable(context)) {
                    val response = safeApiRequest { apiService.getUserByUserId(userId) }
                    withContext(Dispatchers.IO) {
                        userDao.insertUser(response.toEntity())
                    }
                    emit(ResponseStates.Success(response))
                } else if (!isNetworkAvailable(context) && userDao.getUserById(userId) == null) {
                    emit(ResponseStates.Error("No internet connection and no cached data available"))
                }
                emit(ResponseStates.Success(response))

            } catch (e: Exception) {
                emit(ResponseStates.Error(e.message ?: "Unknown Error Occurred"))
            }
        }.catch { e ->
            emit(ResponseStates.Error(e.message ?: "Error in Flow"))
        }
    }
}