package com.orion.templete.presentation.call

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.orion.templete.data.model.call.AudioRoute
import com.orion.templete.data.model.call.CallPeer
import com.orion.templete.data.model.call.CallPhase
import com.orion.templete.data.model.call.CallUiState
import com.orion.templete.presentation.chat.components.ChatAvatar

// ---------------------------------------------------------------------------------------------------- backdrop

private val BackdropSource = 96.dp
private const val BACKDROP_PIXELS = 160

// What every call sits on: Artistry's wine-to-black gradient and, where blur exists (Android 12+), the person's
// photo blurred beyond recognition over it, like Instagram's call screens.
@Composable
internal fun CallBackdrop(avatar: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CallColors.Backdrop),
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !avatar.isNullOrBlank()) {
            BlurredAvatar(url = avatar)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
        )
    }
}

@Composable
private fun BlurredAvatar(url: String) {
    val context = LocalContext.current
    val request = remember(url) {
        // A thumbnail is plenty: it ends up blurred and stretched over the whole screen
        ImageRequest.Builder(context).data(url).size(BACKDROP_PIXELS).crossfade(true).build()
    }
    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Blurring a small square and scaling the result up costs a fraction of blurring the full screen
        val scale = (max(maxWidth, maxHeight) / BackdropSource) * 1.1f
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(BackdropSource)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .blur(14.dp),
        )
    }
}

// ---------------------------------------------------------------------------------------------------- person

private const val PULSE_MS = 2_400

// Soft rings that spread from the portrait while a phone is ringing
@Composable
internal fun CallPulse(color: Color, modifier: Modifier = Modifier, spread: Float = 0.6f) {
    val transition = rememberInfiniteTransition(label = "callPulse")
    val first = transition.pulseWave(delayMillis = 0)
    val second = transition.pulseWave(delayMillis = PULSE_MS / 3)
    val third = transition.pulseWave(delayMillis = 2 * PULSE_MS / 3)
    Box(
        modifier = modifier.drawBehind {
            // The waves are read here, so a tick only redraws the rings
            pulseRing(color, spread, first.value)
            pulseRing(color, spread, second.value)
            pulseRing(color, spread, third.value)
        },
    )
}

// One ring: it starts at the portrait's edge and fades as it grows
private fun DrawScope.pulseRing(color: Color, spread: Float, wave: Float) {
    drawCircle(
        color = color.copy(alpha = 0.22f * (1f - wave)),
        radius = size.minDimension / 2f * (1f + spread * wave),
    )
}

@Composable
private fun InfiniteTransition.pulseWave(delayMillis: Int): State<Float> = animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = PULSE_MS, easing = LinearOutSlowInEasing),
        repeatMode = RepeatMode.Restart,
        initialStartOffset = StartOffset(delayMillis),
    ),
    label = "callPulseWave",
)

@Composable
internal fun CallAvatar(peer: CallPeer?, size: Dp, pulsing: Boolean, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        if (pulsing) CallPulse(color = Color.White, modifier = Modifier.matchParentSize())
        ChatAvatar(
            url = peer?.avatar,
            name = callPeerName(peer),
            size = size,
            modifier = Modifier.border(1.5.dp, Color.White.copy(alpha = 0.16f), CircleShape),
        )
    }
}

/**
 * The layout every "person" screen shares: minimise chevron, portrait, name, one status line, hints and whatever
 * sits at the bottom (controls, or Accept / Decline). Used for the incoming ring, an outgoing ring, an audio call
 * and the "call ended" moment, so the portrait never jumps between them.
 */
