package com.orion.templete.data.model.user_model


data class UserDTO(
    val id: String,
    val name: String,
    val profilePicture: String,
    val dob: String,
    val gender: String,
    val language: String,
    val artist: Boolean,
    val countryIso2: String,
    val follow:Boolean
)
