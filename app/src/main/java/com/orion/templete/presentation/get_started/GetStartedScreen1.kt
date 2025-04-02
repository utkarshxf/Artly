package com.orion.templete.presentation.get_started

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orion.templete.R
import com.orion.templete.presentation.ui.theme.TempleteTheme
import com.orion.templete.presentation.ui.theme.mollie
import kotlinx.coroutines.delay


@Composable
fun GetStartedScreen1() {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.8f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 200f)
    )
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .paint(
                    painter = painterResource(id = R.drawable.get_started),
                    contentScale = ContentScale.Crop
                )
                .fillMaxSize()
        )
        Box(
            contentAlignment = Alignment.Center, modifier = Modifier.size(dP(121), dP(215))
        ) {
            Column {
                Spacer(modifier = Modifier.height(dP(80)))
                Box(
                    modifier = Modifier
                        .size(dP(115), dP(127))
                        .paint(
                            painter = painterResource(id = R.drawable.cardlayer),
                            contentScale = ContentScale.FillBounds
                        ), contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top,
                        modifier = Modifier.padding(dP(8))
                    ) {
                        Text(
                            text = "Artistry",
                            maxLines = 1,
                            fontFamily = mollie,
                            fontSize = sP(sp = 25),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Art Club",
                            maxLines = 1,
                            fontFamily = mollie,
                            fontSize = sP(sp = 25),
                            color = Color.White
                        )
                        Text(
                            text = "Discover, admire, acquire: Your canvas for artistic connect.",
                            fontSize = sP(sp = 6),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(dP(20)))
                    }
                }
            }
            Box(
                modifier = Modifier
                    .size(dP(35), dP(35))
                    .scale(scale)
                    .paint(
                        painter = painterResource(id = R.drawable.getstarted_button),
                        contentScale = ContentScale.Crop
                    )
                    .align(Alignment.BottomEnd)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() }, // Remove ripple effect
                        indication = null) {
                        isPressed = !isPressed
                    }
            )
        }
    }
}

@Composable
fun dP(dp: Int) = (dp * LocalDensity.current.density).dp

@Composable
fun sP(sp: Int) = (sp * LocalDensity.current.density).sp


@Preview
@Composable
private fun TempScreen() {
    TempleteTheme {
        GetStartedScreen1()
    }
}