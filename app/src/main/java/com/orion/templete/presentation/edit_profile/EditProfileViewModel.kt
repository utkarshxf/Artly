package com.orion.templete.presentation.edit_profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orion.templete.data.model.user_model.UserDetails
import com.orion.templete.domain.repository.UserRepository
import com.orion.templete.presentation.user_register.UserDetailsUiState
import com.orion.templete.util.ResponseStates
import com.orion.templete.util.SecureStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val secureStorage: SecureStorage
) : ViewModel() {

    var updateUserState by mutableStateOf<UserDetailsUiState>(UserDetailsUiState(isLoading = false))
        private set
    fun updateUser(
        request: UserDetails
    ) {
        viewModelScope.launch {
            userRepository.updateUser(request)
                .collect { response ->
                    updateUserState = when (response) {
                        is ResponseStates.Loading -> updateUserState.copy(isLoading = true)
                        is ResponseStates.Success -> {
                            secureStorage.saveUserDetails(response.data)
                            updateUserState.copy(data = response.data , isLoading = false)
                        }
                        is ResponseStates.Error -> updateUserState.copy(error = response.error.toString() , isLoading = false)
                    }
                }
        }
    }
}