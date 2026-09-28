package com.orion.templete.data.model.login_model

// Firebase ID token the app gets once the user entered the SMS code; the backend reads the phone number from it
data class PhoneAuthRequest(
    val firebaseIdToken: String
)

// registered = false: the verified number has no account yet, so the app asks for a username and password
data class PhoneAuthResponse(
    val registered: Boolean,
    val phone: String? = null,
    val jwtToken: String? = null,
    val username: String? = null,
    val roles: List<String> = emptyList()
)

data class PhoneSignupRequest(
    val firebaseIdToken: String,
    val username: String,
    val password: String
)
