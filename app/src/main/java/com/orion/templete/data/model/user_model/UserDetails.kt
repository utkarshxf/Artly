package com.orion.templete.data.model.user_model

data class UserDetails(
    val id: String,
    val name: String,
    val dob: String,
    val gender: String,
    val language: String,
    val countryIso2: String,
    val artist: Boolean,
    val profilePicture: String? = null
)