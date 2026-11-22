package com.orion.templete.presentation.call

import android.view.SurfaceView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.orion.templete.domain.call.CallManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import kotlin.math.roundToInt

internal enum class CallVideoStream { LOCAL, REMOTE }

/**
 * Which SurfaceView each stream is drawn into right now. The manager holds one view per stream, and this keeps it
 * to exactly that across the moments where an old view goes away AFTER its replacement was handed over (swapping
 * big / small, a second copy of the screen): a view that is no longer the current one must not take the stream
 * away from its successor when it is released.
 */
internal object CallVideoViews {
    private var local: WeakReference<SurfaceView>? = null
    private var remote: WeakReference<SurfaceView>? = null

    fun bind(manager: CallManager, stream: CallVideoStream, view: SurfaceView) {
        when (stream) {
            CallVideoStream.LOCAL -> {
                local = WeakReference(view)
                manager.setLocalVideoView(view)
            }
            CallVideoStream.REMOTE -> {
                remote = WeakReference(view)
                manager.setRemoteVideoView(view)
            }
        }
    }

    fun release(manager: CallManager, stream: CallVideoStream, view: SurfaceView) {
        when (stream) {
            CallVideoStream.LOCAL -> {
                if (local?.get() === view) {
                    local = null
                    manager.setLocalVideoView(null)
                }
            }
            CallVideoStream.REMOTE -> {
                if (remote?.get() === view) {
                    remote = null
                    manager.setRemoteVideoView(null)
                }
            }
        }
    }
}

/**
 * One video stream. The SurfaceView is created here and handed to the manager; it is taken back (null) when it
 * leaves the screen. Nothing above it in the tree may use alpha or an off-screen layer: a surface sits behind the
 * window and only shows through a hole punched straight into it.
 */
@Composable
internal fun CallVideoSurface(
    stream: CallVideoStream,
    mediaOverlay: Boolean,
    callManager: CallManager,
    modifier: Modifier = Modifier,
) {
    // A surface's place in the Z order is fixed once it is attached, and a stream draws into exactly one view:
    // another stream or another layer means a new SurfaceView, never a reused one
    key(stream, mediaOverlay) {
        AndroidView(
            factory = { context ->
                SurfaceView(context).also { view ->
                    // The small tile floats over the full-screen video; both stay under the controls
                    if (mediaOverlay) view.setZOrderMediaOverlay(true)
                    CallVideoViews.bind(callManager, stream, view)
                }
            },
            modifier = modifier,
            onRelease = { view -> CallVideoViews.release(callManager, stream, view) },
        )
    }
}

private const val SURFACE_REVEAL_DELAY_MS = 350L
private const val SURFACE_REVEAL_FADE_MS = 260

/**
 * A new surface is black until its first frame lands. Whatever is drawn UNDER a SurfaceView is punched out the
 * moment the surface exists, so the cover goes on top of it: it holds for a moment, then fades into the video.
 * Compose it right after the surface, inside the same `key`.
 */
@Composable
internal fun CallSurfaceReveal(content: @Composable () -> Unit) {
    val cover = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        delay(SURFACE_REVEAL_DELAY_MS)
        cover.animateTo(0f, tween(durationMillis = SURFACE_REVEAL_FADE_MS))
    }
    val covering by remember { derivedStateOf { cover.value > 0f } }
    if (covering) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = cover.value },
        ) {
            content()
        }
    }
}

internal enum class CallTileCorner { TopLeft, TopRight, BottomLeft, BottomRight }

// The room the tile has to stay inside: px to keep clear of each edge of the screen
@Immutable
internal data class CallTileBounds(val left: Int, val top: Int, val right: Int, val bottom: Int)

// Plain holder: the finger's position during a drag, updated on every pointer event without waiting for a frame
private class TileDrag {
    var active = false
    var position = Offset.Zero
}

private val TileSpring = spring<Offset>(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)

/**
 * The small floating picture (yourself, or the other person after a swap). It can be dragged anywhere and settles
 * in the nearest corner; a tap is reported to the screen (swap big / small).
 *
 * Its SurfaceView is a media overlay: above the full-screen video's surface but, like every surface, still BEHIND
 * the window. Rounded corners can therefore not be clipped out of it where it floats over the other video: the
 * hole of the big view would show the surface's square corners through. So the tile is an opaque rounded frame
 * and the video sits inside it, far enough from the edge that the frame covers those corners
 * (frame >= 0.3 x corner radius).
 */
