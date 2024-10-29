package com.orion.templete.presentation.get_started

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.orion.templete.R
import com.orion.templete.presentation.get_started.components.GetStartedArtWorkCard
import com.orion.templete.presentation.ui.theme.TempleteTheme
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds


@Composable
fun GetStartedScreen(artworkImages: List<Int>) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AnimatedArtworkGallery()
    }
}

data class Artwork(val id: Int, val resourceId: Int, val description: String)

@Composable
fun AnimatedArtworkGallery() {
    val artworks = listOf(
        Artwork(1, R.drawable.artwork_placeholder, "Artwork 1"),
        Artwork(2, R.drawable.artwork_placeholder, "Artwork 2"),
        Artwork(3, R.drawable.artwork_placeholder, "Artwork 3"),
        Artwork(4, R.drawable.artwork_placeholder, "Artwork 4"),
        Artwork(5, R.drawable.artwork_placeholder, "Artwork 5"),
        Artwork(6, R.drawable.artwork_placeholder, "Artwork 6"),
        Artwork(7, R.drawable.artwork_placeholder, "Artwork 7"),
        Artwork(8, R.drawable.artwork_placeholder, "Artwork 8"),
        Artwork(9, R.drawable.artwork_placeholder, "Artwork 9")
    )

    var animate by remember { mutableStateOf(false) }
    val transition = updateTransition(animate, label = "animate")

    val leftColumnOffset by transition.animateFloat(
        label = "leftColumnOffset",
        transitionSpec = { tween(durationMillis = 30000) }
    ) { if (it) -200f else 0f }

    val centerColumnOffset by transition.animateFloat(
        label = "centerColumnOffset",
        transitionSpec = { tween(durationMillis = 30000) }
    ) { if (it) 200f else 0f }

    val rightColumnOffset by transition.animateFloat(
        label = "rightColumnOffset",
        transitionSpec = { tween(durationMillis = 30000) }
    ) { if (it) -200f else 0f }

    LaunchedEffect(Unit) {
        while (true) {
            delay(15.seconds)
            animate = !animate
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ArtworkColumn(
                artworks.slice(0..8),
                Modifier.offset(y = leftColumnOffset.dp),
                true
            )
            ArtworkColumn(
                artworks.slice(0..8),
                Modifier.offset(y = centerColumnOffset.dp),
                false
            )
            ArtworkColumn(
                artworks.slice(0..8),
                Modifier.offset(y = rightColumnOffset.dp),
                true
            )
        }
    }
}

@Composable
fun ArtworkColumn(
    artworks: List<Artwork>,
    modifier: Modifier = Modifier,
    isHalfVisible: Boolean
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(if (isHalfVisible) 120.dp else 200.dp)
    ) {
        artworks.forEach { artwork ->
            GetStartedArtWorkCard(artwork.resourceId)
        }
    }
}



@Preview
@Composable
private fun myScreen() {
    TempleteTheme {
        var list = listOf<Int>()
        for (int in 1..12)
            list = list.plus(R.drawable.artwork_placeholder)
        GetStartedScreen(list)
    }
}
