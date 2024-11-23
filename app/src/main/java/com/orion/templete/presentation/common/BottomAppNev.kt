package com.orion.templete.presentation.common


import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.orion.templete.data.model.artwork_model.ArtworkDetailsDTO
import com.orion.templete.presentation.artist_profile.ArtistProfileScreen
import com.orion.templete.presentation.profile.ProfileScreen
import com.orion.templete.presentation.search.SearchScreen
import com.orion.templete.presentation.swipe.SwipeScreen
import com.orion.templete.presentation.artwork_detail.ArtworkDetailContent
import com.orion.templete.presentation.artwork_detail.ArtworkDetailScreen
import com.orion.templete.presentation.ui.theme.ButtonHeight
import com.orion.templete.presentation.ui.theme.MediumSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomAppNev(
    navigateToLoginScreen: () -> Unit = {} ,
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    var navigationSelectedItem by remember { mutableStateOf(0) }
    NavHost (
        navController = navController,
        startDestination = Screens.ArtworkDetail.route,
        modifier = Modifier.padding(bottom = ButtonHeight)
    ){
        composable(Screens.Swipe.route) {
            SwipeScreen(navigateToDetailScreen = {data->
                navController.currentBackStackEntry?.savedStateHandle?.set(key = "data-mapped", value = data)
                navController.navigate(Screens.ArtworkDetail.route)
            })
        }
        composable(Screens.Profile.route) {
            ProfileScreen()
        }
//        composable(Screens.ArtworkDetail.route)
//        {
//            val artworkDetailsDTO = navController.previousBackStackEntry?.savedStateHandle?.get<ArtworkDetailsDTO>("data-mapped")
//            if (artworkDetailsDTO!=null){
//                ArtworkDetailContent(artworkDetailsDTO)
//            }
//        }
        composable(Screens.ArtworkDetail.route)
        {
            ArtworkDetailScreen("909727e8-a94c-47c4-9070-14b0d86bc19e")
        }
        composable(Screens.Search.route)
        {
            SearchScreen(){ it ->
                navController.currentBackStackEntry?.savedStateHandle?.set(key = "UserID", value = it)
                navController.navigate(Screens.UserProfile.route)
            }
        }
        composable(Screens.UserProfile.route)
        {
            ArtistProfileScreen()
            val userID = navController.previousBackStackEntry?.savedStateHandle?.get<String>("UserID")
            if (userID!=null){
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize() , contentAlignment = Alignment.BottomCenter) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = MediumSize,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.height(ButtonHeight)
        ) {
            BottomNavigationItem().bottomNavigationItems().forEachIndexed { index, navigationItem ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = {
                            navigationSelectedItem = index
                            navController.navigate(navigationItem.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                restoreState = true
                            }
                        }
                    ) {
                        Icon(
                            painterResource(id = navigationItem.icon),
                            contentDescription = null,
                            tint = if (index == navigationSelectedItem){
                                if(isSystemInDarkTheme()) Color.White else Color.Black
                            } else
                                Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}