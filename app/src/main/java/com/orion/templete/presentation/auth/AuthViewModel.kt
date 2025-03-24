package com.orion.templete.presentation.auth

import android.app.Activity
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orion.templete.data.model.login_model.Registration
import com.orion.templete.data.model.login_model.User
import com.orion.templete.domain.repository.LoginRepository
import com.orion.templete.usecase.RegisterUseCase
import com.orion.templete.util.AuthResultState
import com.orion.templete.util.ResponseStates
import com.orion.templete.util.SecureStorage
import com.orion.templete.util.LoginUiState
import com.orion.templete.util.UserCheckStateHolder
import com.orion.templete.util.SignupUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: RegisterUseCase,
    private val secureStorage: SecureStorage,
    private val authRepository: LoginRepository
) : ViewModel() {

    var signingData by mutableStateOf(LoginUiState())
    var signupData by mutableStateOf(SignupUiState())
    var checkUser by mutableStateOf(UserCheckStateHolder())
    private val _authState = MutableStateFlow<AuthScreenUiState>(AuthScreenUiState.Initial)
    val authState: StateFlow<AuthScreenUiState> = _authState
    private val _otpState = MutableStateFlow<OTPScreenUiState>(OTPScreenUiState.Initial)
    val otpState: StateFlow<OTPScreenUiState> = _otpState

    fun checkSignInStatus() {
        viewModelScope.launch {
            authRepository.alreadySignIn()
                .collect { result ->

                }
        }
    }

    fun createUserWithPhone(mobile: String, activity: Activity) {
        viewModelScope.launch {
            Log.d("createUserWithPhone", "createUserWithPhone")
            authRepository.createUserWithPhone(mobile, activity)
                .collect { result ->
                    _authState.value = when (result) {
                        is AuthResultState.Loading -> AuthScreenUiState.Loading
                        is AuthResultState.Success -> AuthScreenUiState.Success(result.data)
                        is AuthResultState.Failure ->  AuthScreenUiState.Error(result.msg.message.toString())
                    }
                }
        }
    }

    fun signInWithCredential(code: String) {
        viewModelScope.launch {
            authRepository.signWithCredential(code)
                .collect { result ->
                    _otpState.value = when (result) {
                        is AuthResultState.Loading -> OTPScreenUiState.Loading
                        is AuthResultState.Success -> OTPScreenUiState.Success(result.data)
                        is AuthResultState.Failure -> OTPScreenUiState.Error(result.msg.message.toString())
                    }
                }
        }
    }

    fun isValidToken(token: String) {
        viewModelScope.launch {
            loginUseCase(token).collect { isValid ->
                when (isValid) {
                    is ResponseStates.Success -> {
                        checkUser = UserCheckStateHolder(data = isValid.data, isLoading = false)
                    }

                    is ResponseStates.Error -> {
                        checkUser = UserCheckStateHolder(error = isValid.error, isLoading = false)
                    }

                    is ResponseStates.Loading -> {
                        checkUser = UserCheckStateHolder(isLoading = true)
                    }
                }
            }
        }
    }
    fun signup(user: Registration) {
        viewModelScope.launch(Dispatchers.IO) {
            loginUseCase.signup(user).collect{
                when (it) {
                    is ResponseStates.Error -> {
                        Log.d("viewModelScope" , "Error")
                        signupData = SignupUiState(error = it.error)
                    }

                    is ResponseStates.Loading -> {
                        signupData = SignupUiState(isLoading = true)
                    }

                    is ResponseStates.Success -> {
                        Log.d("viewModelScope" , "Success")
                        it.data.let { loginResponse ->
                            secureStorage.saveToken(loginResponse.jwtToken)
                            secureStorage.saveUserId(loginResponse.username)
                        }
                        signupData = SignupUiState(data = it.data)
                    }
                }
            }
        }
    }

    fun loginUser(user: User) {
        viewModelScope.launch(Dispatchers.IO) {
            loginUseCase.signin(user).collect{
                when (it) {
                    is ResponseStates.Error -> {
                        signingData = LoginUiState(error = it.error.toString())
                    }

                    is ResponseStates.Loading -> {
                        signingData = LoginUiState(isLoading = true)
                    }

                    is ResponseStates.Success -> {
                        it.data.let { loginResponse ->
                            secureStorage.saveToken(loginResponse.jwtToken)
                            secureStorage.saveCurrentUserId(loginResponse.username)
                            secureStorage.saveUserId(loginResponse.username)
                        }
                        signingData = LoginUiState(data = it.data)
                    }
                }
            }
        }
    }
}
sealed interface AuthScreenUiState {
    object Initial : AuthScreenUiState
    object Loading : AuthScreenUiState
    data class Success(val verificationId: String) : AuthScreenUiState
    data class Error(val message: String) : AuthScreenUiState
}
sealed interface OTPScreenUiState {
    object Initial : OTPScreenUiState
    object Loading : OTPScreenUiState
    data class Success(val value: String) : OTPScreenUiState
    data class Error(val message: String) : OTPScreenUiState
}


