package com.orion.templete.presentation.artist_profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
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
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showImagePopup by remember { mutableStateOf(false) }
    val artWorksUiState = viewModel.artWorksUiState

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

    if (showImagePopup) {
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
                ProfileDescriptionSection(artist = artist)
                Spacer(modifier = Modifier.height(12.dp))
                ButtonSection(
                    artistId = artist.id,
                    initialFollowState = artist.follow,
                    viewModel = viewModel
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
    val intent = remember { Intent(Intent.ACTION_VIEW, Uri.parse(artist.wikipedia_url)) }

    Column(modifier = Modifier.fillMaxWidth()) {
        GenreSection(artist.art_movement)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = artist.description,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 6,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = artist.wikipedia_url,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable { context.startActivity(intent) }
        )
    }
}

@Composable
private fun ButtonSection(
    artistId: String,
    initialFollowState: Boolean,
    viewModel: ArtistProfileViewModel
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
    }
}
