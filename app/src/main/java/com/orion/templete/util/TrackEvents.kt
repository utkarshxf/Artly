package com.orion.templete.util

import android.content.Context
import android.os.Bundle
import com.facebook.appevents.AppEventsLogger
import com.mixpanel.android.mpmetrics.MixpanelAPI
import com.orion.templete.R
import com.orion.templete.data.model.user_model.UserDetails
import com.orion.templete.presentation.artist_register.RegisterArtistUiState
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject

class TrackEvents @Inject constructor(@ApplicationContext private val context: Context) {

    private val mp: MixpanelAPI =
        MixpanelAPI.getInstance(context, context.getString(R.string.MIXPANEL_TOKEN), false)

    private val logger: AppEventsLogger = AppEventsLogger.newLogger(context)

    private val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    private val secureStorage = SecureStorage(context)

    /**
     * Identifies a user in Mixpanel and sets their properties
     */
    fun identifyUser(userDetails: UserDetails) {
        // Identify the user in Mixpanel
        mp.identify(userDetails.id)

        // Set user properties
        val userProps = JSONObject().apply {
            put("name", userDetails.name)
            put("dob", userDetails.dob)
            put("gender", userDetails.gender)
            put("language", userDetails.language)
            put("country", userDetails.countryIso2)
            put("is_artist", userDetails.artist)
            userDetails.profilePicture?.let { 
                put("avatar", it)
            }
            put("created", System.currentTimeMillis().toString())
        }

        // Update the user's properties in Mixpanel
        mp.people.set(userProps)
    }

    // User Authentication Events
    fun trackLoginImpression() {
        val obj = JSONObject().apply {
            put("Platform", "Android")
        }
        mp.track("Login_Impression", obj)
    }

    fun trackLoginAttempted() {
        val obj = JSONObject().apply {
            put("Platform", "Android")
        }
        mp.track("Login_Attempted", obj)

        // Facebook event tracking
        logger.logEvent("fb_mobile_login_start")
    }

    fun trackLoginSuccess() {
        val userId = sharedPreferences.getString(PreferencesKey.UserId.key, null)
        val obj = JSONObject().apply {
            put("Platform", "Android")
            put("User_ID", userId)
            put("First_Seen", sharedPreferences.getString("createdAt", null))
        }
        mp.track("Login_Success", obj)

        // Facebook event tracking
        logger.logEvent("fb_mobile_complete_registration")

        // Identify user in Mixpanel
        secureStorage.getUserDetails()?.let { userDetails ->
            identifyUser(userDetails)
        }
    }

    fun trackSignupImpression() {
        val obj = JSONObject().apply {
            put("Platform", "Android")
        }
        mp.track("Signup_Impression", obj)
    }

    fun trackSignupAttempted() {
        val obj = JSONObject().apply {
            put("Platform", "Android")
        }
        mp.track("Signup_Attempted", obj)
    }

    fun trackSignupSuccess() {
        val userId = sharedPreferences.getString(PreferencesKey.UserId.key, null)
        val obj = JSONObject().apply {
            put("Platform", "Android")
            put("User_ID", userId)
            put("First_Seen", System.currentTimeMillis().toString())
        }
        mp.track("Signup_Success", obj)

        // Identify user in Mixpanel
        secureStorage.getUserDetails()?.let { userDetails ->
            identifyUser(userDetails)
        }
    }

    // User Profile Events
    fun trackProfileViewed() {
        val userId = sharedPreferences.getString(PreferencesKey.UserId.key, null)
        val obj = JSONObject().apply {
            put("Platform", "Android")
            put("User_ID", userId)
        }
        mp.track("Profile_Viewed", obj)
    }

    fun trackProfileEdited(s: String) {
        val userId = sharedPreferences.getString(PreferencesKey.UserId.key, null)
        val obj = JSONObject().apply {
            put("Platform", "Android")
            put("Status" , s)
            put("User_ID", userId)
        }
        mp.track("Profile_Edited", obj)

        // Update user properties in Mixpanel
        secureStorage.getUserDetails()?.let { userDetails ->
            identifyUser(userDetails)
        }
    }

