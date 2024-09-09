package com.orion.templete.data.model

data class UserDTO(
    val artist: Boolean,
    val id: String,
    val name: String,
    val profilePicture: String,
    val dob: String,
    val gender: String,
    val language: String,
    val countryIso2: String
)
