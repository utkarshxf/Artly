package com.orion.templete.presentation.call

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orion.templete.data.model.call.CallKind
import com.orion.templete.data.model.call.CallPeer
import com.orion.templete.data.model.call.CallPhase
import com.orion.templete.data.model.call.CallUiState
import com.orion.templete.domain.call.CallManager
import kotlinx.coroutines.delay

// Everything the call screen can ask for. Created once by the activity; the handlers read the manager's current
// state when they run, so the screen never hands a stale value back.
@Stable
internal class CallActions(
    val onAccept: () -> Unit,
    val onDecline: () -> Unit,
    val onHangUp: () -> Unit,
    val onToggleMute: () -> Unit,
    val onToggleSpeaker: () -> Unit,
    val onToggleCamera: () -> Unit,
    val onSwitchCamera: () -> Unit,
    val onMinimise: () -> Unit,
)

// How long the controls of a video call stay up over live video
private const val CONTROLS_TIMEOUT_MS = 4_500L

/**
 * The call screen for whatever the manager's state says:
 * - nothing placed yet (permission prompt up): the person about to be called;
 * - incoming ring: Accept / Decline;
 * - `showsVideo` (either camera on): the video layout, also while an outgoing video call rings;
 * - otherwise the audio layout (outgoing ring, connecting, in call);
 * - ENDED: one line saying why;
 * - picture-in-picture: only the remote video (the person and a status line while there is none).
 */
@Composable
internal fun CallScreen(
    state: CallUiState,
    placingPeer: CallPeer?,
    inPictureInPicture: Boolean,
    callManager: CallManager,
    actions: CallActions,
    lockScreenHint: CallLockScreenHintActions?,
) {
    val peer = state.peer ?: placingPeer
    // The clock runs once both sides are connected (and keeps counting through a reconnect)
    val counting = state.phase == CallPhase.CONNECTED || state.phase == CallPhase.RECONNECTING
    val seconds = rememberCallSeconds(if (counting) state.connectedAtElapsed else null)
    val status = callStatusText(state, seconds)
    // The lock-screen offer belongs to the wait while the other phone rings
    val hint = lockScreenHint.takeIf {
        state.phase == CallPhase.OUTGOING_STARTING || state.phase == CallPhase.OUTGOING_RINGING
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CallColors.Background),
    ) {
        when {
            state.phase == CallPhase.IDLE -> {
                // No call yet: the keyguard / permission prompt is up over the person about to be called.
                // (IDLE with nobody to call means the activity is closing: nothing to draw.)
                if (placingPeer != null) {
                    if (inPictureInPicture) {
                        CallCompactContent(peer = placingPeer, status = "")
                    } else {
                        CallBackdrop(avatar = placingPeer.avatar)
                        CallPersonLayout(
                            peer = placingPeer,
                            status = "",
                            pulsing = false,
                            onMinimise = actions.onMinimise,
                        )
                    }
                }
            }
            // A floating window has room for one thing: the other person's video once the call is on; before
            // that (still ringing) and without video, the person and the status line
            inPictureInPicture && (!state.showsVideo || !state.phase.isInCall) -> {
                CallCompactContent(peer = peer, status = status)
            }
            state.phase == CallPhase.ENDED -> {
                // Same place as the call it follows: the buttons go, the reason takes the status line
                CallBackdrop(avatar = peer?.avatar)
                CallPersonLayout(peer = peer, status = status, pulsing = false, onMinimise = null)
            }
            state.phase == CallPhase.INCOMING_RINGING -> {
                CallIncomingContent(
                    state = state,
                    peer = peer,
                    status = status,
                    callManager = callManager,
                    actions = actions,
                )
            }
            state.showsVideo -> {
                CallVideoContent(
                    state = state,
                    peer = peer,
                    status = status,
                    inPictureInPicture = inPictureInPicture,
                    callManager = callManager,
                    actions = actions,
                    lockScreenHint = hint,
                )
            }
            else -> {
                CallAudioContent(
                    state = state,
                    peer = peer,
                    status = status,
                    actions = actions,
                    lockScreenHint = hint,
                )
            }
        }
    }
}

// Outgoing ring ("Calling…" / "Ringing…"), connecting and the call itself while no camera is on
@Composable
private fun CallAudioContent(
    state: CallUiState,
    peer: CallPeer?,
    status: String,
    actions: CallActions,
    lockScreenHint: CallLockScreenHintActions?,
) {
    CallBackdrop(avatar = peer?.avatar)
    CallPersonLayout(
        peer = peer,
        status = status,
        pulsing = state.phase.isRinging,
        onMinimise = actions.onMinimise,
        banner = {
            if (lockScreenHint != null) CallLockScreenHintCard(actions = lockScreenHint)
        },
        hints = { CallHints(state = state, peer = peer) },
        bottom = { CallControls(state = state, video = false, actions = actions) },
    )
}

