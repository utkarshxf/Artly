package com.orion.templete.presentation.swipe

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.saveable.rememberSaveable
import com.orion.templete.presentation.chat.share.PaperPlaneIcon
import com.orion.templete.presentation.chat.share.ShareToChatSheet
import com.orion.templete.presentation.chat.share.toArtworkRef
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.orion.templete.R
import com.orion.templete.data.model.artwork_model.ArtworkDTO
import com.orion.templete.presentation.artwork_detail.SaveSection
import com.orion.templete.presentation.components.AnimatedPreloader
import com.orion.templete.presentation.components.AppIcon
import com.orion.templete.presentation.components.ImageCarouselBottomSheet
import com.orion.templete.presentation.favorites.CollectionViewModel
import com.orion.templete.presentation.swipe.components.BottomSheet
import com.orion.templete.presentation.swipe.components.Direction
import com.orion.templete.presentation.swipe.components.rememberSwipeableCardState
import com.orion.templete.presentation.swipe.components.swipableCard
import com.orion.templete.util.SecureStorage
import com.orion.templete.util.ShareUtils
import com.orion.templete.util.extractYear
import kotlinx.coroutines.launch

@Composable
fun SwipeScreen(
    vm: SwipeScreenViewModel = hiltViewModel(),
    navigateToDetailScreen: (artwork: ArtworkDTO) -> Unit = {},
    onChatClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var isSwipeLayout by remember { mutableStateOf(SecureStorage(context).getLayout()) }
    val unreadChats by vm.unreadConversationCount.collectAsState()

    Column {
        HeaderRow(
            isSwipeLayout = isSwipeLayout,
            onToggleLayout = {
                isSwipeLayout = !isSwipeLayout
                SecureStorage(context).setLayout(isSwipeLayout)
            },
            onChatClick = onChatClick,
            unreadChats = unreadChats
        )

        if (isSwipeLayout) {
            SwipeCard(navigateToDetailScreen, vm)
        } else {
            ScrollCard(navigateToDetailScreen, vm)
        }
    }
}

@Composable
private fun HeaderRow(
    modifier: Modifier = Modifier,
    isSwipeLayout: Boolean = true,
    onToggleLayout: () -> Unit = {},
    onChatClick: () -> Unit = {},
    unreadChats: Int = 0
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

        // The badge sits outside the IconButton, which clips its content to a circle
        Box(modifier = Modifier.size(48.dp)) {
            IconButton(
                onClick = onChatClick,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_message),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(28.dp),
                    contentDescription = when {
                        unreadChats == 1 -> "Messages, 1 unread chat"
                        unreadChats > 1 -> "Messages, $unreadChats unread chats"
                        else -> "Messages"
                    }
                )
            }
            // Over the icon's top-right corner (the 28dp icon spans 10..38dp of this 48dp box)
            UnreadChatsBadge(
                count = unreadChats,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-1).dp, y = 2.dp)
            )
        }
    }
}

private val UnreadBadgeRed = Color(0xFFFF3040)

