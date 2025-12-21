package com.orion.templete.data.model

data class UsernameValidationResponse(
    val isValid: Boolean,
    val message: String?,
    val username: String?,
    val status: Boolean?
)

