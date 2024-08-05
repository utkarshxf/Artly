package com.orion.templete.domain.repository

import com.orion.templete.data.model.LoginResponseDTO
import com.orion.templete.data.model.TokenRequest
import com.orion.templete.data.model.User

interface UserRepository {
    suspend fun loginUserDetail(user: User): LoginResponseDTO
    suspend fun verifyUser(token : TokenRequest):Boolean
}