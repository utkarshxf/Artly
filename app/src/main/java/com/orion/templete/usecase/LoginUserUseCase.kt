package com.orion.templete.usecase

import com.orion.templete.data.model.LoginResponseDTO
import com.orion.templete.data.model.TokenRequest
import com.orion.templete.data.model.User
import com.orion.templete.domain.repository.UserRepository
import com.orion.templete.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class LoginUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    operator fun invoke(user: User): Flow<Resource<LoginResponseDTO>> = flow {
        emit(Resource.Loading(null))
        try {
            emit(Resource.Success(userRepository.loginUserDetail(user)))
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
