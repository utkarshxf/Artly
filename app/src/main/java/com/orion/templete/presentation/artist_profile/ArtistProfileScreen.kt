package com.orion.templete.presentation.artist_profile

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Icon
import com.orion.templete.data.model.chat.ProfileRef
import com.orion.templete.presentation.chat.share.PaperPlaneIcon
import com.orion.templete.presentation.chat.share.SharePayload
import com.orion.templete.presentation.chat.share.ShareToChatSheet
import com.orion.templete.presentation.chat.share.shareLinkText
import com.orion.templete.presentation.chat.share.shareTextExternally
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.orion.templete.R
import com.orion.templete.data.model.artist_model.ArtistDTO
import com.orion.templete.data.model.artwork_model.ArtworkDTO
import com.orion.templete.data.model.user_model.UserDTO
import com.orion.templete.presentation.chat.components.ChatAvatar
import com.orion.templete.presentation.common.ArtworkItem
import com.orion.templete.presentation.common.ErrorScreen
import com.orion.templete.presentation.common.ProfileHeader
import com.orion.templete.presentation.common.Screens
import com.orion.templete.presentation.components.GenreSection
import com.orion.templete.util.ImageWithText

@Composable
fun ArtistProfileScreen(
    artistId: String,
    navController: NavController,
    viewModel: ArtistProfileViewModel = hiltViewModel()
) {
    LaunchedEffect(key1 = Unit) {
        viewModel.getUserProfile(artistId)
    }

    when (val uiState = viewModel.artistProfileScreenUiState) {
        is ArtistProfileScreenUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is ArtistProfileScreenUiState.Success -> {
            ProfileContent(
                artist = uiState.artist,
                navController = navController,
                viewModel = viewModel
            )
        }
        is ArtistProfileScreenUiState.Account -> {
            AccountProfileContent(
                username = uiState.username,
                user = uiState.user,
                messageUsername = viewModel.messageUsername,
                onMessage = { navController.openChatWith(it) }
            )
        }
        is ArtistProfileScreenUiState.Error -> {
            ErrorScreen(
                message = uiState.message,
                onRetry = { viewModel.refreshProfile(artistId) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProfileContent(
    artist: ArtistDTO,
    navController: NavController,
    viewModel: ArtistProfileViewModel
) {
    LaunchedEffect(key1 = Unit) {
        viewModel.getArtistArtworks(artistId = artist.id)
        // MVVM: Fetch artist statistics
        viewModel.getArtistStats(artist.id)
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showImagePopup by remember { mutableStateOf(false) }
    var showShareSheet by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val messageUsername = viewModel.messageUsername
    // Instagram "share profile": a profile card in a chat
    val shareProfile = remember(artist, messageUsername) {
        SharePayload.Profile(
            ProfileRef(
                id = artist.id,
                name = artist.name,
                avatar = artist.image_url,
                subtitle = messageUsername?.let { "@$it" } ?: "Artist",
            )
        )
    }
    if (showShareSheet) {
        ShareToChatSheet(
            payload = shareProfile,
            onDismiss = { showShareSheet = false },
            onShareExternally = { shareTextExternally(context, shareProfile.shareLinkText()) }
        )
    }
    val artWorksUiState = viewModel.artWorksUiState
    val artistStatsUiState = viewModel.artistStatsUiState

    // Single scroll state for entire screen
    val lazyListState = rememberLazyListState()

    // Calculate collapse progress based on scroll
    val collapseProgress by remember {
        derivedStateOf {
            if (lazyListState.firstVisibleItemIndex > 0) {
                1f
            } else {
                val scrollOffset = lazyListState.firstVisibleItemScrollOffset.toFloat()
                val maxOffset = 400f // Adjust based on your header height
                (scrollOffset / maxOffset).coerceIn(0f, 1f)
            }
        }
    }

    if (showImagePopup && artist.image_url != null) {

        ImagePopup(imageUrl = artist.image_url) {
            showImagePopup = false
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        // Header Section (collapsible)
        item(key = "header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = 1f - (collapseProgress * 0.3f)
                        translationY = -collapseProgress * 30f
                    }
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                ProfileHeader(artist) {
                    showImagePopup = true
                }
                Spacer(modifier = Modifier.height(12.dp))
                // MVVM: Display artist statistics
                ArtistStatsSection(artistStatsUiState)
                Spacer(modifier = Modifier.height(12.dp))
                ProfileDescriptionSection(artist = artist)
                Spacer(modifier = Modifier.height(12.dp))
                ButtonSection(
                    artistId = artist.id,
                    initialFollowState = artist.follow,
                    viewModel = viewModel,
                    messageUsername = messageUsername,
                    onMessage = { navController.openChatWith(it) },
                    onShare = { showShareSheet = true }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Tabs Section (sticky)
        stickyHeader(key = "tabs") {
            ArtworkTabs(
                selectedTabIndex = selectedTabIndex,
                onTabSelected = { selectedTabIndex = it }
            )
        }

        // Content Section
        when (artWorksUiState) {
            is ArtWorksUiState.Loading -> {
                item(key = "loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            is ArtWorksUiState.Success -> {
                val artworks = artWorksUiState.artworks
                if (artworks.isEmpty()) {
                    item(key = "empty") {
                        EmptyArtworksState()
                    }
                } else {
                    when (selectedTabIndex) {
                        0 -> artworkGridItems(artworks, navController)
                        1 -> artworkListItems(artworks, navController)
                    }
                }
            }
            is ArtWorksUiState.Error -> {
                item(key = "error") {
                    ErrorScreen(
                        message = artWorksUiState.message,
                        onRetry = { viewModel.refreshProfile(artist.id) }
                    )
                }
            }
        }
    }
}

// Extension function for grid items in LazyColumn
private fun LazyListScope.artworkGridItems(
    artworks: List<ArtworkDTO>,
    navController: NavController
) {
    // Group artworks into rows of 2
    val rows = artworks.chunked(2)

    items(
        count = rows.size,
        key = { index -> rows[index].firstOrNull()?.id ?: "grid_row_$index" }
    ) { index ->
        val row = rows[index]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            row.forEach { artwork ->
                Box(modifier = Modifier.weight(1f)) {
                    ArtworkItem(
                        artwork = artwork,
                        showArtistName = false,
                        onArtworkClick = {
                            artwork.id?.let { id ->
                                navController.currentBackStackEntry?.savedStateHandle?.set(
                                    key = "artworkId",
                                    value = id
                                )
                                navController.navigate(Screens.ArtworkDetail.route)
                            }
                        }
                    )
                }
            }
            // Fill empty space if odd number of items
            if (row.size < 2) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

// Extension function for list items in LazyColumn
private fun LazyListScope.artworkListItems(
    artworks: List<ArtworkDTO>,
    navController: NavController
) {
    items(
        count = artworks.size,
        key = { index -> artworks[index].id ?: "list_item_$index" }
    ) { index ->
        val artwork = artworks[index]
        ArtworkListItem(
            artwork = artwork,
            onClick = {
                artwork.id?.let { id ->
                    navController.currentBackStackEntry?.savedStateHandle?.set(
                        key = "artworkId",
                        value = id
                    )
                    navController.navigate(Screens.ArtworkDetail.route)
                }
            }
        )
    }
}

@Composable
private fun ArtworkTabs(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    TabRow(
        selectedTabIndex = selectedTabIndex,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                color = MaterialTheme.colorScheme.primary
            )
        }
    ) {
        Tab(
            selected = selectedTabIndex == 0,
            onClick = { onTabSelected(0) },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_gird),
                    contentDescription = "Grid View",
                    modifier = Modifier.size(24.dp)
                )
            }
        )
        Tab(
            selected = selectedTabIndex == 1,
            onClick = { onTabSelected(1) },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_list),
                    contentDescription = "List View",
                    modifier = Modifier.size(24.dp)
                )
            }
        )
    }
}

@Composable
private fun ArtworkListItem(
    artwork: ArtworkDTO,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(artwork.imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = artwork.title,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(id = R.drawable.placeholder),
            error = painterResource(id = R.drawable.placeholder)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = artwork.title ?: "Untitled",
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = artwork.medium ?: artwork.artist ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmptyArtworksState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "🎨",
                style = MaterialTheme.typography.displayMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No artworks yet",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "This artist hasn't posted any artworks yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun ImagePopup(imageUrl: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Artist Profile Image",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun ProfileDescriptionSection(artist: ArtistDTO) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        GenreSection(artist.art_movement)

        // Only show description if it's not null or empty
        if (!artist.description.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = artist.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Wikipedia page for historical artists, the website of Artistry artists
        if (!artist.wikipedia_url.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = artist.wikipedia_url,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable {
                    // older profiles may have saved "mysite.com" without https://
                    val url = com.orion.templete.presentation.artist_register.normalizeWebsite(artist.wikipedia_url)
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    } catch (e: android.content.ActivityNotFoundException) {
                        android.widget.Toast.makeText(context, "Couldn't open this link", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

@Composable
private fun ButtonSection(
    artistId: String,
    initialFollowState: Boolean,
    viewModel: ArtistProfileViewModel,
    messageUsername: String? = null,
    onMessage: (String) -> Unit = {},
    onShare: (() -> Unit)? = null
) {
    var isFollowing by remember { mutableStateOf(initialFollowState) }
    var isLoading by remember { mutableStateOf(false) }

    // Observe follow state
    LaunchedEffect(viewModel.followArtistUiState) {
        when (viewModel.followArtistUiState) {
            is FollowArtistUiState.Ideal -> isLoading = false
            is FollowArtistUiState.Loading -> isLoading = true
            is FollowArtistUiState.Success -> {
                isLoading = false
                isFollowing = true
            }
            is FollowArtistUiState.Error -> {
                isLoading = false
            }
        }
    }

    // Observe unfollow state
    LaunchedEffect(viewModel.unFollowArtistUiState) {
        when (viewModel.unFollowArtistUiState) {
            is UnFollowArtistUiState.Ideal -> isLoading = false
            is UnFollowArtistUiState.Loading -> isLoading = true
            is UnFollowArtistUiState.Success -> {
                isLoading = false
                isFollowing = false
            }
            is UnFollowArtistUiState.Error -> {
                isLoading = false
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = {
                if (!isFollowing) {
                    viewModel.followUser(artistId)
                } else {
                    viewModel.unfollowArtist(artistId)
                }
            },
            modifier = Modifier.weight(1f),
            enabled = !isLoading
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = LocalContentColor.current
                    )
                }
                isFollowing -> Text("Following")
                else -> Text("Follow")
            }
        }
        // Instagram-style "Message" next to Follow; only for other people who have an Artistry account
        if (messageUsername != null) {
            MessageButton(
                onClick = { onMessage(messageUsername) },
                modifier = Modifier.weight(1f)
            )
        }
        if (onShare != null) ShareProfileButton(onClick = onShare)
    }
}

// Small third button like Instagram's: send this profile to a chat
@Composable
private fun ShareProfileButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Icon(
            imageVector = PaperPlaneIcon,
            contentDescription = "Share profile",
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun MessageButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text("Message")
    }
}

// Opens the conversation with [username]. Coming from that same conversation (thread -> profile -> Message),
// it goes back to it instead of stacking a second copy of the thread.
private fun NavController.openChatWith(username: String) {
    val peer = username.trim()
    if (peer.isEmpty() || currentDestination?.route != Screens.UserProfile.route) return
    val previous = previousBackStackEntry
    if (previous != null &&
        previous.destination.route == Screens.ChatThread.route &&
        previous.arguments?.getString(Screens.ChatThread.ARG_PEER) == peer
    ) {
        popBackStack()
        return
    }
    navigate(Screens.ChatThread.route(peer))
}

// Profile of an Artistry account that is not an artist (no artworks): picture, name, @username and "Message"
@Composable
private fun AccountProfileContent(
    username: String,
    user: UserDTO,
    messageUsername: String?,
    onMessage: (String) -> Unit
) {
    // The backend can leave fields out even though the DTO declares them non-null
    val name: String? = user.name
    val picture: String? = user.profilePicture
    val displayName = name?.trim()?.takeIf { it.isNotEmpty() } ?: username
    val avatar = picture?.trim()?.takeIf { it.isNotEmpty() }
    var showImagePopup by remember { mutableStateOf(false) }
    var showShareSheet by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val shareProfile = remember(username, displayName, avatar) {
        SharePayload.Profile(ProfileRef(id = username, name = displayName, avatar = avatar, subtitle = "@$username"))
    }

    if (showImagePopup && avatar != null) {
        ImagePopup(imageUrl = avatar) { showImagePopup = false }
    }
    if (showShareSheet) {
        ShareToChatSheet(
            payload = shareProfile,
            onDismiss = { showShareSheet = false },
            onShareExternally = { shareTextExternally(context, shareProfile.shareLinkText()) }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChatAvatar(
                url = avatar,
                name = displayName,
                size = 70.dp,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(enabled = avatar != null) { showImagePopup = true }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "@$username",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (messageUsername != null) {
                MessageButton(
                    onClick = { onMessage(messageUsername) },
                    modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            ShareProfileButton(onClick = { showShareSheet = true })
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Artistry member",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

// MVVM: Display artist statistics (followers, likes, artworks count)
@Composable
fun ArtistStatsSection(statsState: ArtistStatsUiState) {
    when (statsState) {
        is ArtistStatsUiState.Success -> {
            val stats = statsState.stats
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Followers",
                    value = stats.followers.toString()
                )
                Divider(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
                StatItem(
                    label = "Likes",
                    value = stats.totalLikesOnArtworks.toString()
                )
                Divider(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
                StatItem(
                    label = "Artworks",
                    value = stats.totalArtworks.toString()
                )
            }
        }
        is ArtistStatsUiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }
        is ArtistStatsUiState.Error -> {
            // Silent error - don't show error on stats
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}
