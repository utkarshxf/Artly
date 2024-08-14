package com.orion.templete.presentation.swipe

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.shapes
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.orion.templete.presentation.components.AppIcon
import com.orion.templete.R
import com.orion.templete.data.model.Content
import com.orion.templete.presentation.swipe.components.Direction
import com.orion.templete.presentation.swipe.components.rememberSwipeableCardState
import com.orion.templete.presentation.swipe.components.swipableCard


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeScreen(navigateToDetailScreen: (artwork:Content) -> Unit ) {
    ArtCardRow(
        header = {
            HeaderRow()
        },
        content = {
            SwipeCard(navigateToDetailScreen)
        }
    )
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
        AppIcon(icon = R.drawable.artistry , tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun SwipeCard( navigateToDetailScreen: (artwork:Content) -> Unit,swipeScreenViewModel: MyViewModel = hiltViewModel() ) {
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
                Text(text = stateOfCards.error ?: "An unknown error occurred")
            }
        }
        stateOfCards.items.isEmpty() && stateOfCards.endReached -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No more items to load")
            }
        }
        else -> {
            val artworkList = stateOfCards.items
            val states = artworkList.reversed().map { it to rememberSwipeableCardState() }
            var currentIndex by remember { mutableStateOf(0) }
            Box(
                Modifier
                    .padding(24.dp)
                    .fillMaxSize()
            ) {
                states.forEach { (artwork, state) ->
                    LaunchedEffect(state.swipedDirection) {
                        isSwipedLeft = state.swipedDirection == Direction.Left
                    }
                    if (state.swipedDirection == null) {
                        ProfileCard(
                            modifier = Modifier
                                .fillMaxSize()
                                .swipableCard(
                                    state = state,
                                    blockedDirections = listOf(Direction.Down),
                                    onSwiped = {
                                        Log.d("Swipeable-Card", "Swiped ${state.swipedDirection}")
                                        currentIndex++
                                        if (currentIndex >= artworkList.size && !stateOfCards.isLoading) {
                                            swipeScreenViewModel.loadNextItems()
                                        }
                                        isSwipedLeft = state.swipedDirection == Direction.Left
                                    },
                                    onSwipeCancel = {
                                        Log.d("Swipeable-Card", "Cancelled swipe")
                                    }
                                )
                                .clickable {
                                    navigateToDetailScreen(artwork)
                                },
                            artwork = artwork
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun ProfileCard(
    modifier: Modifier,
    artwork: Content,
) {
    Card(
        modifier = modifier,
        shape = shapes.large
    ) {
        Box {
            Image(
                modifier = Modifier.fillMaxSize(),
                painter = rememberAsyncImagePainter(artwork.imageUrl),
                contentDescription = null
            )
            Column(Modifier.align(Alignment.BottomStart)) {
                Text(
                    text = artwork.name,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}
