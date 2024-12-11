package com.orion.templete.presentation.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orion.templete.data.model.user_model.UserDTO
import com.orion.templete.domain.repository.UserRepository
import com.orion.templete.util.ResponseStates
import com.orion.templete.util.SecureStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileScreenViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val secureStorage: SecureStorage
) : ViewModel() {

    // Initialize with Loading state instead of null
    var userData by mutableStateOf<ProfileScreenUiState>(ProfileScreenUiState.Loading)
        private set

    init {
        getUserProfile("test4")
//        secureStorage.getUserId()?.let {
//            getUserProfile(it)
//        }
    }

    private fun getUserProfile(userId: String) {
        viewModelScope.launch {
            userRepository.getUserByUserId(userId)
                .collect { response ->
                    userData = when (response) {
                        is ResponseStates.Loading -> ProfileScreenUiState.Loading
                        is ResponseStates.Success -> ProfileScreenUiState.Success(response.data)
                        is ResponseStates.Error -> ProfileScreenUiState.Error(response.error)
                    }
                }
        }
    }

    fun refreshProfile(userId: String) {
        getUserProfile(userId)
    }
}

sealed interface ProfileScreenUiState {
    object Loading : ProfileScreenUiState
    data class Success(val user: UserDTO) : ProfileScreenUiState
    data class Error(val message: String) : ProfileScreenUiState
}