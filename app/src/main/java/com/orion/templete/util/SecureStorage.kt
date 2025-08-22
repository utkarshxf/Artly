package com.orion.templete.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.orion.templete.data.model.user_model.UserDTO
import com.orion.templete.data.model.user_model.UserDetails
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
    fun saveCurrentUserId(userID: String) {
        with(sharedPreferences.edit()) {
            putString(PreferencesKey.UserId.key, userID)
            apply()
        }
    }

    fun getToken(): String? {
        return sharedPreferences.getString("token", null)
    }
    fun getUserId(): String? {
        return sharedPreferences.getString("userID", null)
    }

    fun getLayout(): Boolean {
        return sharedPreferences.getBoolean("layout", true)
    }
    fun setLayout(value: Boolean) {
        with(sharedPreferences.edit()) {
            putBoolean("layout", value)
            apply()
        }
    }

    fun saveUserDetails(userDetails: UserDetails) {
        with(sharedPreferences.edit()) {
            putString(PreferencesKey.UserId.key, userDetails.id)
            putString(PreferencesKey.UserName.key, userDetails.name)
            putString(PreferencesKey.UserDob.key, userDetails.dob)
            putString(PreferencesKey.UserGender.key, userDetails.gender)
            putString(PreferencesKey.UserLanguage.key, userDetails.language)
            putString(PreferencesKey.UserCountryIso2.key, userDetails.countryIso2)
            putBoolean(PreferencesKey.UserIsArtist.key, userDetails.artist)
            userDetails.profilePicture?.let { profilePic ->
                putString(PreferencesKey.UserProfilePicture.key, profilePic)
            }
            apply()
        }
    }
    fun saveUserDto(userDetails: UserDTO) {
        with(sharedPreferences.edit()) {
            putString(PreferencesKey.UserId.key, userDetails.id)
            putString(PreferencesKey.UserName.key, userDetails.name)
            putString(PreferencesKey.UserDob.key, userDetails.dob)
            putString(PreferencesKey.UserGender.key, userDetails.gender)
            putString(PreferencesKey.UserLanguage.key, userDetails.language)
            putString(PreferencesKey.UserCountryIso2.key, userDetails.countryIso2)
            userDetails.profilePicture?.let { profilePic ->
                putString(PreferencesKey.UserProfilePicture.key, profilePic)
            }
            apply()
        }
    }
    fun getUserDetails(): UserDetails? {
        val userId = sharedPreferences.getString(PreferencesKey.UserId.key, null) ?: return null

        return UserDetails(
            id = userId,
            name = sharedPreferences.getString(PreferencesKey.UserName.key, "") ?: "",
            dob = sharedPreferences.getString(PreferencesKey.UserDob.key, "") ?: "",
            gender = sharedPreferences.getString(PreferencesKey.UserGender.key, "") ?: "",
            language = sharedPreferences.getString(PreferencesKey.UserLanguage.key, "") ?: "",
            countryIso2 = sharedPreferences.getString(PreferencesKey.UserCountryIso2.key, "") ?: "",
            artist = sharedPreferences.getBoolean(PreferencesKey.UserIsArtist.key, false),
            profilePicture = sharedPreferences.getString(PreferencesKey.UserProfilePicture.key, null)
        )
    }
    fun clearSharedPref(){
        sharedPreferences.edit().clear().apply()
    }

    fun isFirstTime(): Boolean {
        return sharedPreferences.getBoolean(PreferencesKey.UserFirstTimeLogin.key, true)
    }
    fun setFirstTime(value: Boolean) {
        with(sharedPreferences.edit()) {
            putBoolean(PreferencesKey.UserFirstTimeLogin.key, value)
            apply()
        }
    }
}
