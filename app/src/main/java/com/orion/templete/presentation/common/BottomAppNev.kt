package com.orion.templete.presentation.common


import UserEditScreen
import android.os.Build
import androidx.annotation.RequiresApi
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.orion.templete.data.model.user_model.UserDetails
import com.orion.templete.presentation.address.Address
import com.orion.templete.presentation.address.InAppShippingAddressScreen
import com.orion.templete.presentation.address.ShippingAddressScreen
import com.orion.templete.presentation.artist_profile.ArtistProfileScreen
import com.orion.templete.presentation.profile.ProfileScreen
import com.orion.templete.presentation.search.SearchScreen
import com.orion.templete.presentation.swipe.SwipeScreen
import com.orion.templete.presentation.artwork_detail.ArtworkDetailScreen
import com.orion.templete.presentation.cart.CartScreen
import com.orion.templete.presentation.favorites.Artwork
import com.orion.templete.presentation.favorites.Collection
import com.orion.templete.presentation.favorites.CollectionArtworksScreen
import com.orion.templete.presentation.favorites.CollectionDetail
import com.orion.templete.presentation.favorites.CollectionsScreen
import com.orion.templete.presentation.order_state.OrderConfirmationDetails
import com.orion.templete.presentation.order_state.OrderFailedScreen
import com.orion.templete.presentation.order_state.OrderSuccessScreen
import com.orion.templete.presentation.order_tracking.DeliveryStatus
import com.orion.templete.presentation.order_tracking.OrderStatus
import com.orion.templete.presentation.order_tracking.OrderTrackingScreen
import com.orion.templete.presentation.order_tracking.StatusUpdate
import com.orion.templete.presentation.orders.YourOrdersScreen
import com.orion.templete.presentation.ui.theme.ButtonHeight
import com.orion.templete.presentation.ui.theme.MediumSize
import com.orion.templete.presentation.upload.UploadScreen

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomAppNev(
    navigateToLoginScreen: () -> Unit = {} ,
) {
    val navController = rememberNavController()
    var navigationSelectedItem by remember { mutableIntStateOf(0) }

    val screens = listOf(
        Screens.Profile,
        Screens.Search,
        Screens.Swipe,
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val bottomBarDestination = screens.any { it.route == currentDestination?.route }
    val bottomPadding = if (!bottomBarDestination) 0.dp else ButtonHeight
    NavHost (
        navController = navController,
        startDestination = Screens.Swipe.route,
        modifier = Modifier.padding(bottom = bottomPadding)
    ){
        composable(Screens.Swipe.route) {
            SwipeScreen(navigateToDetailScreen = { data->
                navController.currentBackStackEntry?.savedStateHandle?.set(key = "data-mapped", value = data)
                navController.navigate(Screens.ArtworkDetail.route)
            })
        }
        composable(Screens.Profile.route) {
            ProfileScreen(navController = navController)
        }
        composable(Screens.Upload.route) {
            UploadScreen()
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
            ArtworkDetailScreen("4bfde960-4807-421e-b11b-7f5472e848ea")
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
        composable(Screens.ShippingAddress.route){
            ShippingAddressScreen(onBackClick = {}, onContinueClick = {})
        }
        composable(Screens.Cart.route){
            CartScreen({} , {})
        }
        composable(Screens.OrderSuccess.route) {
            OrderSuccessScreen(
                orderDetails = OrderConfirmationDetails(
                    orderId = "123456",
                    totalAmount = 100.0,
                    estimatedDelivery = "Tomorrow",
                    shippingAddress = Address(
                        id = "1",
                        name = "Home",
                        streetAddress = "",
                        apartment = null,
                        city = "",
                        state = "",
                        zipCode = "",
                        phone = "",
                    ),
                ),
                onViewOrderDetailsClick = {},
                onContinueShoppingClick = {},
            )
        }
        composable(Screens.OrderFailed.route) {
            OrderFailedScreen("error" , {} ,{} , {})
        }
        composable(Screens.OrderTracking.route) {
            val orderStatus = OrderStatus(
                orderId = "ORD-123456",
                currentStatus = DeliveryStatus.SHIPPED,
                estimatedDelivery = "March 20, 2025",
                trackingNumber = "1Z999AA1234567890",
                carrier = "FedEx",
                deliveryPartner = "Express Delivery",
                updates = listOf(
                    StatusUpdate(
                        status = DeliveryStatus.ORDERED,
                        timestamp = "March 15, 2025 10:30 AM",
                        location = null,
                        description = "Order placed successfully",
                        isCompleted = true
                    ),
                    StatusUpdate(
                        status = DeliveryStatus.SHIPPED,
                        timestamp = "March 16, 2025 2:45 PM",
                        location = "New York Distribution Center",
                        description = "Package has left the facility",
                        isCompleted = true
                    ), StatusUpdate(
                        status = DeliveryStatus.SHIPPED,
                        timestamp = "March 16, 2025 2:45 PM",
                        location = "New York Distribution Center",
                        description = "Package has left the facility",
                        isCompleted = true
                    )
                    // Add more updates as needed
                )
            )
            OrderTrackingScreen(
                orderStatus = orderStatus,
                onBackClick = { /* Navigate back */ },
                onContactSupportClick = { /* Open support */ }
            )
        }
        composable(Screens.YourOrder.route) {
            YourOrdersScreen(
                onBackClick = { /* Navigate back */ },
                onOrderClick = { /* Navigate to order details */ }
            )
        }
        composable(Screens.FavoritesScreen.route) {
            val sampleCollections = listOf(
                Collection("1", "Favorites", 12),
                Collection("2", "Watch Later", 5),
                Collection("3", "Reading List", 8),
                Collection("4", "Shopping List", 3),
                Collection("5", "Travel Plans", 6),
                Collection("6", "Music", 15)
            )
            CollectionsScreen(
                collections = sampleCollections,
                onBackClick = {},
                onAddClick = {},
                onCollectionClick = {
                    navController.navigate(Screens.CollectionArtworksScreen.route)
                }
            )
        }
        composable(Screens.CollectionArtworksScreen.route) {
            val sampleCollection = CollectionDetail(
                id = "1",
                name = "My Favorite Artworks",
                artworkCount = 6,
                artworks = listOf(
                    Artwork("1", "url1", "Starry Night", "Vincent van Gogh"),
                    Artwork("2", "url2", "Mona Lisa", "Leonardo da Vinci"),
                    Artwork("3", "url3", "The Scream", "Edvard Munch"),
                    Artwork("4", "url4", "Girl with a Pearl Earring", "Johannes Vermeer"),
                    Artwork("5", "url5", "The Persistence of Memory", "Salvador Dalí"),
                    Artwork("6", "url6", "The Kiss", "Gustav Klimt")
                )
            )
            CollectionArtworksScreen(
                collection = sampleCollection,
                onBackClick = {},
                onArtworkClick = {},
                onMoreClick = {}
            )
        }
        composable(Screens.InAppShippingAddressScreen.route) {
            InAppShippingAddressScreen(onBackClick = {})
        }
        composable(Screens.SettingsScreen.route) {
//            SettingsScreen()
        }
        composable(Screens.UserEditScreen.route) {
            UserEditScreen(
                userDetails = UserDetails(
                    name = "John Doe",
                    dob = "1990-01-01",
                    gender = "Male",
                    language = "English",
                    countryIso2 = "US",
                    artist = true,
                    profilePicture = null,
                    id = "1",
                ),
                onSave = { },
            )
        }
    }
    if (bottomBarDestination){
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
}