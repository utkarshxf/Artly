package com.orion.templete.presentation.auth

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orion.templete.data.model.login_model.User
import com.orion.templete.usecase.RegisterUseCase
import com.orion.templete.util.ResponseStates
import com.orion.templete.util.SecureStorage
import com.orion.templete.util.LoginUiState
import com.orion.templete.util.UserCheckStateHolder
import com.orion.templete.util.SignupUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: RegisterUseCase,
    private val secureStorage: SecureStorage
) : ViewModel() {

    var signingData by mutableStateOf(LoginUiState())
    var signupData by mutableStateOf(SignupUiState())
    var checkUser by mutableStateOf(UserCheckStateHolder())

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
    fun signup(user: User) {
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
                            secureStorage.saveUserId(loginResponse.username)
                        }
                        signingData = LoginUiState(data = it.data)
                    }
                }
            }
        }
    }
}


