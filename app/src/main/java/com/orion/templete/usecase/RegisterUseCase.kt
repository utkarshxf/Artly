package com.orion.templete.usecase

import com.orion.templete.data.model.login_model.LoginResponseDTO
import com.orion.templete.data.model.login_model.TokenRequest
import com.orion.templete.data.model.login_model.User
import com.orion.templete.domain.repository.LoginRepository
import com.orion.templete.util.ResponseStates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val loginRepository: LoginRepository
) {

    fun signin(user: User): Flow<ResponseStates<LoginResponseDTO>> = flow {
        emit(ResponseStates.Loading)
        try {
            emit(ResponseStates.Success(loginRepository.loginUserDetail(user)))
        } catch (e: Exception) {
            emit(ResponseStates.Error(e.message ?: "An unknown error occurred"))
        }
    }
    fun signup(user: User): Flow<ResponseStates<User>> = flow {
        emit(ResponseStates.Loading)
        try {
            emit(ResponseStates.Success(loginRepository.signup(user)))
        } catch (e: Exception) {
            emit(ResponseStates.Error(e.message ?: "An unknown error occurred"))
            }
    }

    operator fun invoke(token: String): Flow<ResponseStates<Boolean>> = flow {
        emit(ResponseStates.Loading)
        try {
            val response = loginRepository.verifyUser(TokenRequest(token))
            emit(ResponseStates.Success(response))
        } catch (e: Exception) {
            emit(ResponseStates.Error(e.message ?: "An unknown error occurred"))
        }
    }
}
