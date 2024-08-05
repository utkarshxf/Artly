package com.orion.templete.data.model

data class LoginResponseDTO(
    val jwtToken: String,
    val username: String
)