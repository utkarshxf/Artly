package com.orion.templete.presentation.common

sealed class Screens(val route: String){
    object Swipe : Screens("swipe_route")
    object Profile : Screens("profile_route")
    object Search : Screens("search_route")
    object ArtworkDetail : Screens("details_route")
    object UserProfile : Screens("user_profile")
    object UserRegister : Screens("user_register")
    object Login : Screens("login_route")
    object Signup : Screens("signup_route")
    object Home : Screens("home_route")



    object Splash : Screens("splash_route")
    object ForgotPassword : Screens("forgot_password_route")
    object EditProfile : Screens("edit_profile_route")
    object Notification : Screens("notification_route")
    object Chat : Screens("chat_route")
    object Settings : Screens("settings_route")
    object Welcome : Screens("welcome_route")
    object AddArtwork : Screens("add_artwork_route")
    object AddArtworkSuccess : Screens("add_artwork_success_route")
    object AddArtworkFailed : Screens("add_artwork_failed_route")
    object EditArtwork : Screens("edit_artwork_route")

}
