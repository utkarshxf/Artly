package com.orion.templete.util

sealed class PreferencesKey(val key: String) {
    data object UserId : PreferencesKey("user_id")
    data object UserName : PreferencesKey("user_name")
    data object UserDob : PreferencesKey("user_dob")
    data object UserGender : PreferencesKey("user_gender")
    data object UserLanguage : PreferencesKey("user_language")
    data object UserCountryIso2 : PreferencesKey("user_country_iso2")
    data object UserIsArtist : PreferencesKey("user_is_artist")
        data object UserProfilePicture : PreferencesKey("user_profile_picture")
    data object UserFirstTimeLogin : PreferencesKey("user_first_time_login")
    data object HasShownBecomeArtist : PreferencesKey("has_shown_become_artist")
    data object SwipeCount : PreferencesKey("swipe_count")

}