@Composable
internal fun CallPersonLayout(
    peer: CallPeer?,
    status: String,
    pulsing: Boolean,
    onMinimise: (() -> Unit)?,
    modifier: Modifier = Modifier,
    caption: String? = null,
    banner: @Composable () -> Unit = {},
    hints: @Composable () -> Unit = {},
    bottom: @Composable () -> Unit = {},
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        // Short windows (small phones, split screen): a smaller portrait keeps the buttons on screen
        val compact = maxHeight < 560.dp
        val avatarSize = if (compact) 96.dp else 140.dp
        val name = callPeerName(peer)
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CallDimens.TopBarHeight),
                contentAlignment = Alignment.Center,
            ) {
                if (onMinimise != null) {
                    CallMinimiseButton(
                        onClick = onMinimise,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 4.dp),
                    )
                }
                if (caption != null) {
                    Text(
                        text = caption,
                        color = CallColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, letterSpacing = 0.4.sp),
                        maxLines = 1,
                    )
                }
            }
            banner()
            Spacer(Modifier.weight(if (compact) 0.3f else 0.62f))
            CallAvatar(peer = peer, size = avatarSize, pulsing = pulsing)
            Spacer(Modifier.height(if (compact) 16.dp else 28.dp))
            Text(
                text = name,
                color = CallColors.TextPrimary,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp, lineHeight = 32.sp),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                // Always there, so the name doesn't shift when the first status arrives
                text = status.ifEmpty { " " },
                color = CallColors.TextSecondary,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
            Spacer(Modifier.height(14.dp))
            hints()
            Spacer(Modifier.weight(1f))
            bottom()
            Spacer(Modifier.height(if (compact) 16.dp else 40.dp))
        }
    }
}

// The whole call in a picture-in-picture window when there is no video to show: just the person
@Composable
internal fun CallCompactContent(peer: CallPeer?, status: String) {
    CallBackdrop(avatar = peer?.avatar)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CallAvatar(peer = peer, size = 56.dp, pulsing = false)
        if (status.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = status,
                color = CallColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------------- buttons

@Composable
internal fun CallMinimiseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = Icons.Rounded.KeyboardArrowDown,
            contentDescription = "Minimise call",
            tint = Color.White,
            modifier = Modifier.size(32.dp),
        )
    }
}

// Round call button. `engaged` = the toggle is on (muted, speaker on, camera off): solid white, like Instagram.
@Composable
internal fun CallControlButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    engaged: Boolean = false,
    enabled: Boolean = true,
    container: Color = if (engaged) CallColors.ControlEngaged else CallColors.Control,
    tint: Color = if (engaged) CallColors.OnControlEngaged else CallColors.OnControl,
    size: Dp = 56.dp,
    iconSize: Dp = 26.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (pressed) 0.9f else 1f, label = "callButtonPress")
    val containerColor by animateColorAsState(targetValue = container, label = "callButtonContainer")
    val tintColor by animateColorAsState(targetValue = tint, label = "callButtonTint")
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.4f
            }
            .clip(CircleShape)
            .background(containerColor)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tintColor,
            modifier = Modifier.size(iconSize),
        )
    }
}

