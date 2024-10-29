package com.orion.templete.domain.repository

import com.orion.templete.data.model.login_model.LoginResponseDTO
import com.orion.templete.data.model.user_model.UserDTO
import com.orion.templete.util.ResponseStates
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun getUserByUserId(userId: String): Flow<ResponseStates<UserDTO>>
}