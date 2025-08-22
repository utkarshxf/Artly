package com.orion.templete.data.model.login_model

data class LoginResponseDTO(
    val jwtToken: String,
    val username: String,
    val roles: List<String> = emptyList()
)
