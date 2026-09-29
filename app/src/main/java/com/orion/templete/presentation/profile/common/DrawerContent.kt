package com.orion.templete.presentation.profile.common

import android.widget.Toast
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.orion.templete.data.network.ApiService
import kotlinx.coroutines.withContext
import android.content.Context
import android.util.Log
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.freshchat.consumer.sdk.Freshchat
import com.orion.templete.R
import com.orion.templete.domain.repository.chat.ChatSession
import com.orion.templete.presentation.common.Screens
import com.orion.templete.util.SecureStorage
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "DrawerContent"

// Chat keeps its own Firebase session (uid == username) and this phone's push token. Both must go on logout,
// otherwise the next account used on this phone would keep receiving the previous user's messages.
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ChatSessionEntryPoint {
    fun chatSession(): ChatSession
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AccountEntryPoint {
    fun apiService(): ApiService
}

// DELETE /account; true when the server deleted the account
private suspend fun deleteMyAccount(context: Context): Boolean = withContext(Dispatchers.IO) {
    try {
        EntryPointAccessors.fromApplication(context.applicationContext, AccountEntryPoint::class.java)
            .apiService().deleteMyAccount().isSuccessful
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Account deletion failed", e)
        false
    }
}

// Outlives the screen that starts the logout, so navigating to the login screen can't cancel the chat sign-out
private val chatLogoutScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

/**
 * Signs this phone out of chat (removes its push token, stops presence, signs out of Firebase).
 * Call it BEFORE clearing SecureStorage. Waits at most [timeoutMs] so being offline (or the backend waking up)
 * never blocks logging out; the sign-out keeps running in the background after that.
 */
suspend fun signOutOfChat(context: Context, timeoutMs: Long = 3_000L) {
    val session = try {
        EntryPointAccessors.fromApplication(context.applicationContext, ChatSessionEntryPoint::class.java)
            .chatSession()
    } catch (e: Exception) {
        Log.w(TAG, "Chat session unavailable, skipping chat sign-out", e)
        return
    }
    val signOut = chatLogoutScope.launch {
        try {
            session.signOut()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Chat sign-out failed", e)
        }
    }
    withTimeoutOrNull(timeoutMs) { signOut.join() }
}

@Composable
fun DrawerContent(navController: NavController ,logOut:()->Unit ,  onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(300.dp)
            .padding(16.dp)
    ) {
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
                tint = MaterialTheme.colorScheme.onSurface,
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

        val logoutScope = rememberCoroutineScope()
        var loggingOut by remember { mutableStateOf(false) }

        // Delete account (Play policy): confirm, stop chat first (its presence heartbeat would re-create the chat
        // profile), delete on the server, then log out exactly like Logout does
        var confirmDelete by remember { mutableStateOf(false) }
        var deleting by remember { mutableStateOf(false) }
        NavigationDrawerItem(colors = drawerItemColor,
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            label = { Text("Delete account", color = MaterialTheme.colorScheme.error) },
            selected = false,
            onClick = { if (!loggingOut) confirmDelete = true })
        if (confirmDelete) {
            AlertDialog(
                onDismissRequest = { if (!deleting) confirmDelete = false },
                title = { Text("Delete your account?") },
                text = {
                    Text(
                        "This permanently deletes your account, profile, artist profile, uploaded artworks, " +
                            "comments, likes, follows, collections and chats. It can't be undone."
                    )
                },
                confirmButton = {
                    TextButton(
                        enabled = !deleting,
                        onClick = {
                            deleting = true
                            logoutScope.launch {
                                signOutOfChat(context)
                                if (deleteMyAccount(context)) {
                                    Toast.makeText(context, "Your account was deleted", Toast.LENGTH_LONG).show()
                                    Freshchat.resetUser(context)
                                    SecureStorage(context).clearSharedPref()
                                    confirmDelete = false
                                    logOut()
                                } else {
                                    // still logged in: bring chat back
                                    try {
                                        EntryPointAccessors.fromApplication(context.applicationContext, ChatSessionEntryPoint::class.java)
                                            .chatSession().start()
                                    } catch (e: Exception) {
                                        Log.w(TAG, "Couldn't restart chat", e)
                                    }
                                    deleting = false
                                    Toast.makeText(context, "Couldn't delete your account. Check your connection and try again.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    ) {
                        if (deleting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                dismissButton = {
                    TextButton(enabled = !deleting, onClick = { confirmDelete = false }) { Text("Cancel") }
                }
            )
        }
        NavigationDrawerItem(colors = drawerItemColor,
            icon = {
                if (loggingOut) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null , tint = MaterialTheme.colorScheme.error)
                }
            },
            label = { Text(if (loggingOut) "Logging out…" else "Logout") },
            selected = false,
            onClick = {
                if (!loggingOut) {
                    loggingOut = true
                    logoutScope.launch {
                        // Chat first: removing this phone's push token still needs the stored username
                        signOutOfChat(context)
                        Freshchat.resetUser(context)
                        SecureStorage(context).clearSharedPref()
                        logOut()
                    }
                }
            })
    }
}