    fun trackProfilePictureUpdated() {
        val userId = sharedPreferences.getString(PreferencesKey.UserId.key, null)
        val obj = JSONObject().apply {
            put("Platform", "Android")
            put("User_ID", userId)
        }
        mp.track("Profile_Picture_Updated", obj)

        // Update user properties in Mixpanel
        secureStorage.getUserDetails()?.let { userDetails ->
            identifyUser(userDetails)
        }
    }

    // Artwork Interaction Events
    fun trackArtworkViewed(artworkId: String) {
        val userId = sharedPreferences.getString(PreferencesKey.UserId.key, null)
        val obj = JSONObject().apply {
            put("Platform", "Android")
            put("User_ID", userId)
            put("Artwork_ID", artworkId)
        }
        mp.track("Artwork_Viewed", obj)
    }

    fun trackArtworkLiked(artworkId: String) {
        val userId = sharedPreferences.getString(PreferencesKey.UserId.key, null)
        val obj = JSONObject().apply {
            put("Platform", "Android")
            put("User_ID", userId)
            put("Artwork_ID", artworkId)
        }
        mp.track("Artwork_Liked", obj)
    }

    fun trackArtworkUnliked(artworkId: String) {
        val userId = sharedPreferences.getString(PreferencesKey.UserId.key, null)
        val obj = JSONObject().apply {
            put("Platform", "Android")
            put("User_ID", userId)
            put("Artwork_ID", artworkId)
        }
        mp.track("Artwork_Unliked", obj)
    }

    fun trackArtworkDisliked(artworkId: String) {
        val userId = sharedPreferences.getString(PreferencesKey.UserId.key, null)
        val obj = JSONObject().apply {
            put("Platform", "Android")
            put("User_ID", userId)
            put("Artwork_ID", artworkId)
        }
        mp.track("Artwork_Disliked", obj)
    }

    fun trackArtworkSavedToFavorites(artworkId: String) {
        val userId = sharedPreferences.getString(PreferencesKey.UserId.key, null)
        val obj = JSONObject().apply {
            put("Platform", "Android")
            put("User_ID", userId)
            put("Artwork_ID", artworkId)
        }
        mp.track("Artwork_Saved_To_Favorites", obj)
    }

    fun trackArtworkCommented(artworkId: String) {
        val userId = sharedPreferences.getString(PreferencesKey.UserId.key, null)
        val obj = JSONObject().apply {
            put("Platform", "Android")
            put("User_ID", userId)
            put("Artwork_ID", artworkId)
        }
        mp.track("Artwork_Commented", obj)
    }

    // Artist Interaction Events
    fun trackArtistProfileViewed(artistId: String) {
        val userId = sharedPreferences.getString(PreferencesKey.UserId.key, null)
        val obj = JSONObject().apply {
            put("Platform", "Android")
            put("User_ID", userId)
            put("Artist_ID", artistId)
        }
        mp.track("Artist_Profile_Viewed", obj)
    }

    fun trackArtistFollowed(artistId: String) {
        val userId = sharedPreferences.getString(PreferencesKey.UserId.key, null)
        val obj = JSONObject().apply {
            put("Platform", "Android")
            put("User_ID", userId)
            put("Artist_ID", artistId)
        }
        mp.track("Artist_Followed", obj)
    }

    fun trackArtistUnfollowed(artistId: String) {
        val userId = sharedPreferences.getString(PreferencesKey.UserId.key, null)
        val obj = JSONObject().apply {
            put("Platform", "Android")
            put("User_ID", userId)
            put("Artist_ID", artistId)
        }
        mp.track("Artist_Unfollowed", obj)
    }

    // Search Events
    fun trackArtistSearched(query: String) {
        val userId = sharedPreferences.getString(PreferencesKey.UserId.key, null)
        val obj = JSONObject().apply {
            put("Platform", "Android")
            put("User_ID", userId)
            put("Search_Artist_Name", query)
        }
        mp.track("Artist_Searched", obj)
    }

