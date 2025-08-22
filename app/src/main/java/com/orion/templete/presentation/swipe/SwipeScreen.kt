package com.orion.templete.presentation.swipe

import android.util.Log
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.orion.templete.R
import com.orion.templete.data.model.artwork_model.ArtworkDTO
import com.orion.templete.presentation.components.AppIcon
import com.orion.templete.presentation.swipe.components.Direction
import com.orion.templete.presentation.swipe.components.rememberSwipeableCardState
import com.orion.templete.presentation.swipe.components.swipableCard
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.abs
import kotlin.math.max
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import com.orion.templete.presentation.components.AnimatedPreloader
import com.orion.templete.presentation.components.ImageCarouselBottomSheet
import com.orion.templete.presentation.favorites.CollectionViewModel
import com.orion.templete.util.SecureStorage
import com.orion.templete.util.ShareUtils
import com.orion.templete.util.extractYear
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeScreen(vm:SwipeScreenViewModel = hiltViewModel() , navigateToDetailScreen: (artwork: ArtworkDTO) -> Unit = {}) {
    // State to track the current layout mode (true for swipe, false for scroll)
    val context = LocalContext.current
    var isSwipeLayout by remember { mutableStateOf(SecureStorage(context).getLayout()) }

    ArtCardRow(header = {
        HeaderRow(
            isSwipeLayout = isSwipeLayout,
            onToggleLayout = {
                isSwipeLayout = !isSwipeLayout
                SecureStorage(context).setLayout(isSwipeLayout)
            }
        )
    }, content = {
        if (isSwipeLayout) {
            SwipeCard(navigateToDetailScreen, vm)
        } else {
            ScrollCard(navigateToDetailScreen, vm)
        }
    })
}


@Composable
fun ArtCardRow(
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    content: (@Composable () -> Unit)? = null,
) {
    Column {
        header?.invoke()
        content?.invoke()
    }
}

@Composable
private fun HeaderRow(
    modifier: Modifier = Modifier,
    isSwipeLayout: Boolean = true,
    onToggleLayout: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, bottom = 4.dp, top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        // Empty spacer to balance the layout
        Spacer(modifier = Modifier.size(48.dp))

        // App logo in center
        AppIcon(
            icon = R.drawable.ic_logo_no_bacground, 
            tint = MaterialTheme.colorScheme.onSurface, 
            modifier = Modifier.size(40.dp)
        )

        // Layout toggle button
        IconButton(
            onClick = onToggleLayout,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                painter = painterResource(id = if (isSwipeLayout) R.drawable.ic_list else R.drawable.cardlayer),
                contentDescription = if (isSwipeLayout) "Switch to scroll layout" else "Switch to swipe layout",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun ScrollCard(
    navigateToDetailScreen: (artwork: ArtworkDTO) -> Unit,
    swipeScreenViewModel: SwipeScreenViewModel = hiltViewModel()
) {
    val stateOfCards = swipeScreenViewModel.state
    val context = LocalContext.current
    val userId = SecureStorage(context).getUserId()
    val collectionViewModel: CollectionViewModel = hiltViewModel()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    when {
        stateOfCards.error?.isNotBlank() == true -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = stateOfCards.error)
            }
        }

        stateOfCards.items.isEmpty() -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AnimatedPreloader(R.raw.loading_app)
            }
        }

        else -> {
            LaunchedEffect(stateOfCards.items) {
                if (stateOfCards.items.isNotEmpty()) {
                    coroutineScope.launch {
                        listState.animateScrollToItem(0)
                    }
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
            ) {
                items(stateOfCards.items) { artwork ->
                    swipeScreenViewModel.likeArtwork(artwork.id.toString())
                    ScrollCardItem(
                        artwork = artwork,
                        onLike = {
                            swipeScreenViewModel.likeArtwork(artwork.id.toString())
                        },
                        onDislike = {
                            swipeScreenViewModel.disLikeArtwork(artwork.id.toString())
                        },
                        onSaveToFavorites = {
                            artwork.id?.let { id ->
                                swipeScreenViewModel.trackArtworkSavedToFavorites(id)
                                collectionViewModel.saveOnFavorites(
                                    "saved_${userId}",
                                    artworkId = id
                                )
                            }
                        },
                        onClick = {
                            artwork.id?.let { swipeScreenViewModel.trackArtworkView(it) }
                            navigateToDetailScreen(artwork)
                        }
                    )
                    if (artwork == stateOfCards.items.last()) {
                        swipeScreenViewModel.loadNextItems()
                    }
                }
                item {
                    if (stateOfCards.isLoading) {
                        AnimatedPreloader(R.raw.loading_app)
                    }
                }
            }
        }
    }
}

