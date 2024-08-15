package com.orion.templete.util

import com.orion.templete.data.model.LoginResponseDTO
import com.orion.templete.data.model.User

data class LoginUiState(
    val isLoading: Boolean = false,
    val data: LoginResponseDTO? = null,
    val error: String? = null
)
data class SignupUiState(
    val isLoading: Boolean = false,
    val data: User? = null,
    val error: String? = null
)
data class UserCheckStateHolder(
    val isLoading: Boolean = true,
    val data: Boolean? = null,
    val error: String? = null
)

