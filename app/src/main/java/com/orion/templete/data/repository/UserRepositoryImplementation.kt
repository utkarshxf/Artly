package com.orion.templete.data.repository

import com.orion.templete.data.model.LoginResponseDTO
import com.orion.templete.data.model.TokenRequest
import com.orion.templete.data.model.User
import com.orion.templete.data.network.ApiService
import com.orion.templete.domain.repository.UserRepository
import com.orion.templete.util.SafeApiRequest
import javax.inject.Inject

class UserRepositoryImplementation @Inject constructor(
    private val apiService: ApiService
) : UserRepository, SafeApiRequest() {
    override suspend fun loginUserDetail(user: User): LoginResponseDTO {
        return safeApiRequest { apiService.loginUser(user) }
    }

    override suspend fun signup(user: User): User {
        return safeApiRequest { apiService.signup(user) }
    }

    override suspend fun verifyUser(token: TokenRequest): Boolean {
        return safeApiRequest { apiService.verifyUser(token)}
    }
}