@Composable
internal fun CallVideoTile(
    container: IntSize,
    bounds: CallTileBounds,
    corner: CallTileCorner,
    onCornerChange: (CallTileCorner) -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    val tile = with(density) { IntSize(CallDimens.TileWidth.roundToPx(), CallDimens.TileHeight.roundToPx()) }
    val target = remember(corner, container, bounds, tile) { tileCornerOffset(corner, container, bounds, tile) }
    val position = remember { Animatable(target, Offset.VectorConverter) }
    val drag = remember { TileDrag() }
    val scope = rememberCoroutineScope()

    val currentContainer by rememberUpdatedState(container)
    val currentBounds by rememberUpdatedState(bounds)
    val currentTile by rememberUpdatedState(tile)
    val currentTarget by rememberUpdatedState(target)
    val currentOnCornerChange by rememberUpdatedState(onCornerChange)
    val currentOnTap by rememberUpdatedState(onTap)

    // A new corner, or the bars sliding in / out, moves the tile; during a drag the finger decides
    LaunchedEffect(target) {
        if (!drag.active) position.animateTo(target, TileSpring)
    }

    Box(
        modifier = modifier
            // Laid out at its position (not merely drawn there), so the surface follows on every Android version
            .absoluteOffset { IntOffset(position.value.x.roundToInt(), position.value.y.roundToInt()) }
            .size(CallDimens.TileWidth, CallDimens.TileHeight)
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(CallDimens.TileRadius))
            .background(CallColors.TileFrame)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { currentOnTap() })
            }
            // After the tap detector on purpose: this one sees a move first and consumes it, which cancels the tap
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        drag.active = true
                        drag.position = position.value
                    },
                    onDragEnd = {
                        drag.active = false
                        val next = nearestTileCorner(drag.position, currentContainer, currentTile)
                        currentOnCornerChange(next)
                        val settle = tileCornerOffset(next, currentContainer, currentBounds, currentTile)
                        scope.launch { position.animateTo(settle, TileSpring) }
                    },
                    onDragCancel = {
                        drag.active = false
                        val settle = currentTarget
                        scope.launch { position.animateTo(settle, TileSpring) }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val next = clampTile(drag.position + dragAmount, currentContainer, currentBounds, currentTile)
                        drag.position = next
                        scope.launch { position.snapTo(next) }
                    },
                )
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(CallDimens.TileFrame)
                .clip(RoundedCornerShape(CallDimens.TileInnerRadius))
                .background(Color.Black),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

// Top-left position of the tile in each corner of the room it has
private fun tileCornerOffset(corner: CallTileCorner, container: IntSize, bounds: CallTileBounds, tile: IntSize): Offset {
    val left = bounds.left.toFloat()
    val top = bounds.top.toFloat()
    val right = (container.width - tile.width - bounds.right).toFloat().coerceAtLeast(left)
    val bottom = (container.height - tile.height - bounds.bottom).toFloat().coerceAtLeast(top)
    return when (corner) {
        CallTileCorner.TopLeft -> Offset(left, top)
        CallTileCorner.TopRight -> Offset(right, top)
        CallTileCorner.BottomLeft -> Offset(left, bottom)
        CallTileCorner.BottomRight -> Offset(right, bottom)
    }
}

private fun clampTile(position: Offset, container: IntSize, bounds: CallTileBounds, tile: IntSize): Offset {
    val left = bounds.left.toFloat()
    val top = bounds.top.toFloat()
    val right = (container.width - tile.width - bounds.right).toFloat().coerceAtLeast(left)
    val bottom = (container.height - tile.height - bounds.bottom).toFloat().coerceAtLeast(top)
    return Offset(position.x.coerceIn(left, right), position.y.coerceIn(top, bottom))
}

// The quarter of the screen the tile's centre was dropped in
private fun nearestTileCorner(position: Offset, container: IntSize, tile: IntSize): CallTileCorner {
    val onLeft = position.x + tile.width / 2f < container.width / 2f
    val onTop = position.y + tile.height / 2f < container.height / 2f
    return when {
        onTop && onLeft -> CallTileCorner.TopLeft
        onTop -> CallTileCorner.TopRight
        onLeft -> CallTileCorner.BottomLeft
        else -> CallTileCorner.BottomRight
    }
}
