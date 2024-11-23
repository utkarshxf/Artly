package com.orion.templete.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SecureStorage @Inject constructor(@ApplicationContext context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = context.getSharedPreferences(
        "app_prefs", // name of preferences file
        Context.MODE_PRIVATE
    )

    fun saveToken(token: String) {
        with(sharedPreferences.edit()) {
            putString("token", token)
            apply()
        }
    }
    fun saveUserId(userID: String) {
        with(sharedPreferences.edit()) {
            putString("userID", userID)
            apply()
        }
    }

    fun getToken(): String? {
        return sharedPreferences.getString("token", null)
    }
    fun getUserId(): String? {
        return sharedPreferences.getString("userID", null)
    }
}
