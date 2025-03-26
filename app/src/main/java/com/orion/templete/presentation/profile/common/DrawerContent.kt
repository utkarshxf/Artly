package com.orion.templete.presentation.profile.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.freshchat.consumer.sdk.Freshchat
import com.orion.templete.R
import com.orion.templete.presentation.common.Screens
import com.orion.templete.util.SecureStorage

@Composable
fun DrawerContent(navController: NavController ,logOut:()->Unit ,  onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(300.dp)
            .padding(16.dp)
    ) {
        val dividerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        val context = LocalContext.current
        val drawerItemColor = NavigationDrawerItemDefaults.colors(
            unselectedBadgeColor = Color.Transparent,
            selectedBadgeColor = Color.Transparent,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            selectedContainerColor = Color.Transparent,
            unselectedContainerColor = Color.Transparent
        )
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_logo_no_bacground),
                contentDescription = "app_icon",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(8.dp)
                    .size(40.dp)
            )
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close, contentDescription = "Close menu"
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // Menu items



        NavigationDrawerItem(colors = drawerItemColor,icon = { Icon(painterResource(id =R.drawable.ic_save), contentDescription = null) },
            label = { Text("Favorites") },
            selected = false,
            onClick = {
                navController.navigate(Screens.FavoritesScreen.route){
                    popUpTo(0){
                        inclusive = true
                    }
                }
            })
//        NavigationDrawerItem(colors = drawerItemColor,icon = { Icon(painterResource(id =R.drawable.ic_add), contentDescription = null) },
//            label = { Text("Custom Print") },
//            selected = false,
//            onClick = {
//                navController.navigate(Screens.Upload.route)
//            })

//        NavigationDrawerItem(
//            colors = drawerItemColor,
//            icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
//            label = { Text("Cart") },
//            selected = false,
//            onClick = { navController.navigate(Screens.Cart.route) }
//        )

        // Your Orders
//        NavigationDrawerItem(
//            colors = drawerItemColor,
//            icon = { Icon(Icons.Default.List, contentDescription = null) },
//            label = { Text("Your Orders") },
//            selected = false,
//            onClick = { navController.navigate(Screens.YourOrder.route) }
//        )

        // Shipping Address
//        NavigationDrawerItem(
//            colors = drawerItemColor,
//            icon = { Icon(Icons.Default.Home, contentDescription = null) },
//            label = { Text("Shipping Address") },
//            selected = false,
//            onClick = { navController.navigate(Screens.InAppShippingAddressScreen.route) }
//        )

//        NavigationDrawerItem(colors = drawerItemColor,icon = { Icon(Icons.Default.Settings, contentDescription = null) },
//            label = { Text("Settings") },
//            selected = false,
//            onClick = { navController.navigate(Screens.SettingsScreen.route)})

        NavigationDrawerItem(colors = drawerItemColor,icon = { Icon(
            Icons.Default.Face,
            contentDescription = "" )},

            label = { Text("Support") },
            selected = false,
            onClick = {
                Freshchat.showConversations(context)
            })

        // Add logout at bottom
        Spacer(modifier = Modifier.weight(1f))

        NavigationDrawerItem(colors = drawerItemColor,
            icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null , tint = MaterialTheme.colorScheme.error) },
            label = { Text("Logout") },
            selected = false,
            onClick = {
                Freshchat.resetUser(context)
                SecureStorage(context).clearSharedPref()
                logOut()
            })
    }
}