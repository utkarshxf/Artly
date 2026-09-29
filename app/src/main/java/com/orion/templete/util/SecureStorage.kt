package com.orion.templete.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.orion.templete.data.model.user_model.RegisterArtistRequest
import com.orion.templete.data.model.user_model.UserDTO
import com.orion.templete.data.model.user_model.UserDetails
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
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

    fun userIsAnArtist(): Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            // Only emit when the specific preference changes
            if (key == PreferencesKey.UserIsArtist.key) {
                val isInPipMode = prefs.getBoolean(PreferencesKey.UserIsArtist.key, false)
                trySend(isInPipMode)
            }
        }

        // Initial emission
        val initialValue = sharedPreferences.getBoolean(PreferencesKey.UserIsArtist.key, false)
        trySend(initialValue)

        // Register listener
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)

        // Ensure cleanup when flow is cancelled
        awaitClose {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }.flowOn(Dispatchers.IO)

    fun setUserIsAnArtist(isArtist: Boolean) {
        with(sharedPreferences.edit()) {
            putBoolean(PreferencesKey.UserIsArtist.key, isArtist)
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

    // Forget the cached profile only (the login itself stays)
    fun clearUserDetails() {
        with(sharedPreferences.edit()) {
            listOf(
                PreferencesKey.UserId, PreferencesKey.UserName, PreferencesKey.UserDob, PreferencesKey.UserGender,
                PreferencesKey.UserLanguage, PreferencesKey.UserCountryIso2, PreferencesKey.UserIsArtist,
                PreferencesKey.UserProfilePicture
            ).forEach { remove(it.key) }
            apply()
        }
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

    fun isUserArtist(): Boolean {
        return sharedPreferences.getBoolean(PreferencesKey.UserIsArtist.key, false)
    }

    fun hasShownBecomeArtist(): Boolean {
        return sharedPreferences.getBoolean(PreferencesKey.HasShownBecomeArtist.key, false)
    }

    fun setHasShownBecomeArtist(value: Boolean) {
        with(sharedPreferences.edit()) {
            putBoolean(PreferencesKey.HasShownBecomeArtist.key, value)
            apply()
        }
    }

    fun getSwipeCount(): Int {
        return sharedPreferences.getInt(PreferencesKey.SwipeCount.key, 0)
    }

    fun incrementSwipeCount(): Int {
        val count = getSwipeCount() + 1
        with(sharedPreferences.edit()) {
            putInt(PreferencesKey.SwipeCount.key, count)
            apply()
        }
        return count
    }

    // MVVM: Save current artist details
    fun saveCurrentArtistDetails(artistDetails: RegisterArtistRequest) {
        val gson = Gson()
        val json = gson.toJson(artistDetails)
        with(sharedPreferences.edit()) {
            putString("current_artist_details", json)
            apply()
        }
    }

    // MVVM: Get current artist details
    fun getCurrentArtistDetails(): RegisterArtistRequest? {
        val json = sharedPreferences.getString("current_artist_details", null) ?: return null
        return try {
            val gson = Gson()
            gson.fromJson(json, RegisterArtistRequest::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // MVVM: Clear artist details
    fun clearCurrentArtistDetails() {
        with(sharedPreferences.edit()) {
            remove("current_artist_details")
            apply()
        }
    }
}