@Composable
private fun CallIncomingContent(
    state: CallUiState,
    peer: CallPeer?,
    status: String,
    callManager: CallManager,
    actions: CallActions,
) {
    val video = state.kind == CallKind.VIDEO
    CallBackdrop(avatar = peer?.avatar)
    if (state.localCameraOn) {
        // Should the manager already run the camera for a video call: yourself behind the ring, like Instagram
        CallVideoSurface(
            stream = CallVideoStream.LOCAL,
            mediaOverlay = false,
            callManager = callManager,
            modifier = Modifier.fillMaxSize(),
        )
        CallSurfaceReveal { CallBackdrop(avatar = peer?.avatar) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f)),
        )
    }
    CallPersonLayout(
        peer = peer,
        status = status,
        pulsing = true,
        onMinimise = actions.onMinimise,
        caption = "Incoming call",
        bottom = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top,
            ) {
                CallLabeledButton(
                    icon = Icons.Rounded.CallEnd,
                    label = "Decline",
                    container = CallColors.Decline,
                    onClick = actions.onDecline,
                )
                CallLabeledButton(
                    icon = if (video) Icons.Rounded.Videocam else Icons.Rounded.Call,
                    label = "Accept",
                    container = CallColors.Accept,
                    onClick = actions.onAccept,
                    pulsing = true,
                )
            }
        },
    )
}

/**
 * Either camera is on. One stream fills the screen, the other floats in a tile:
 * - ringing out: my own camera full screen behind a scrim, the person I'm calling on top of it;
 * - in the call: the other person full screen (their portrait while their camera is off), me in the tile; a tap
 *   on the tile swaps the two;
 * - picture-in-picture (only once the call is on): the other person's video and nothing else.
 *
 * The SurfaceViews are never wrapped in anything animated (alpha / off-screen layers hide a surface), which is why
 * the picture-in-picture layout is this same composable with its overlays left out: the remote view stays the
 * same view on the way in and out.
 */