// Accept / Decline on the incoming ring: a large coloured button with its name underneath
@Composable
internal fun CallLabeledButton(
    icon: ImageVector,
    label: String,
    container: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    pulsing: Boolean = false,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            if (pulsing) CallPulse(color = container, modifier = Modifier.matchParentSize(), spread = 0.42f)
            CallControlButton(
                icon = icon,
                contentDescription = label,
                onClick = onClick,
                container = container,
                tint = Color.White,
                size = 72.dp,
                iconSize = 32.dp,
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = label,
            color = CallColors.TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            // The button already carries the name for screen readers
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}

/**
 * The row of in-call buttons. Audio layout: route, camera, mute, end. Video layout: flip, camera, mute, route,
 * end. `onInteraction` lets the video layout restart its auto-hide timer on every press.
 */
@Composable
internal fun CallControls(
    state: CallUiState,
    video: Boolean,
    actions: CallActions,
    modifier: Modifier = Modifier,
    onInteraction: () -> Unit = {},
) {
    val size = if (video) 52.dp else 60.dp
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(if (video) 14.dp else 22.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (video) {
            CallControlButton(
                icon = Icons.Rounded.Cameraswitch,
                contentDescription = "Switch camera",
                onClick = {
                    onInteraction()
                    actions.onSwitchCamera()
                },
                // Nothing to flip while my camera is off
                enabled = state.localCameraOn,
                size = size,
            )
        } else {
            RouteButton(state = state, size = size, onClick = actions.onToggleSpeaker)
        }
        CameraButton(
            state = state,
            video = video,
            size = size,
            onClick = {
                onInteraction()
                actions.onToggleCamera()
            },
        )
        CallControlButton(
            icon = if (state.micMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
            contentDescription = if (state.micMuted) "Unmute microphone" else "Mute microphone",
            onClick = {
                onInteraction()
                actions.onToggleMute()
            },
            engaged = state.micMuted,
            size = size,
        )
        if (video) {
            RouteButton(
                state = state,
                size = size,
                onClick = {
                    onInteraction()
                    actions.onToggleSpeaker()
                },
            )
        }
        CallControlButton(
            icon = Icons.Rounded.CallEnd,
            contentDescription = "End call",
            onClick = actions.onHangUp,
            container = CallColors.Decline,
            tint = Color.White,
            size = size,
            iconSize = 28.dp,
        )
    }
}

// Shows where the sound really goes (the manager reports the route the system picked); a tap moves it to the
// speaker, or from the speaker back to the earpiece / headset.
@Composable
private fun RouteButton(state: CallUiState, size: Dp, onClick: () -> Unit) {
    val icon = when (state.audioRoute) {
        AudioRoute.BLUETOOTH -> Icons.Rounded.BluetoothAudio
        AudioRoute.WIRED_HEADSET -> Icons.Rounded.Headset
        AudioRoute.SPEAKER, AudioRoute.EARPIECE -> Icons.AutoMirrored.Rounded.VolumeUp
    }
    val description = when (state.audioRoute) {
        AudioRoute.SPEAKER -> "Speaker is on. Turn speaker off"
        AudioRoute.EARPIECE -> "Turn speaker on"
        AudioRoute.BLUETOOTH -> "Sound is on Bluetooth. Switch to speaker"
        AudioRoute.WIRED_HEADSET -> "Sound is on the headset. Switch to speaker"
    }
    CallControlButton(
        icon = icon,
        contentDescription = description,
        onClick = onClick,
        engaged = state.audioRoute == AudioRoute.SPEAKER,
        size = size,
    )
}

// Audio layout: "start video". Video layout: my camera on / off (off stands out, the other person can't see me).
@Composable
private fun CameraButton(state: CallUiState, video: Boolean, size: Dp, onClick: () -> Unit) {
    val off = video && !state.localCameraOn
    CallControlButton(
        icon = if (off) Icons.Rounded.VideocamOff else Icons.Rounded.Videocam,
        contentDescription = if (state.localCameraOn) "Turn camera off" else "Turn camera on",
        onClick = onClick,
        engaged = off,
        size = size,
    )
}

// ---------------------------------------------------------------------------------------------------- hints

@Composable
internal fun CallPill(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// What the other side can't tell you by itself: their microphone is off, or my connection is struggling
@Composable
internal fun CallHints(
    state: CallUiState,
    peer: CallPeer?,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
) {
    // Only once both sides are in: before that there is nobody to be muted
    val remoteMuted = state.remoteMicMuted &&
        (state.phase == CallPhase.CONNECTED || state.phase == CallPhase.RECONNECTING)
    // "Reconnecting…" already says it
    val weakNetwork = state.weakNetwork && state.phase.isInCall && state.phase != CallPhase.RECONNECTING
    if (remoteMuted || weakNetwork) {
        Column(
            modifier = modifier,
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (weakNetwork) CallPill(icon = Icons.Rounded.NetworkCheck, text = "Poor connection")
            if (remoteMuted) {
                val who = callPeerFirstName(peer)
                CallPill(icon = Icons.Rounded.MicOff, text = if (who != null) "$who is muted" else "Muted")
            }
        }
    }
}
