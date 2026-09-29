package com.orion.templete.presentation.common

import android.net.Uri

sealed class Screens(val route: String){

    object FavoritesScreen : Screens("favorites_screen")
    object SettingsScreen : Screens("settings_screen")
    object CollectionArtworksScreen : Screens("collection_artworks_screen")
    object InAppShippingAddressScreen : Screens("in_app_shipping_address_screen")

    object ArtView: Screens("art_view")
    object Swipe : Screens("swipe_route")
    object Profile : Screens("profile_route")
    object Search : Screens("search_route")
    object ArtworkDetail : Screens("details_route")
    object UserProfile : Screens("user_profile")
    object UserEditScreen : Screens("user_edit_screen")
    object Leaderboard : Screens("leaderboard_route")
    object TopArtists : Screens("top_artists_route")
    object TopCreators : Screens("top_creators_route")
    object Login : Screens("login_route")
    object Signup : Screens("signup_route")
    object CreateAccount : Screens("create_account_route")
    object ForgetPassword : Screens("forget_password_route")
    object Home : Screens("home_route")
    object Upload :Screens("upload_route")
    object UploadImageScreen : Screens("Upload_Screen_route")
    object ArtistRegister : Screens("user_register_route")
    object EditArtist : Screens("edit_artist_route")

    object Cart : Screens("cart_route")
    object OrderSuccess : Screens("order_success_route")
    object OrderFailed : Screens("order_failed_route")
    object YourOrder : Screens("order_history_route")
    object OrderTracking : Screens("order_tracking_route")
    object ShippingAddress : Screens("shipping_address_route")

    // Chat (Instagram-style direct messages)
    object RecentChats : Screens("recent_chats_route")
    object NewMessage : Screens("new_message_route")
    object ChatThread : Screens("chat_thread_route/{peerUsername}") {
        const val ARG_PEER = "peerUsername"

        // The username is URL-encoded so any character is safe inside the path; Navigation decodes it again
        fun route(peerUsername: String) = "chat_thread_route/${Uri.encode(peerUsername)}"
    }
}