@Composable
private fun CallVideoContent(
    state: CallUiState,
    peer: CallPeer?,
    status: String,
    inPictureInPicture: Boolean,
    callManager: CallManager,
    actions: CallActions,
    lockScreenHint: CallLockScreenHintActions?,
) {
    val inCall = state.phase.isInCall
    val bothOn = state.localCameraOn && state.remoteCameraOn
    var swapped by rememberSaveable { mutableStateOf(false) }
    var corner by rememberSaveable { mutableStateOf(CallTileCorner.TopRight) }
    // A swap only means something while both pictures exist
    LaunchedEffect(bothOn) {
        if (!bothOn) swapped = false
    }
    val swap = swapped && bothOn && inCall && !inPictureInPicture

    // Until the other person picks up there is only my own camera
    val big = if (!inCall || swap) CallVideoStream.LOCAL else CallVideoStream.REMOTE
    val small = if (big == CallVideoStream.LOCAL) CallVideoStream.REMOTE else CallVideoStream.LOCAL
    val bigOn = if (big == CallVideoStream.LOCAL) state.localCameraOn else state.remoteCameraOn
    val smallOn = if (small == CallVideoStream.LOCAL) state.localCameraOn else state.remoteCameraOn

    // Over live video the controls step aside after a few seconds; a tap brings them back. They stay while there
    // is something to read (connecting, reconnecting, the other camera off).
    var controlsShown by remember { mutableStateOf(true) }
    var touches by remember { mutableIntStateOf(0) }
    val autoHide = state.phase == CallPhase.CONNECTED && bigOn && !inPictureInPicture
    val controlsVisible = controlsShown || !autoHide
    val currentAutoHide by rememberUpdatedState(autoHide)
    val accessibility = LocalAccessibilityManager.current
    LaunchedEffect(autoHide, controlsShown, touches) {
        if (autoHide && controlsShown) {
            // Longer (or never) for people who use a screen reader or switch access
            val timeout = accessibility?.calculateRecommendedTimeoutMillis(CONTROLS_TIMEOUT_MS, true, false, true)
                ?: CONTROLS_TIMEOUT_MS
            delay(timeout)
            controlsShown = false
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val container = IntSize(constraints.maxWidth, constraints.maxHeight)

        // The person, under the video: what shows while a camera is off and what a leaving picture falls back to
        CallBackdrop(avatar = peer?.avatar)
        if (bigOn) {
            key(big) {
                CallVideoSurface(
                    stream = big,
                    mediaOverlay = false,
                    callManager = callManager,
                    modifier = Modifier.fillMaxSize(),
                )
                CallSurfaceReveal { CallBackdrop(avatar = peer?.avatar) }
            }
        }

        when {
            inPictureInPicture -> {
                // The floating window: the picture and nothing else
                if (!bigOn) {
                    CallAvatar(
                        peer = peer,
                        size = 56.dp,
                        pulsing = false,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
            !inCall -> {
                // Ringing out on a video call: myself behind a scrim, the person I'm calling in front
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.42f)),
                )
                CallPersonLayout(
                    peer = peer,
                    status = status,
                    pulsing = true,
                    onMinimise = actions.onMinimise,
                    banner = {
                        if (lockScreenHint != null) CallLockScreenHintCard(actions = lockScreenHint)
                    },
                    bottom = { CallControls(state = state, video = true, actions = actions) },
                )
            }
            else -> {
                if (!bigOn) {
                    // Their camera is off (or not there yet): the person instead of the picture
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CallAvatar(peer = peer, size = 116.dp, pulsing = state.phase == CallPhase.CONNECTING)
                        if (state.phase == CallPhase.CONNECTED) {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "Camera off",
                                color = CallColors.TextSecondary,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                // A tap anywhere on the picture shows / hides the controls
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    controlsShown = if (currentAutoHide) !controlsShown else true
                                    touches++
                                },
                            )
                        },
                )

                AnimatedVisibility(visible = controlsVisible, enter = fadeIn(), exit = fadeOut()) {
                    CallVideoScrims()
                }

                // The tile keeps clear of the system bars, the cut-out and whichever bars are showing
                val density = LocalDensity.current
                val direction = LocalLayoutDirection.current
                val insets = WindowInsets.safeDrawing
                val bounds = with(density) {
                    val edge = 12.dp.roundToPx()
                    val topBar = if (controlsVisible) CallDimens.TopBarHeight.roundToPx() else 0
                    val controls = if (controlsVisible) CallDimens.VideoControlsHeight.roundToPx() else 0
                    CallTileBounds(
                        left = insets.getLeft(this, direction) + edge,
                        top = insets.getTop(this) + topBar + edge,
                        right = insets.getRight(this, direction) + edge,
                        bottom = insets.getBottom(this) + controls + edge,
                    )
                }
                CallVideoTile(
                    container = container,
                    bounds = bounds,
                    corner = corner,
                    onCornerChange = { corner = it },
                    onTap = {
                        if (bothOn) swapped = !swapped
                        controlsShown = true
                        touches++
                    },
                    modifier = Modifier.align(AbsoluteAlignment.TopLeft),
                ) {
                    if (smallOn) {
                        CallVideoSurface(
                            stream = small,
                            mediaOverlay = true,
                            callManager = callManager,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.VideocamOff,
                            contentDescription = "Your camera is off",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }

                AnimatedVisibility(
                    visible = controlsVisible,
                    modifier = Modifier.align(Alignment.TopCenter),
                    enter = fadeIn() + slideInVertically { -it / 2 },
                    exit = fadeOut() + slideOutVertically { -it / 2 },
                ) {
                    CallVideoTopBar(name = callPeerName(peer), status = status, onMinimise = actions.onMinimise)
                }

                // Hints stay up with the controls hidden; they sit beside the tile, never under it
                val hintsAlignment = when (corner) {
                    CallTileCorner.TopRight -> AbsoluteAlignment.TopLeft
                    CallTileCorner.TopLeft -> AbsoluteAlignment.TopRight
                    CallTileCorner.BottomLeft, CallTileCorner.BottomRight -> Alignment.TopCenter
                }
                val hintsSide = when (corner) {
                    CallTileCorner.TopRight -> AbsoluteAlignment.Left
                    CallTileCorner.TopLeft -> AbsoluteAlignment.Right
                    CallTileCorner.BottomLeft, CallTileCorner.BottomRight -> Alignment.CenterHorizontally
                }
                CallHints(
                    state = state,
                    peer = peer,
                    modifier = Modifier
                        .align(hintsAlignment)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(start = 16.dp, end = 16.dp, top = CallDimens.TopBarHeight + 8.dp),
                    horizontalAlignment = hintsSide,
                )

                AnimatedVisibility(
                    visible = controlsVisible,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 },
                ) {
                    CallControls(
                        state = state,
                        video = true,
                        actions = actions,
                        modifier = Modifier
                            .windowInsetsPadding(
                                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                            )
                            .padding(top = 12.dp, bottom = 24.dp),
                        onInteraction = { touches++ },
                    )
                }
            }
        }
    }
}

// Minimise chevron, name and the timer over the top of the picture
@Composable
private fun CallVideoTopBar(name: String, status: String, onMinimise: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top))
            .height(CallDimens.TopBarHeight)
            .padding(start = 4.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CallMinimiseButton(onClick = onMinimise)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 2.dp),
        ) {
            Text(
                text = name,
                color = CallColors.TextPrimary,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = status,
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// Darkens the top and bottom of the picture so the white controls read on any video
@Composable
private fun CallVideoScrims() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(150.dp)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent))),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(230.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)))),
        )
    }
}
