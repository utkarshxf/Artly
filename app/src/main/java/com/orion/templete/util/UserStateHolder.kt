package com.orion.templete.util

import com.orion.templete.data.model.LoginResponseDTO

data class UserStateHolder(
    val isLoading: Boolean = false,
    val data: LoginResponseDTO? = null,
    val error: String? = null
)
data class UserCheckStateHolder(
    val isLoading: Boolean = true,
    val data: Boolean? = null,
    val error: String? = null
)

