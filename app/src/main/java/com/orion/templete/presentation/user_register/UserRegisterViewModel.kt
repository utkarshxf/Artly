package com.orion.templete.presentation.user_register

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flashcall.me.data.local.dao.UserDao
import com.orion.templete.data.model.user_model.UserDetails
import com.orion.templete.domain.repository.UserRepository
import com.orion.templete.util.ResponseStates
import com.orion.templete.util.SecureStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserRegisterScreenViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val secureStorage: SecureStorage
) : ViewModel() {

    var createUserState by mutableStateOf<UserDetailsUiState>(UserDetailsUiState(isLoading = true))
        private set
    fun createUser(
        request:UserDetails
    ) {
        viewModelScope.launch {
            userRepository.createUser(request)
                .collect { response ->
                    createUserState = when (response) {
                        is ResponseStates.Loading -> createUserState.copy(isLoading = true)
                        is ResponseStates.Success -> createUserState.copy(data = response.data)
                        is ResponseStates.Error -> createUserState.copy(error = response.error.toString())
                    }
                }
        }
    }
}

data class UserDetailsUiState(
    val isLoading: Boolean = false,
    val data: UserDetails? = null,
    val error: String? = null
)
