package com.orion.templete.presentation.common

import com.orion.templete.R

data class BottomNavigationItem(
    val icon : Int = R.drawable.user,
    val route : String = ""
) {
    fun bottomNavigationItems() : List<BottomNavigationItem> {
        return listOf(
            BottomNavigationItem(
                icon = R.drawable.ic_swip,
                route = Screens.Swipe.route
            ),
            BottomNavigationItem(
                icon = R.drawable.search_24px,
                route = Screens.Search.route
            ),
            BottomNavigationItem(
                icon = R.drawable.ic_save,
                route = Screens.FavoritesScreen.route
            ),
            BottomNavigationItem(
                icon = R.drawable.ic_profile,
                route = Screens.Profile.route
            ),
        )
    }
}