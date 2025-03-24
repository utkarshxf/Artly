package com.orion.templete.presentation.common

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
    object UserRegister : Screens("user_register")
    object Login : Screens("login_route")
    object Signup : Screens("signup_route")
    object Home : Screens("home_route")
    object Upload :Screens("upload_route")

    object Cart : Screens("cart_route")
    object OrderSuccess : Screens("order_success_route")
    object OrderFailed : Screens("order_failed_route")
    object YourOrder : Screens("order_history_route")
    object OrderTracking : Screens("order_tracking_route")
    object ShippingAddress : Screens("shipping_address_route")
}