    // Recommendation Events
    fun trackRecommendedArtworkViewed(artworkId: String, source: String) {
        val userId = sharedPreferences.getString(PreferencesKey.UserId.key, null)
        val obj = JSONObject().apply {
            put("Platform", "Android")
            put("User_ID", userId)
            put("Artwork_ID", artworkId)
            put("Source", source) // "Today", "Popular", "New Arrivals", etc.
        }
        mp.track("Recommended_Artwork_Viewed", obj)
    }

    fun trackSimilarArtworkViewed(originalArtworkId: String, similarArtworkId: String) {
        val userId = sharedPreferences.getString(PreferencesKey.UserId.key, null)
        val obj = JSONObject().apply {
            put("Platform", "Android")
            put("User_ID", userId)
            put("Original_Artwork_ID", originalArtworkId)
            put("Similar_Artwork_ID", similarArtworkId)
        }
        mp.track("Similar_Artwork_Viewed", obj)
    }

    // Navigation Events
    fun trackScreenViewed(screenName: String) {
        val userId = sharedPreferences.getString(PreferencesKey.UserId.key, null)
        val obj = JSONObject().apply {
            put("Platform", "Android")
            put("User_ID", userId)
            put("Screen_Name", screenName)
        }
        mp.track("Screen_Viewed", obj)
    }

    // Favorites Events
    fun trackFavoritesViewed(favoriteId: String) {
        val userId = sharedPreferences.getString(PreferencesKey.UserId.key, null)
        val obj = JSONObject().apply {
            put("Platform", "Android")
            put("favorite_ID" , favoriteId)
            put("User_ID", userId)
        }
        mp.track("Favorites_Viewed", obj)
    }

    fun trackFavoriteCreated(favoriteName: String) {
        val userId = sharedPreferences.getString(PreferencesKey.UserId.key, null)
        val obj = JSONObject().apply {
            put("Platform", "Android")
            put("User_ID", userId)
            put("Favorite_Name", favoriteName)
        }
        mp.track("Favorite_Created", obj)
    }

    // AI Image Generation Events
    fun trackImageGenerationInitiated(hasSourceImage: Boolean) {
        val userId = sharedPreferences.getString(PreferencesKey.UserId.key, null)
        val obj = JSONObject().apply {
            put("Platform", "Android")
            put("User_ID", userId)
            put("Has_Source_Image", hasSourceImage)
        }
        mp.track("Image_Generation_Initiated", obj)
    }

    fun trackImageGenerationCompleted(prompt: String, steps: Int) {
        val userId = sharedPreferences.getString(PreferencesKey.UserId.key, null)
        val obj = JSONObject().apply {
            put("Platform", "Android")
            put("User_ID", userId)
            put("Prompt_Length", prompt.length)
            put("Steps", steps)
        }
        mp.track("Image_Generation_Completed", obj)
    }

    // First Time User Experience
    fun trackFirstTimeUser() {
        val userId = sharedPreferences.getString(PreferencesKey.UserId.key, null)
        val obj = JSONObject().apply {
            put("Platform", "Android")
            put("User_ID", userId)
            put("First_Seen", System.currentTimeMillis().toString())
        }
        mp.track("First_Time_User", obj)
    }

    fun trackOnboardingCompleted() {
        val userId = sharedPreferences.getString(PreferencesKey.UserId.key, null)
        val obj = JSONObject().apply {
            put("Platform", "Android")
            put("User_ID", userId)
        }
        mp.track("Onboarding_Completed", obj)
    }

    // App Session Events
    fun trackAppOpened() {
        val userId = sharedPreferences.getString(PreferencesKey.UserId.key, null)
        val obj = JSONObject().apply {
            put("Platform", "Android")
            put("User_ID", userId)
        }
        mp.track("App_Opened", obj)

        // Identify user in Mixpanel if they are logged in
        secureStorage.getUserDetails()?.let { userDetails ->
            identifyUser(userDetails)
        }
    }

