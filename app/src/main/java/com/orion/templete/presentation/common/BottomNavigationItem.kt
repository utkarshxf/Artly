package com.orion.templete.presentation.common

import com.orion.templete.R

data class BottomNavigationItem(
    val label : String = "",
    val icon : Int = R.drawable.home,
    val route : String = ""
) {

    //function to get the list of bottomNavigationItems
    fun bottomNavigationItems() : List<BottomNavigationItem> {
        return listOf(
            BottomNavigationItem(
                label = "Home",
                icon = R.drawable.home,
                route = Screens.Swipe.route
            ),
            BottomNavigationItem(
                label = "Profile",
                icon = R.drawable.user,
                route = Screens.Profile.route
            )
        )
    }
}