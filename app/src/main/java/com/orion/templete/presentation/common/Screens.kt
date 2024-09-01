package com.orion.templete.presentation.common

sealed class Screens(val route: String){
    object Swipe : Screens("swipe_route")
    object Profile : Screens("profile_route")
    object Search : Screens("search_route")
    object ArtworkDetail : Screens("details_route")
}
