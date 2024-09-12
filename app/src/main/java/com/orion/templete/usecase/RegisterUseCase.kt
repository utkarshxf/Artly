package com.orion.templete.usecase

import com.orion.templete.data.model.login_model.LoginResponseDTO
import com.orion.templete.data.model.login_model.TokenRequest
import com.orion.templete.data.model.login_model.User
import com.orion.templete.domain.repository.UserRepository
import com.orion.templete.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val userRepository: UserRepository
) {

    fun signin(user: User): Flow<Resource<LoginResponseDTO>> = flow {
        emit(Resource.Loading(null))
        try {
            emit(Resource.Success(userRepository.loginUserDetail(user)))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "An unknown error occurred"))
        }
    }
    fun signup(user: User): Flow<Resource<User>> = flow {
        emit(Resource.Loading(null))
        try {
            emit(Resource.Success(userRepository.signup(user)))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "An unknown error occurred"))
            }
    }

    operator fun invoke(token: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading(null))
        try {
            val response = userRepository.verifyUser(TokenRequest(token))
            emit(Resource.Success(response))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "An unknown error occurred"))
        }
    }
}