@Composable
fun ScrollCardItem(
    artwork: ArtworkDTO,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onSaveToFavorites: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column {
            // Artwork image
            val screenHeight = LocalContext.current.resources.displayMetrics.heightPixels / (LocalContext.current.resources.displayMetrics.density * 2)
            Image(
                painter = rememberAsyncImagePainter(artwork.imageUrl),
                contentDescription = artwork.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(screenHeight.dp),
                contentScale = ContentScale.Fit
            )

            // Artwork details
            ArtworkProfileCard(
                artwork = artwork
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeCard(
    navigateToDetailScreen: (artwork: ArtworkDTO) -> Unit,
    swipeScreenViewModel: SwipeScreenViewModel = hiltViewModel()
) {
    val stateOfCards = swipeScreenViewModel.state
    val scope = rememberCoroutineScope()
    var isSwipedLeft by remember { mutableStateOf(false) }
    val collectionViewModel: CollectionViewModel = hiltViewModel()
    val context = LocalContext.current
    val userId = SecureStorage(context).getUserId()
    val firstTime = SecureStorage(context).isFirstTime()
    var showUserHint by remember { mutableStateOf(firstTime) }
    when {
        stateOfCards.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AnimatedPreloader(R.raw.loading_app)
            }
        }

        stateOfCards.error?.isNotBlank() == true -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = stateOfCards.error)
            }
        }

        stateOfCards.items.isEmpty() -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No more items to load")
            }
        }

        else -> {
            val artworkList = stateOfCards.items as ArrayList<ArtworkDTO>
            val states = artworkList.reversed().map { it to rememberSwipeableCardState() }

            // Main container with glow effects
            Box(modifier = Modifier.fillMaxSize()) {
                var leftGlowTarget by remember { mutableStateOf(0f) }
                var rightGlowTarget by remember { mutableStateOf(0f) }
                val leftGlowAlpha by animateFloatAsState(
                    targetValue = leftGlowTarget,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                    label = "leftGlowAlpha"
                )

                val rightGlowAlpha by animateFloatAsState(
                    targetValue = rightGlowTarget,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                    label = "rightGlowAlpha"
                )

                // Cards
                Box {
                    states.forEach { (artwork, state) ->
                        LaunchedEffect(state.swipedDirection) {
                            isSwipedLeft = state.swipedDirection == Direction.Left
                        }

                        // Update glow alphas based on card position
                        LaunchedEffect(state.offset.value.x) {
                            // Calculate normalized position (-1 to 1)
                            val normalizedX = state.offset.value.x / state.maxWidth

                            // Update left glow (red) when swiping left
                            leftGlowTarget = if (normalizedX < 0) {
                                (-normalizedX).coerceIn(0f, 0.7f)
                            } else {
                                0f
                            }

                            // Update right glow target when swiping right
                            rightGlowTarget = if (normalizedX > 0) {
                                normalizedX.coerceIn(0f, 0.7f)
                            } else {
                                0f
                            }
                        }

                        if (state.swipedDirection == null) {
                            ProfileCard(modifier = Modifier
                                .padding(16.dp)
                                .swipableCard(state = state,
                                    blockedDirections = listOf(Direction.Down),
                                    onSwiped = {
                                        if (state.swipedDirection == Direction.Right) {
                                            swipeScreenViewModel.likeArtwork(
                                                artwork?.id.toString()
                                            )
                                        }
                                        if (state.swipedDirection == Direction.Left) {
                                            swipeScreenViewModel.disLikeArtwork(
                                                artwork?.id.toString()
                                            )
                                        }
                                        if (state.swipedDirection == Direction.Up) {

                                            artwork.id?.let { it1 ->
                                                swipeScreenViewModel.trackArtworkSavedToFavorites(it1)
                                                collectionViewModel.saveOnFavorites(
                                                    "saved_${userId}",
                                                    artworkId = it1
                                                )
                                            }
                                        }
                                        if (artworkList.isNotEmpty()) {
                                            artworkList.remove(artwork)
                                        }
                                        if (artworkList.isEmpty()) {
                                            swipeScreenViewModel.loadNextItems()
                                        }
                                        isSwipedLeft = state.swipedDirection == Direction.Left
                                    },
                                    onSwipeCancel = {
                                        Log.d("Swappable-Card", "Cancelled swipe")
                                    })
                                .clickable {
                                    artwork.id?.let { swipeScreenViewModel.trackArtworkView(it) }
                                    navigateToDetailScreen(artwork)
                                }, artwork = artwork
                            )
                        }
                    }
                    if (showUserHint) {
                        ImageCarouselBottomSheet(
                            onDismiss = {
                                showUserHint = false
                                SecureStorage(context).setFirstTime(false)
                            }
                        )
                    }
                }
                // Left glow box
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            if (leftGlowAlpha > 0) {
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Red.copy(alpha = leftGlowAlpha),
                                            Color.Transparent
                                        ),
                                        startX = 0f,
                                        endX = size.width * 0.3f
                                    )
                                )
                            }
                            if (leftGlowAlpha == 0.7f) {
                                leftGlowTarget = 0f
                            }
                        }
                )

                // Right glow box
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            if (rightGlowAlpha > 0) {
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Green.copy(alpha = rightGlowAlpha)
                                        ),
                                        startX = size.width * 0.7f,
                                        endX = size.width
                                    )
                                )
                            }
                            if (rightGlowAlpha == 0.7f) {
                                rightGlowTarget = 0f
                            }
                        }
                )
            }
        }
    }
}

@Composable
fun ArtworkProfileCard(artwork: ArtworkDTO) {
    // State for expanded/collapsed state
    var isExpanded by remember { mutableStateOf(false) }
    // Configure animation specs
    val cardHeight by animateDpAsState(
        targetValue = if (isExpanded) 300.dp else 150.dp,
        label = "cardHeight"
    )

    val context = LocalContext.current

    Card(
        modifier = Modifier
            .height(cardHeight)
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                artwork.title?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Share button
                IconButton(
                    onClick = {
                        ShareUtils.shareArtwork(context, artwork)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                    )
                }
            }

            // Artist Name
            artwork.artist?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            artwork.currentLocation?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Light,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                artwork.medium?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                extractYear(artwork.releasedDate)?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Expanded content that only shows when expanded
            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = artwork.description?: "No description",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Light,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}



@Composable
private fun ProfileCard(
    modifier: Modifier,
    artwork: ArtworkDTO,
) {
    Card(
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier,
    ) {
        Image(
            modifier = Modifier.fillMaxSize(),
            painter = rememberAsyncImagePainter(artwork.imageUrl),
            contentDescription = null
        )
    }
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        ArtworkProfileCard(artwork)
    }
}
