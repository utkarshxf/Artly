package com.orion.templete.presentation.swipe

import android.util.Log
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import com.orion.templete.presentation.components.AnimatedPreloader
import com.orion.templete.presentation.favorites.CollectionViewModel
import com.orion.templete.util.SecureStorage
import com.orion.templete.util.extractYear


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeScreen(vm:SwipeScreenViewModel = hiltViewModel() , navigateToDetailScreen: (artwork: ArtworkDTO) -> Unit = {}) {
    ArtCardRow(header = {
        HeaderRow()
    }, content = {
        SwipeCard(navigateToDetailScreen , vm)
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
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, bottom = 4.dp, top = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(icon = R.drawable.artistry, tint = MaterialTheme.colorScheme.primary)
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
    val userId = SecureStorage(LocalContext.current).getUserDetails()?.id
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
            Box {
                states.forEach { (artwork, state) ->
                    LaunchedEffect(state.swipedDirection) {
                        isSwipedLeft = state.swipedDirection == Direction.Left
                    }
                    if (state.swipedDirection == null) {
                        ProfileCard(modifier = Modifier
                            .padding(16.dp)
                            .aspectRatio(3f / 4f)
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
                                    if(state.swipedDirection == Direction.Up){
                                        artwork.id?.let { it1 -> collectionViewModel.saveOnFavorites("saved_${userId}" , artworkId = it1) }
                                    }
                                    if(artworkList.isNotEmpty()){
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
                                navigateToDetailScreen(artwork)
                            }, artwork = artwork
                        )
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            ArtworkProfileCard(artwork)
                        }
                    }
                }
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
            Row {
                // Artwork Name
                artwork.title?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                AppIcon(icon = R.drawable.ic_logo_no_bacground, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(40.dp))
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
                    fontWeight = FontWeight.Medium,
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
                    style = MaterialTheme.typography.bodyMedium
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
}