    fun trackAppClosed() {
        val userId = sharedPreferences.getString(PreferencesKey.UserId.key, null)
        val obj = JSONObject().apply {
            put("Platform", "Android")
            put("User_ID", userId)
        }
        mp.track("App_Closed", obj)
    }
    fun trackPhoneLoginAttempted(phoneNumber: String) {
        val obj = JSONObject().apply {
            put("Platform", "Android")
            // For privacy, only include a masked version of the phone number
            put("Phone_Masked", maskPhoneNumber(phoneNumber))
        }
        mp.track("Phone_Login_Attempted", obj)
    }

    fun trackPhoneLoginOTPSent(phoneNumber: String) {
        val obj = JSONObject().apply {
            put("Platform", "Android")
            put("Phone_Masked", maskPhoneNumber(phoneNumber))
        }
        mp.track("Phone_Login_OTP_Sent", obj)
    }

    fun trackPhoneLoginOTPError(phoneNumber: String, errorMessage: String) {
        val obj = JSONObject().apply {
            put("Platform", "Android")
            put("Phone_Masked", maskPhoneNumber(phoneNumber))
            put("Error_Message", errorMessage)
        }
        mp.track("Phone_Login_OTP_Error", obj)
    }

    fun trackPhoneLoginOTPSubmitted() {
        val obj = JSONObject().apply {
            put("Platform", "Android")
        }
        mp.track("Phone_Login_OTP_Submitted", obj)
    }

    fun trackPhoneLoginSuccess() {
        val userId = sharedPreferences.getString(PreferencesKey.UserId.key, null)
        val obj = JSONObject().apply {
            put("Platform", "Android")
            put("User_ID", userId)
        }
        mp.track("Phone_Login_Success", obj)
    }

    fun trackPhoneLoginOTPVerificationError(errorMessage: String) {
        val obj = JSONObject().apply {
            put("Platform", "Android")
            put("Error_Message", errorMessage)
        }
        mp.track("Phone_Login_OTP_Verification_Error", obj)
    }

    // Token validation events
    fun trackTokenValidationAttempt() {
        val obj = JSONObject().apply {
            put("Platform", "Android")
        }
        mp.track("Token_Validation_Attempt", obj)
    }

    fun trackTokenValidationSuccess() {
        val userId = sharedPreferences.getString(PreferencesKey.UserId.key, null)
        val obj = JSONObject().apply {
            put("Platform", "Android")
            put("User_ID", userId)
        }
        mp.track("Token_Validation_Success", obj)
    }

    fun trackTokenValidationError(errorMessage: String) {
        val obj = JSONObject().apply {
            put("Platform", "Android")
            put("Error_Message", errorMessage)
        }
        mp.track("Token_Validation_Error", obj)
    }

    fun trackSignupError(errorMessage: String) {
        val obj = JSONObject().apply {
            put("Platform", "Android")
            put("Error_Message", errorMessage)
        }
        mp.track("Signup_Error", obj)
    }

    // Helper function to mask phone number for privacy
    private fun maskPhoneNumber(phoneNumber: String): String {
        if (phoneNumber.length <= 4) return "****"
        return phoneNumber.takeLast(4) + "****"
    }

    fun trackLoginError(toString: String) {
        val obj = JSONObject().apply {
            put("Platform", "Android")
            put("Error_Message", toString)
        }
        mp.track("Login_Error", obj)

        // Facebook event tracking
        val params = Bundle()
        params.putString("error_message", toString)
        logger.logEvent("fb_mobile_login_error", params)
    }

    fun tackUserBecomeArtist(userId: String) {
        val obj = JSONObject().apply {
            put("Platform", "Android")
            put("User_ID", userId)
        }
        mp.track("User_Become_Artist", obj)
    }

    fun trackUserProfileUpdated(userId: String) {}
}
