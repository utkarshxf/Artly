package com.orion.templete.presentation.login

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orion.templete.data.model.User
import com.orion.templete.usecase.LoginUserUseCase
import com.orion.templete.util.Resource
import com.orion.templete.util.SecureStorage
import com.orion.templete.util.UserCheckStateHolder
import com.orion.templete.util.UserStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginScreenViewModel @Inject constructor(
    private val loginUseCase: LoginUserUseCase,
    private val secureStorage: SecureStorage
) : ViewModel() {

    val userData = mutableStateOf(UserStateHolder())
    val checkUser = mutableStateOf(UserCheckStateHolder())

    fun isValidToken(token: String) {
        viewModelScope.launch {
            loginUseCase(token).collect { isValid ->
                when (isValid) {
                    is Resource.Success -> {
                        checkUser.value = UserCheckStateHolder(data = isValid.data, isLoading = false)
                    }

                    is Resource.Error -> {
                        checkUser.value = UserCheckStateHolder(error = isValid.message, isLoading = false)
                    }

                    is Resource.Loading -> {
                        checkUser.value = UserCheckStateHolder(isLoading = true)
                    }
                }
            }
        }
    }

    fun loginUser(user: User) {
        viewModelScope.launch(Dispatchers.IO) {
            loginUseCase(user).collect {
                when (it) {
                    is Resource.Error -> {
                        userData.value = UserStateHolder(error = it.message.toString())
                    }

                    is Resource.Loading -> {
                        userData.value = UserStateHolder(isLoading = true)
                    }

                    is Resource.Success -> {
                        userData.value = UserStateHolder(data = it.data)
                        it.data?.let { loginResponse ->
                            secureStorage.saveToken(loginResponse.jwtToken)
                        }
                    }
                }
            }
        }
    }
}