// Instagram-style red count bubble on the direct-messages icon; hidden at 0, capped at "9+"
@Composable
private fun UnreadChatsBadge(count: Int, modifier: Modifier = Modifier) {
    // Plain holder (not state): keeps the last count on screen while the badge animates out
    val lastCount = remember { IntArray(1) }
    if (count > 0) lastCount[0] = count
    val shown = if (count > 0) count else lastCount[0]
    AnimatedVisibility(
        visible = count > 0,
        modifier = modifier,
        enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
        exit = scaleOut() + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .clearAndSetSemantics { }
                .background(MaterialTheme.colorScheme.surface, CircleShape)
                .padding(2.dp)
                .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                .background(UnreadBadgeRed, CircleShape)
                .padding(horizontal = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (shown > 9) "9+" else shown.toString(),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
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
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    when {
        stateOfCards.error?.isNotBlank() == true -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Button(onClick = { swipeScreenViewModel.loadNextItems() }) {
                    Text(text = "Error occurred. Tap to retry.")
                }
            }
        }

        stateOfCards.items.isEmpty() -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AnimatedPreloader(R.raw.loading_app)
            }
        }

        else -> {
            // Auto-scroll to top when new items are loaded
            LaunchedEffect(stateOfCards.items.size) {
                if (stateOfCards.items.isNotEmpty() && listState.firstVisibleItemIndex > 0) {
                    coroutineScope.launch {
                        listState.animateScrollToItem(0)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(
                    items = stateOfCards.items,
                    key = { artwork -> artwork.id ?: "item_${artwork.hashCode()}" }
                ) { artwork ->
                    ScrollCardItem(
                        artwork = artwork,
                        onLike = {
                            swipeScreenViewModel.likeArtwork(artwork.id.toString())
                        },
                        onDislike = {
                            swipeScreenViewModel.disLikeArtwork(artwork.id.toString())
                        },
                        onClick = {
                            artwork.id?.let { swipeScreenViewModel.trackArtworkView(it) }
                            navigateToDetailScreen(artwork)
                        }
                    )

                    // Load more items when reaching the last item
                    if (artwork == stateOfCards.items.last() && !stateOfCards.isLoading) {
                        LaunchedEffect(Unit) {
                            swipeScreenViewModel.loadNextItems()
                        }
                    }
                }

                if (stateOfCards.isLoading) {
                    item(key = "loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedPreloader(R.raw.loading_app)
                        }
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
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val screenHeight = context.resources.displayMetrics.heightPixels /
            (context.resources.displayMetrics.density * 2)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column {
            // Artwork image
            Image(
                painter = rememberAsyncImagePainter(artwork.imageUrl),
                contentDescription = artwork.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(screenHeight.dp),
                contentScale = ContentScale.Fit
            )

            // Artwork details
            ArtworkProfileCard(artwork = artwork)
        }
    }
}


@Composable
fun ArtworkProfileCard(artwork: ArtworkDTO) {
    var isExpanded by remember { mutableStateOf(false) }
    // Instagram-style share: send the artwork to chats (the sheet also offers "Share to…" other apps)
    val shareRef = remember(artwork) { artwork.toArtworkRef() }
    var showShareSheet by rememberSaveable { mutableStateOf(false) }

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
                .padding(16.dp),
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

                IconButton(onClick = {
                    if (shareRef != null) showShareSheet = true else ShareUtils.shareArtwork(context, artwork)
                }) {
                    Icon(
                        imageVector = PaperPlaneIcon,
                        contentDescription = "Share",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

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

            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = artwork.description ?: "No description",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Light,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
    if (showShareSheet && shareRef != null) {
        ShareToChatSheet(
            artwork = shareRef,
            onDismiss = { showShareSheet = false },
            onShareExternally = { ShareUtils.shareArtwork(context, artwork) }
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeCard(
    navigateToDetailScreen: (artwork: ArtworkDTO) -> Unit,
    swipeScreenViewModel: SwipeScreenViewModel = hiltViewModel()
) {
    val stateOfCards = swipeScreenViewModel.state
    val collectionViewModel: CollectionViewModel = hiltViewModel()
    val context = LocalContext.current

    val firstTime = SecureStorage(context).isFirstTime()
    var showUserHint by remember { mutableStateOf(firstTime) }
    var showSavedFolders by remember { mutableStateOf(false) }
    var currentArtworkIdToSave by remember { mutableStateOf("") }

    when {
        stateOfCards.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AnimatedPreloader(R.raw.loading_app)
            }
        }

        stateOfCards.error?.isNotBlank() == true -> {
            swipeScreenViewModel.resetPagination()
        }

        stateOfCards.items.isEmpty() -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Button(onClick = { swipeScreenViewModel.loadNextItems() }) {
                    Text(text = "Tap to load artworks")
                }
            }
        }

        else -> {
            val artworkList = remember(stateOfCards.items) {   stateOfCards.items as? ArrayList<ArtworkDTO> ?: arrayListOf() }
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

                // Bottom sheet for saving
                if (showSavedFolders) {
                    BottomSheet(onDismiss = { showSavedFolders = false }) {
                        SaveSection(
                            dismiss = { showSavedFolders = false },
                            currentArtworkIdToSave,
                            vm = collectionViewModel
                        )
                        swipeScreenViewModel.trackArtworkSavedToFavorites(currentArtworkIdToSave)
                    }
                }

                // Cards layer
                Box {
                    states.forEach { (artwork, state) ->
                        // Update glow alphas based on card position
                        LaunchedEffect(state.offset.value.x) {
                            val normalizedX = state.offset.value.x / state.maxWidth

                            leftGlowTarget = if (normalizedX < 0) {
                                (-normalizedX).coerceIn(0f, 0.7f)
                            } else {
                                0f
                            }

                            rightGlowTarget = if (normalizedX > 0) {
                                normalizedX.coerceIn(0f, 0.7f)
                            } else {
                                0f
                            }

                            // Auto-reset if maximum glow reached
                            if (leftGlowTarget >= 0.7f) {
                                leftGlowTarget = 0f
                            }
                            if (rightGlowTarget >= 0.7f) {
                                rightGlowTarget = 0f
                            }
                        }


                        if (state.swipedDirection == null) {
                            key(artwork.id) {
                                ProfileCard(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .swipableCard(
                                            state = state,
                                            blockedDirections = listOf(Direction.Down),
                                                                                        onSwiped = {
                                                swipeScreenViewModel.onCardSwiped()
                                                if (artworkList.isNotEmpty()) {
                                                    artworkList.remove(artwork)
                                                }
                                                if (artworkList.isEmpty()) {
                                                    swipeScreenViewModel.loadNextItems()
                                                }
                                                when (state.swipedDirection) {
                                                    Direction.Right -> {
                                                        swipeScreenViewModel.likeArtwork(
                                                            artwork.id.toString()
                                                        )
                                                    }
                                                    Direction.Left -> {
                                                        swipeScreenViewModel.disLikeArtwork(
                                                            artwork.id.toString()
                                                        )
                                                    }
                                                    Direction.Up -> {
                                                        artwork.id?.let { id ->
                                                            swipeScreenViewModel.trackArtworkSavedToFavorites(id)
                                                            currentArtworkIdToSave = id
                                                            showSavedFolders = true
                                                        }
                                                    }
                                                    else -> {}
                                                }
                                            },
                                            onSwipeCancel = {
                                                Log.d("Swappable-Card", "Cancelled swipe")
                                            }
                                        )
                                        .clickable {
                                            artwork.id?.let {
                                                swipeScreenViewModel.trackArtworkView(it)
                                            }
                                            navigateToDetailScreen(artwork)
                                        },
                                    artwork = artwork
                                )
                            }
                        }
                    }

                    // User hint on first time
                    if (showUserHint) {
                        ImageCarouselBottomSheet(
                            onDismiss = {
                                showUserHint = false
                                SecureStorage(context).setFirstTime(false)
                            }
                        )
                    }
                }

                // Left glow effect (red for dislike)
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
                        }
                )

                // Right glow effect (green for like)
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
                        }
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
            contentDescription = artwork.title,
            contentScale = ContentScale.Fit
        )
    }
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        ArtworkProfileCard(artwork)
    }
}
