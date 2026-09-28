package com.orion.templete.presentation.profile

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.orion.templete.R
import com.orion.templete.data.model.artwork_model.ArtworkDTO
import com.orion.templete.data.model.user_model.RegisterArtistRequest
import com.orion.templete.data.model.user_model.UserDTO
import com.orion.templete.presentation.artist_profile.ArtistStatsSection
import com.orion.templete.presentation.common.ArtworkItem
import com.orion.templete.presentation.common.ErrorScreen
import com.orion.templete.presentation.common.Screens
import com.orion.templete.presentation.components.AnimatedPreloader
import com.orion.templete.presentation.components.AppIcon
import com.orion.templete.presentation.profile.common.DrawerContent
import com.orion.templete.presentation.ui.theme.TempleteTheme
import kotlinx.coroutines.launch
import kotlin.math.min

@Composable
fun ProfileScreen(
    navController: NavController,
    logOut: () -> Unit = {},
    viewModel: ProfileScreenViewModel = hiltViewModel()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val isUserArtist = viewModel.isUserArtist
    val uiState = viewModel.userData
    LaunchedEffect(Unit) {
        viewModel.refreshProfile()
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    ModalDrawerSheet(
                        drawerContainerColor = MaterialTheme.colorScheme.background,
                        windowInsets = WindowInsets(0)
                    ) {
                        DrawerContent(
                            navController = navController,
                            logOut = logOut,
                            onClose = { scope.launch { drawerState.close() } }
                        )
                    }
                }
            }
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                when (uiState) {
                    is ProfileScreenUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is ProfileScreenUiState.Success -> {
                        if (isUserArtist) {
                            ArtistProfileContent(
                                user = uiState.user,
                                onMenuClick = { scope.launch { drawerState.open() } },
                                onEditClick = {
                                    navController.navigate(Screens.EditArtist.route)
                                },
                                navController = navController,
                                viewModel = viewModel
                            )
                        } else {
                            ProfileContent(
                                user = uiState.user,
                                onMenuClick = { scope.launch { drawerState.open() } },
                                onEditClick = { navController.navigate(Screens.UserEditScreen.route) },
                                navController = navController,
                                viewModel = viewModel
                            )
                        }
                    }
                    is ProfileScreenUiState.Error -> {
                        ErrorScreen(
                            message = uiState.message,
                            onRetry = { viewModel.refreshProfile() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileContent(
    user: UserDTO,
    onMenuClick: () -> Unit,
    onEditClick: () -> Unit,
    navController: NavController,
    viewModel: ProfileScreenViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        UserCard(
            user = user,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        ButtonSection(onClick = onEditClick, onMenuClick = onMenuClick)
        Spacer(modifier = Modifier.height(16.dp))
        ExploreMoreArtistsCard(navController = navController, viewModel = viewModel)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ArtistProfileContent(
    user: UserDTO,
    onMenuClick: () -> Unit,
    onEditClick: () -> Unit,
    navController: NavController,
    viewModel: ProfileScreenViewModel
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showImagePopup by remember { mutableStateOf(false) }
    val artWorksUiState = viewModel.artWorksUiState

    val artistDetails by viewModel.artistDetails.collectAsState()

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
        // Use artist image if available, fallback to user image
        val imageUrl = artistDetails?.image_url ?: user.profilePicture
        ImagePopup(imageUrl = imageUrl) {
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
                // MVVM: Pass artist details to header if available
                ArtistProfileHeader(
                    user = user,
                    artistDetails = artistDetails,
                    onImageClick = { showImagePopup = true }
                )
                Spacer(modifier = Modifier.height(12.dp))
                // MVVM: Display artist stats
                ArtistStatsSection(viewModel.artistStatsUiState)
                Spacer(modifier = Modifier.height(12.dp))
                ArtistDescriptionSection(user, artistDetails)
                Spacer(modifier = Modifier.height(12.dp))
                ButtonSection(
                    onClick = onEditClick,
                    onMenuClick = onMenuClick
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
                        0 -> artworkListItems(artworks, navController)
                        1 -> artworkGridItems(artworks, navController)
                    }
                }
            }
            is ArtWorksUiState.Error -> {
                item(key = "error") {
                    ErrorScreen(
                        message = artWorksUiState.message,
                        onRetry = { viewModel.refreshProfile() }
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
                    painter = painterResource(id = R.drawable.ic_list),
                    contentDescription = "List View",
                    modifier = Modifier.size(24.dp)
                )
            }
        )
        Tab(
            selected = selectedTabIndex == 1,
            onClick = { onTabSelected(1) },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_gird),
                    contentDescription = "Grid View",
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
            contentScale = ContentScale.Fit,
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
                text = "Your posted artworks will appear here",
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
            contentDescription = "Profile Image",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun ArtistProfileHeader(
    user: UserDTO,
    artistDetails: com.orion.templete.data.model.user_model.RegisterArtistRequest? = null,
    onImageClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // MVVM: Use artist image if available
            val displayImage = artistDetails?.image_url ?: user.profilePicture
            AsyncImage(
                model = displayImage,
                contentDescription = "Profile picture",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .clickable { onImageClick() },
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                // MVVM: Use artist name if available
                val displayName = artistDetails?.name ?: user.name
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "ID: ${user.id.take(8)}...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }
        AppIcon(
            icon = R.drawable.ic_logo_no_bacground,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(40.dp)
        )
    }
}

@Composable
fun ArtistDescriptionSection(
    user: UserDTO,
    artistDetails: RegisterArtistRequest? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Birth Date
        val displayDate = artistDetails?.birth_date ?: user.dob
        displayDate?.let {
            if (it.isNotEmpty()) {
                Text(
                    text = "Birth: $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        // Death Date (Artist only)
        artistDetails?.death_date?.let {
            if (it.isNotEmpty()) {
                Text(
                    text = "Death: $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        // Nationality
        artistDetails?.nationality?.let {
            if (it.isNotEmpty()) {
                Text(
                    text = "Nationality: $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        // Art Movement
        artistDetails?.art_movement?.let {
            if (it.isNotEmpty()) {
                Text(
                    text = "Art Movement: $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        // Education
        artistDetails?.education?.let {
            if (it.isNotEmpty()) {
                Text(
                    text = "Education: $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        // Awards
        artistDetails?.awards?.let {
            if (it.isNotEmpty()) {
                Text(
                    text = "Awards: $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        // Notable Works
        artistDetails?.notable_works?.let {
            if (it.isNotEmpty()) {
                Text(
                    text = "Notable Works: $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        // Wikipedia URL
        artistDetails?.wikipedia_url?.let {
            if (it.isNotEmpty()) {
                Text(
                    text = "Wikipedia: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        // Description
        artistDetails?.description?.let {
            if (it.isNotEmpty()) {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        // Show user gender if artist details not available
        if (artistDetails == null) {
            user.gender?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun ExploreMoreArtistsCard(navController: NavController, viewModel: ProfileScreenViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        val context = LocalContext.current
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedPreloader(
                R.raw.artist,
                modifier = Modifier.size(
                    300.dp
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Title
            Text(
                text = "Become an Artist",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Unleash Your Creativity",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Share your artwork with a global audience and turn your passion into opportunity.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Primary Button
            Button(
                onClick = {
                    if (viewModel.canRegisterAsArtist()) {
                        navController.navigate(Screens.ArtistRegister.route)
                    } else {
                        Toast.makeText(
                            context,
                            "You are already registered as an artist.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "Start Your Artist Journey",
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "🎨 It's free and takes just a minute!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }

//    Box(modifier = Modifier.fillMaxSize()) {
//            AsyncImage(
//                model = R.drawable.become_artist,
//                contentDescription = "Featured artist",
//                modifier = Modifier.fillMaxSize(),
//                contentScale = ContentScale.Crop
//            )
//
//            Box(
//                modifier = Modifier
//                    .matchParentSize()
//                    .background(
//                        Brush.verticalGradient(
//                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)),
//                            startY = 0f,
//                            endY = Float.POSITIVE_INFINITY
//                        )
//                    )
//            )
//
//            val context = LocalContext.current
//            Button(
//                onClick = {
//                    if (viewModel.canRegisterAsArtist()) {
//                        navController.navigate(Screens.ArtistRegister.route)
//                    } else {
//                        Toast.makeText(
//                            context,
//                            "You are already registered as an artist.",
//                            Toast.LENGTH_LONG
//                        ).show()
//                    }
//                },
//                modifier = Modifier
//                    .align(Alignment.CenterEnd)
//                    .padding(end = 12.dp),
//                shape = RoundedCornerShape(8.dp)
//            ) {
//                Text(text = "Register", color = MaterialTheme.colorScheme.onPrimary)
//            }
//        }
    }
}

@Composable
private fun ButtonSection(onClick: () -> Unit, onMenuClick: () -> Unit = {}) {
    TempleteTheme {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onClick() }
                    .background(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .border(
                        1.dp,
                        color = MaterialTheme.colorScheme.onSurface,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clip(shape = RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Edit",
                    modifier = Modifier.padding(12.dp),
                    color = LocalContentColor.current,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu"
                )
            }
        }
    }
}

@Composable
fun UserCard(user: UserDTO, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                AsyncImage(
                    model = user.profilePicture,
                    contentDescription = "Profile picture",
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "ID: ${user.id.take(8)}...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }

            AppIcon(
                icon = R.drawable.ic_logo_no_bacground,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}
