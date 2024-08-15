package com.orion.templete.presentation.auth.common

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orion.templete.data.model.User
import com.orion.templete.usecase.RegisterUseCase
import com.orion.templete.util.Resource
import com.orion.templete.util.SecureStorage
import com.orion.templete.util.LoginUiState
import com.orion.templete.util.UserCheckStateHolder
import com.orion.templete.util.SignupUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class authViewModel @Inject constructor(
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
                    is Resource.Success -> {
                        checkUser = UserCheckStateHolder(data = isValid.data, isLoading = false)
                    }

                    is Resource.Error -> {
                        checkUser = UserCheckStateHolder(error = isValid.message, isLoading = false)
                    }

                    is Resource.Loading -> {
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
                    is Resource.Error -> {
                        signupData = SignupUiState(error = it.message.toString())
                    }

                    is Resource.Loading -> {
                        signupData = SignupUiState(isLoading = true)
                    }

                    is Resource.Success -> {
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
                    is Resource.Error -> {
                        signingData = LoginUiState(error = it.message.toString())
                    }

                    is Resource.Loading -> {
                        signingData = LoginUiState(isLoading = true)
                    }

                    is Resource.Success -> {
                        signingData = LoginUiState(data = it.data)
                        it.data?.let { loginResponse ->
                            secureStorage.saveToken(loginResponse.jwtToken)
                        }
                    }
                }
            }
        }
    }
}


