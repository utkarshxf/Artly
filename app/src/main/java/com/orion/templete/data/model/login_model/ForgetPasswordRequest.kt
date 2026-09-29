package com.orion.templete.data.model.login_model

data class ForgetPasswordRequest(
    val phoneNumber: String,
    val newPassword: String,
    // proves the phone number to the backend
    val firebaseIdToken: String
)
