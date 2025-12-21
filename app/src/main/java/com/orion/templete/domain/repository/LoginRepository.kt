package com.orion.templete.domain.repository

import android.app.Activity
import com.orion.templete.data.model.login_model.ForgetPasswordRequest
import com.orion.templete.data.model.login_model.LoginResponseDTO
import com.orion.templete.data.model.login_model.Registration
import com.orion.templete.data.model.login_model.TokenRequest
import com.orion.templete.data.model.login_model.User
import com.orion.templete.data.model.UsernameValidationResponse
import com.orion.templete.util.AuthResultState
import kotlinx.coroutines.flow.Flow

interface LoginRepository {
    suspend fun loginUserDetail(user: User): LoginResponseDTO
    suspend fun signup(user : Registration): LoginResponseDTO
    suspend fun verifyUser(token : TokenRequest):Boolean
    suspend fun forgetPassword(request: ForgetPasswordRequest): LoginResponseDTO
    fun alreadySignIn(): Flow<AuthResultState<String>>
    fun createUserWithPhone(phone:String, activity: Activity) : Flow<AuthResultState<String>>
    fun signWithCredential(otp:String): Flow<AuthResultState<String>>
    fun validateUsername(username: String): Flow<AuthResultState<UsernameValidationResponse>>
}
