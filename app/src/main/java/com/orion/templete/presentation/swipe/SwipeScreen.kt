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
import androidx.compose.runtime.mutableIntStateOf
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
import com.orion.templete.data.model.RecommendedArtworkDTO
import com.orion.templete.presentation.components.AppIcon
import com.orion.templete.presentation.swipe.components.Direction
import com.orion.templete.presentation.swipe.components.rememberSwipeableCardState
import com.orion.templete.presentation.swipe.components.swipableCard


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeScreen(navigateToDetailScreen: (artwork: RecommendedArtworkDTO) -> Unit = {}) {
    ArtCardRow(header = {
        HeaderRow()
    }, content = {
        SwipeCard(navigateToDetailScreen)
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
    navigateToDetailScreen: (artwork: RecommendedArtworkDTO) -> Unit,
    swipeScreenViewModel: SwipeScreenViewModel = hiltViewModel()
) {
    val stateOfCards = swipeScreenViewModel.state
    val scope = rememberCoroutineScope()
    var isSwipedLeft by remember { mutableStateOf(false) }
    when {
        stateOfCards.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
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
            val artworkList = stateOfCards.items
            val states = artworkList.reversed().map { it to rememberSwipeableCardState() }
            var currentIndex by remember { mutableIntStateOf(0) }
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
                                    if(state.swipedDirection == Direction.Right)
                                    {
                                        Log.d("Swappable-Card", "Swiped ${state.swipedDirection}")
                                        swipeScreenViewModel.likeArtwork(artwork.artwork?.id.toString(), "1")
                                    }
                                    currentIndex++
                                    if (currentIndex >= artworkList.size && !stateOfCards.isLoading) {
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
                            ArtworkProfileCard(artwork.artwork?.name, artwork.artistName, artwork.artworkGenre, artwork.artwork?.releasedDate)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArtworkProfileCard(
    name: String?,
    artistName: String?,
    artworkGenre: String?,
    releasedDate: String?
) {
    Card(
        modifier = Modifier
            .height(150.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Artwork Name
            Text(
                text = name ?: "No name",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Artist Name
            Text(
                text = artistName ?: "Unknown Artist",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = artworkGenre ?: "Unknown Genre",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = releasedDate ?: "Unknown Date",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}



@Composable
private fun ProfileCard(
    modifier: Modifier,
    artwork: RecommendedArtworkDTO,
) {
    Card(
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier,
    ) {
        Image(
            modifier = Modifier.fillMaxSize(),
            painter = rememberAsyncImagePainter(artwork.artwork?.imageUrl),
            contentDescription = null
        )
    }
}
