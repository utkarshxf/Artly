package com.orion.templete.presentation.home


import ArtViewScreen
import UserEditScreen
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.orion.templete.presentation.address.Address
import com.orion.templete.presentation.address.InAppShippingAddressScreen
import com.orion.templete.presentation.address.ShippingAddressScreen
import com.orion.templete.presentation.artwork_upload.UploadImageScreen
import com.orion.templete.presentation.artist_profile.ArtistProfileScreen
import com.orion.templete.presentation.artist_register.ArtistRegisterScreen
import com.orion.templete.presentation.artist_register.EditArtistScreen
import com.orion.templete.presentation.profile.ProfileScreen
import com.orion.templete.presentation.chat.RecentChatsScreen
import com.orion.templete.presentation.chat.ChatScreen
import com.orion.templete.presentation.search.SearchScreen
import com.orion.templete.presentation.swipe.SwipeScreen
import com.orion.templete.presentation.artwork_detail.ArtworkDetailScreen
import com.orion.templete.presentation.cart.CartScreen
import com.orion.templete.presentation.common.BottomNavigationItem
import com.orion.templete.presentation.common.Screens
import com.orion.templete.presentation.favorites.CollectionArtworksScreen
import com.orion.templete.presentation.favorites.CollectionViewModel
import com.orion.templete.presentation.favorites.CollectionsScreen
import com.orion.templete.presentation.leaderboard.LeaderboardScreen
import com.orion.templete.presentation.top_artists.TopArtistsScreen
import com.orion.templete.presentation.top_creators.TopCreatorsScreen
import com.orion.templete.presentation.order_state.OrderConfirmationDetails
import com.orion.templete.presentation.order_state.OrderFailedScreen
import com.orion.templete.presentation.order_state.OrderSuccessScreen
import com.orion.templete.presentation.order_tracking.DeliveryStatus
import com.orion.templete.presentation.order_tracking.OrderStatus
import com.orion.templete.presentation.order_tracking.OrderTrackingScreen
import com.orion.templete.presentation.order_tracking.StatusUpdate
import com.orion.templete.presentation.orders.YourOrdersScreen
import com.orion.templete.presentation.profile.ProfileScreenUiState
import com.orion.templete.presentation.profile.ProfileScreenViewModel
import com.orion.templete.presentation.search.SearchScreenViewModel
import com.orion.templete.presentation.swipe.SwipeScreenViewModel
import com.orion.templete.presentation.ui.theme.ButtonHeight
import com.orion.templete.presentation.ui.theme.MediumSize
import com.orion.templete.presentation.upload.UploadScreen
import com.orion.templete.util.SecureStorage

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Home(
    navigateToLoginScreen: () -> Unit = {} ,
) {
    val appViewModel:HomeViewModel = hiltViewModel()
    val isArtist by appViewModel.IsArtist.collectAsState(initial = false)

    val navController = rememberNavController()
    var navigationSelectedItem by remember { mutableIntStateOf(0) }

    // First-time user popup state
    val context = LocalContext.current
    val secureStorage = remember { SecureStorage(context) }
    var showBecomeArtistDialog by remember { mutableStateOf(secureStorage.isFirstTime() && !isArtist) }

    val screens = listOf(
        Screens.Profile,
        Screens.Search,
        Screens.UploadImageScreen,
        Screens.FavoritesScreen,
        Screens.Swipe,
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val bottomBarDestination = screens.any { it.route == currentDestination?.route }
    val bottomPadding = if (!bottomBarDestination) 0.dp else ButtonHeight
    val collectionViewModel: CollectionViewModel = hiltViewModel()
    val swipeViewModel: SwipeScreenViewModel = hiltViewModel()
    val searchScreenViewModel: SearchScreenViewModel = hiltViewModel()
    val profileScreenViewModel = hiltViewModel<ProfileScreenViewModel>()

    // Only a rejected session means "log in again". Offline / backend waking up / 5xx must not log the
    // user out - ProfileScreen shows its own retry for those.
    val profileError = (profileScreenViewModel.userData as? ProfileScreenUiState.Error)?.message.orEmpty()
    if (listOf("error code: 401", "error code: 403", "error code: 404").any { profileError.startsWith(it) }) {
        LaunchedEffect(Unit) {
            secureStorage.clearSharedPref()
            navigateToLoginScreen()
        }
    }
    NavHost (
        navController = navController,
        startDestination = Screens.Swipe.route,
        modifier = Modifier.padding(bottom = bottomPadding)
    ){
        composable(Screens.Swipe.route) {
            SwipeScreen(swipeViewModel , navigateToDetailScreen = { data->
                navController.currentBackStackEntry?.savedStateHandle?.set(key = "artworkId", value = data.id)
                navController.navigate(Screens.ArtworkDetail.route)
            }, onChatClick = {
                navController.navigate(Screens.RecentChats.route)
            })
        }
        composable(Screens.Profile.route) {
            ProfileScreen(navController = navController , logOut = navigateToLoginScreen ,profileScreenViewModel)
        }
        composable(Screens.Upload.route) {
            UploadScreen()
        }
        composable(Screens.ArtistRegister.route) {
            ArtistRegisterScreen(onNavigateBack = {
                navController.popBackStack()
            })
        }
        composable(Screens.ArtworkDetail.route) {
            val artworkId = navController.previousBackStackEntry?.savedStateHandle?.get<String>("artworkId")
            if (artworkId != null) {
                ArtworkDetailScreen(artworkId , navController)
            }
        }
        composable(Screens.Search.route) {
            SearchScreen(navController , searchScreenViewModel)
        }
        composable(Screens.ArtView.route){
            val url = navController.previousBackStackEntry?.savedStateHandle?.get<String>("url")
            if (url != null){
                ArtViewScreen(url)
            }
        }
        composable(Screens.UserProfile.route) {
            val userID = navController.previousBackStackEntry?.savedStateHandle?.get<String>("UserID")
            if (userID != null){
                ArtistProfileScreen(userID , navController)
            }
        }
        composable(Screens.Leaderboard.route) {
            LeaderboardScreen(navController)
        }
        composable(Screens.TopArtists.route) {
            TopArtistsScreen(navController)
        }
        composable(Screens.TopCreators.route) {
            TopCreatorsScreen(navController)
        }
        composable(Screens.UploadImageScreen.route) {
            UploadImageScreen(navController)
        }
        composable(Screens.ShippingAddress.route){
            ShippingAddressScreen(onBackClick = {}, onContinueClick = {})
        }
        composable(Screens.EditArtist.route) {
            EditArtistScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(Screens.Cart.route){
            CartScreen( {} , {} )
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
            OrderFailedScreen("Error" , {} ,{} , {})
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
            CollectionsScreen(navController , collectionViewModel)
        }
        composable(Screens.CollectionArtworksScreen.route) {
            val collectionId = navController.previousBackStackEntry?.savedStateHandle?.get<String>("collectionId")
            if (collectionId != null){
                CollectionArtworksScreen(collectionId ,
                    onBackClick = { navController.popBackStack() },
                    onArtworkClick = {
                        navController.currentBackStackEntry?.savedStateHandle?.set(key = "artworkId", value = it.id)
                        navController.navigate(Screens.ArtworkDetail.route)
                    })
            }
        }
        composable(Screens.InAppShippingAddressScreen.route) {
            InAppShippingAddressScreen(onBackClick = {})
        }
        composable(Screens.SettingsScreen.route) {
//            SettingsScreen()
        }
        composable(Screens.UserEditScreen.route) {
            val userDetails = SecureStorage(LocalContext.current).getUserDetails()
            if (userDetails != null) {
                UserEditScreen(
                    userDetails = userDetails,
                   navController = navController
                )
            }
        }
        composable(Screens.RecentChats.route) {
            RecentChatsScreen(onOpenChat = { peer ->
                val encoded = android.net.Uri.encode(peer)
                navController.navigate(com.orion.templete.presentation.common.Screens.ChatThread.route(encoded))
            })
        }
        composable(com.orion.templete.presentation.common.Screens.ChatThread.route) { backStackEntry ->
            ChatScreen()
        }
    }
    if (bottomBarDestination){
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            // Main navigation bar
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = MediumSize,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.height(ButtonHeight)
            ) {
                val navigationItems = BottomNavigationItem().bottomNavigationItems()
                navigationItems.forEachIndexed { index, navigationItem ->
                    if (index != navigationItems.size / 2 || !isArtist) {
                        if(index == 2 && !isArtist){
                            // Skip the middle item if not an artist
                            return@forEachIndexed
                        }

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
                                    tint = if (navigationItem.route == currentDestination?.route) {
                                        if (isSystemInDarkTheme()) Color.White else Color.Black
                                    } else Color.Gray,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    } else {
                        // Empty space for the FAB
                       Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            if(isArtist){
                // Floating Action Button for the middle item
                val middleItemIndex = BottomNavigationItem().bottomNavigationItems().size / 2
                val middleItem = BottomNavigationItem().bottomNavigationItems()[middleItemIndex]
                FloatingActionButton(
                    onClick = {
                        navigationSelectedItem = middleItemIndex
                        navController.navigate(middleItem.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            restoreState = true
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(6.dp),
                    modifier = Modifier
                        .size(60.dp)
                        .offset(y = (-8).dp)
                ) {
                    Icon(
                        painterResource(id = middleItem.icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }

    // First-time user "Become an Artist" popup
    BecomeArtistBottomSheet(
        visible = showBecomeArtistDialog,
        onDismiss = {
            showBecomeArtistDialog = false
            secureStorage.setFirstTime(false)
        },
        onBecomeArtist = {
            showBecomeArtistDialog = false
            secureStorage.setFirstTime(false)
            navController.navigate(Screens.ArtistRegister.route)
        }
    )
}