package com.orion.templete.data.model.login_model

// Firebase ID token the app gets after phone (SMS code) or Google sign-in; the backend reads the verified
// phone number / email from it
data class FirebaseAuthRequest(
    val firebaseIdToken: String
)

// registered = false: no account uses the verified phone/email yet, so the app asks for a username and password
data class FirebaseAuthResponse(
    val registered: Boolean,
    val phone: String? = null,
    val email: String? = null,
    val jwtToken: String? = null,
    val username: String? = null,
    val roles: List<String> = emptyList()
)

data class FirebaseSignupRequest(
    val firebaseIdToken: String,
    val username: String,
    val password: String
)
