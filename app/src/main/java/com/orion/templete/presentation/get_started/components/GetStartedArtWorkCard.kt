package com.orion.templete.presentation.get_started.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.orion.templete.R
import com.orion.templete.presentation.ui.theme.TempleteTheme

@Composable
fun GetStartedArtWorkCard(
    imageResId: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 246.dp, height = 340.dp)
            .clip(RoundedCornerShape(62.dp))
    ) {
        Image(
            painter = painterResource(id = imageResId),
            contentDescription = "Artwork image",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent),
                        startY = 340f ,
                        endY = 0.4f
                    )
                )
        )
    }
}

@Preview
@Composable
private fun myprev() {
    TempleteTheme {
        GetStartedArtWorkCard(R.drawable.artwork_placeholder)
    }
}