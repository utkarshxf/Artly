package com.orion.templete.domain.repository

import com.orion.templete.data.model.login_model.LoginResponseDTO
import com.orion.templete.data.model.login_model.TokenRequest
import com.orion.templete.data.model.login_model.User

interface LoginRepository {
    suspend fun loginUserDetail(user: User): LoginResponseDTO
    suspend fun signup(user : User): User
    suspend fun verifyUser(token : TokenRequest):Boolean
}