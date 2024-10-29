package com.orion.templete.data.repository

import com.orion.templete.data.model.login_model.LoginResponseDTO
import com.orion.templete.data.model.login_model.TokenRequest
import com.orion.templete.data.model.login_model.User
import com.orion.templete.data.network.ApiService
import com.orion.templete.domain.repository.LoginRepository
import com.orion.templete.util.SafeApiRequest
import javax.inject.Inject

class LoginRepositoryImplementation @Inject constructor(
    private val apiService: ApiService
) : LoginRepository, SafeApiRequest() {
    override suspend fun loginUserDetail(user: User): LoginResponseDTO {
        return safeApiRequest { apiService.loginUser(user) }
    }

    override suspend fun signup(user: User): User {
        return safeApiRequest { apiService.signup(user)}
    }

    override suspend fun verifyUser(token: TokenRequest): Boolean {
        return safeApiRequest { apiService.verifyUser(token)}
    }
}
