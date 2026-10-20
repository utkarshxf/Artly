package com.orion.templete.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orion.templete.R
import com.orion.templete.presentation.ui.theme.TempleteTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageCarouselBottomSheet(
    onDismiss: () -> Unit
) {
    // Define your image and text content
    val carouselItems = listOf(
        CarouselItem(
            imageRes = R.drawable.ic_right_swip,
            title = "Swipe Right to Like",
            description = "Swipe right to like an artwork that appeals to you",
            backgroundColor = MaterialTheme.colorScheme.surface // Light pink
        ),
        CarouselItem(
            imageRes = R.drawable.ic_left_swip,
            title = "Swipe Left to Dislike",
            description = "Swipe left to dislike an artwork that doesn't match your taste",
            backgroundColor = MaterialTheme.colorScheme.surface // Light blue
        ),
        CarouselItem(
            imageRes = R.drawable.ic_up_swipe,
            title = "Swipe Up to Save",
            description = "Swipe up to save an artwork to your Swipped collection",
            backgroundColor = MaterialTheme.colorScheme.surface
        )
    )
    val scope = rememberCoroutineScope()
    var currentPageIndex by remember { mutableStateOf(0) }
    val isLastPage = currentPageIndex == carouselItems.size - 1
    var skipPartiallyExpanded by rememberSaveable { mutableStateOf(true) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded)
    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        sheetState = bottomSheetState,
        dragHandle = null
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(carouselItems[currentPageIndex].backgroundColor)
                .padding(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Carousel content with animated transitions
                val pagerState = rememberPagerState(initialPage = 0) { carouselItems.size }

                // Update currentPageIndex when page changes
                LaunchedEffect(pagerState.currentPage) {
                    currentPageIndex = pagerState.currentPage
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) { page ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Image(
                            painter = painterResource(id = carouselItems[page].imageRes),
                            contentDescription = null,
                            modifier = Modifier.height(300.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }


                // Title and description
                Text(
                    text = carouselItems[currentPageIndex].title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Text(
                    text = carouselItems[currentPageIndex].description,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Pagination dots
                Row(
                    modifier = Modifier
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    carouselItems.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (currentPageIndex == index) MaterialTheme.colorScheme.onSurface else Color.Gray.copy(
                                        alpha = 0.5f
                                    )
                                )
                        )
                    }
                }

                // Next/Done button
                Button(
                    onClick = {
                        if (isLastPage) {
                            onDismiss()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(currentPageIndex + 1)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isLastPage) "Done" else "Next",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

data class CarouselItem(
    val imageRes: Int,
    val title: String,
    val description: String,
    val backgroundColor: Color
)