package com.orion.templete.presentation.chat.thread

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import kotlin.math.abs

// Photo opened from a bubble
@Immutable
data class ThreadViewerImage(
    val image: ThreadImageUi,
    val title: String,
    val subtitle: String,
)

private const val MAX_SCALE = 5f
private const val DOUBLE_TAP_SCALE = 2.5f

// Full-screen photo viewer: pinch / double-tap zoom, pan when zoomed, drag down (or up) to close.
@Composable
fun ThreadImageViewer(image: ThreadViewerImage, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        ViewerContent(image = image, onDismiss = onDismiss)
    }
}

@Composable
private fun ViewerContent(image: ThreadViewerImage, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var container by remember { mutableStateOf(IntSize.Zero) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val dismissOffset = remember { Animatable(0f) }
    var chromeVisible by remember { mutableStateOf(true) }

    val request = remember(image.image) {
        ImageRequest.Builder(context)
            .data(image.image.model)
            .crossfade(true)
            .apply {
                image.image.memoryKey?.let { memoryCacheKey(it) }
                // Show whatever is already in memory (the bubble's copy) while the full image loads
                placeholderMemoryCacheKey(image.image.memoryKey ?: image.image.placeholderKey)
            }
            .build()
    }

    fun clamp(value: Offset, zoom: Float): Offset {
        val maxX = (container.width * (zoom - 1f)) / 2f
        val maxY = (container.height * (zoom - 1f)) / 2f
        return Offset(value.x.coerceIn(-maxX, maxX), value.y.coerceIn(-maxY, maxY))
    }

    fun animateZoom(targetScale: Float, targetOffset: Offset) {
        val startScale = scale
        val startOffset = offset
        scope.launch {
            animate(0f, 1f, animationSpec = tween(durationMillis = 260)) { t, _ ->
                scale = startScale + (targetScale - startScale) * t
                offset = Offset(
                    startOffset.x + (targetOffset.x - startOffset.x) * t,
                    startOffset.y + (targetOffset.y - startOffset.y) * t,
                )
            }
        }
    }

    val dragging by remember { derivedStateOf { dismissOffset.value != 0f } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { container = it },
    ) {
        // Backdrop that fades while dragging to close (the chat shows through)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val progress = if (size.height > 0f) abs(dismissOffset.value) / (size.height * 0.6f) else 0f
                    alpha = (1f - progress).coerceIn(0f, 1f)
                }
                .background(Color.Black)
        )
        SubcomposeAsyncImage(
            model = request,
            contentDescription = image.title.ifBlank { "Photo" },
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { chromeVisible = !chromeVisible },
                        onDoubleTap = { tap ->
                            if (scale > 1.05f) {
                                animateZoom(1f, Offset.Zero)
                            } else {
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val target = (tap - center) * (1f - DOUBLE_TAP_SCALE)
                                animateZoom(DOUBLE_TAP_SCALE, clamp(target, DOUBLE_TAP_SCALE))
                            }
                        },
                    )
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var dragging = false
                        var dismissing = false
                        var accumulatedPan = Offset.Zero
                        var accumulatedZoom = 1f
                        val slop = viewConfiguration.touchSlop
                        do {
                            val event = awaitPointerEvent()
                            if (event.changes.any { it.isConsumed }) break
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            val pointers = event.changes.count { it.pressed }
                            if (!dragging) {
                                accumulatedPan += panChange
                                accumulatedZoom *= zoomChange
                                val zoomMotion = abs(1f - accumulatedZoom) * (size.width / 2f)
                                if (pointers > 1 || zoomMotion > slop || accumulatedPan.getDistance() > slop) {
                                    dragging = true
                                    dismissing = pointers == 1 && scale <= 1.01f &&
                                        abs(accumulatedPan.y) > abs(accumulatedPan.x)
                                }
                            }
                            if (dragging) {
                                if (dismissing) {
                                    val next = dismissOffset.value + panChange.y
                                    scope.launch { dismissOffset.snapTo(next) }
                                } else {
                                    val centroid = event.calculateCentroid(useCurrent = false)
                                    val newScale = (scale * zoomChange).coerceIn(1f, MAX_SCALE)
                                    val factor = newScale / scale
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val focus = if (centroid == Offset.Unspecified) center else centroid
                                    val moved = offset * factor + (focus - center) * (1f - factor) + panChange
                                    scale = newScale
                                    offset = clamp(moved, newScale)
                                }
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            }
                        } while (event.changes.any { it.pressed })

                        if (dismissing) {
                            val threshold = size.height * 0.18f
                            if (abs(dismissOffset.value) > threshold) {
                                onDismiss()
                            } else {
                                scope.launch { dismissOffset.animateTo(0f, spring(dampingRatio = 0.8f)) }
                            }
                        } else if (scale <= 1.01f && offset != Offset.Zero) {
                            animateZoom(1f, Offset.Zero)
                        }
                    }
                }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y + dismissOffset.value
                },
        ) {
            val state = painter.state
            if (state is AsyncImagePainter.State.Loading && image.image.memoryKey == null && image.image.placeholderKey == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 2.5.dp,
                        color = Color.White,
                    )
                }
            } else if (state is AsyncImagePainter.State.Error) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Couldn't load this photo",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                SubcomposeAsyncImageContent()
            }
        }

        AnimatedVisibility(
            visible = chromeVisible && !dragging,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopStart),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                }
                Spacer(Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = image.title,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (image.subtitle.isNotBlank()) {
                        Text(
                            text = image.subtitle,
                            color = Color.White.copy(alpha = 0.75f),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
