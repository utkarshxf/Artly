package com.orion.templete.presentation.call

import android.os.SystemClock
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.orion.templete.data.model.call.CallEndReason
import com.orion.templete.data.model.call.CallKind
import com.orion.templete.data.model.call.CallPeer
import com.orion.templete.data.model.call.CallPhase
import com.orion.templete.data.model.call.CallUiState
import com.orion.templete.presentation.chat.components.ChatActiveGreen
import com.orion.templete.presentation.chat.components.ChatPeerBubbleDark
import com.orion.templete.presentation.ui.theme.DarkColors
import com.orion.templete.presentation.ui.theme.Shapes
import com.orion.templete.presentation.ui.theme.Typography
import kotlinx.coroutines.delay
import java.util.Locale

// Call screens are dark whatever the app theme is (like Instagram's): Artistry's dark scheme, type and shapes,
// with white as the default content colour because nothing here sits on a Material surface.
@Composable
internal fun CallTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, shapes = Shapes, typography = Typography) {
        CompositionLocalProvider(LocalContentColor provides Color.White, content = content)
    }
}

internal object CallColors {
    val Background = Color(0xFF0A0A0C)
    val TextPrimary = Color.White
    val TextSecondary = Color.White.copy(alpha = 0.72f)

    // Round buttons: translucent while idle, solid white once the toggle is engaged (muted, speaker on, camera off)
    val Control = Color.White.copy(alpha = 0.18f)
    val ControlEngaged = Color.White
    val OnControl = Color.White
    val OnControlEngaged = Color(0xFF111111)

    val Accept = ChatActiveGreen
    val Decline = Color(0xFFFF3B30)

    // Dialogs and cards: the chat's dark bubble grey
    val Sheet = ChatPeerBubbleDark
    val Card = Color(0xF2262626)

    // Opaque on purpose, see CallVideoTile
    val TileFrame = Color(0xFF1C1C1E)

    // Darker than the "active now" green so white text stays readable on it
    val ReturnBar = Color(0xFF1C9A4B)

    // Behind every call: Artistry's wine accent fading into black
    val Backdrop = Brush.verticalGradient(listOf(Color(0xFF4A0D22), Color(0xFF1B070E), Color(0xFF050505)))
}

internal object CallDimens {
    val TopBarHeight = 56.dp

    // Controls row of the video layout including its paddings; the floating tile keeps clear of it
    val VideoControlsHeight = 88.dp

    val TileWidth = 104.dp
    val TileHeight = 156.dp
    val TileRadius = 14.dp
    val TileFrame = 4.5.dp
    val TileInnerRadius = 10.dp
}

// "02:15", and "1:02:15" from the first hour on
internal fun formatCallClock(seconds: Long): String {
    val total = seconds.coerceAtLeast(0L)
    val hours = total / 3600L
    val minutes = (total % 3600L) / 60L
    val secs = total % 60L
    return if (hours > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, secs)
    }
}

// Seconds since both sides were connected, ticking on the second; null while there is nothing to count
@Composable
internal fun rememberCallSeconds(connectedAtElapsed: Long?): Long? {
    val seconds by produceState<Long?>(connectedAtElapsed?.let { secondsSince(it) }, connectedAtElapsed) {
        if (connectedAtElapsed == null) {
            value = null
            return@produceState
        }
        while (true) {
            val elapsed = (SystemClock.elapsedRealtime() - connectedAtElapsed).coerceAtLeast(0L)
            value = elapsed / 1000L
            delay(1000L - elapsed % 1000L)
        }
    }
    return seconds
}

private fun secondsSince(elapsedRealtime: Long): Long =
    (SystemClock.elapsedRealtime() - elapsedRealtime).coerceAtLeast(0L) / 1000L

// The one line under the name
internal fun callStatusText(state: CallUiState, seconds: Long?): String = when (state.phase) {
    CallPhase.IDLE -> ""
    CallPhase.OUTGOING_STARTING -> "Calling…"
    CallPhase.OUTGOING_RINGING -> "Ringing…"
    CallPhase.INCOMING_RINGING -> if (state.kind == CallKind.VIDEO) "Video call" else "Audio call"
    CallPhase.CONNECTING -> "Connecting…"
    CallPhase.CONNECTED -> if (seconds != null) formatCallClock(seconds) else "Connected"
    CallPhase.RECONNECTING -> "Reconnecting…"
    CallPhase.ENDED -> callEndedText(state)
}

// Why the call is over, for the short moment the screen stays up
internal fun callEndedText(state: CallUiState): String = when (state.endReason) {
    CallEndReason.HUNG_UP, CallEndReason.REMOTE_HUNG_UP, null -> "Call ended"
    CallEndReason.DECLINED -> "Declined"
    CallEndReason.BUSY -> "On another call"
    CallEndReason.NO_ANSWER -> "No answer"
    CallEndReason.MISSED -> "Missed call"
    CallEndReason.ANSWERED_ELSEWHERE -> "Answered on another device"
    CallEndReason.CONNECTION_LOST -> "Connection lost"
    CallEndReason.FAILED -> state.message?.takeIf { it.isNotBlank() } ?: "Couldn't connect the call"
}

internal fun callPeerName(peer: CallPeer?): String =
    peer?.displayName?.trim()?.takeIf { it.isNotEmpty() } ?: "Artistry user"

// "Maya" from "Maya Lin", for short hints such as "Maya is muted"
internal fun callPeerFirstName(peer: CallPeer?): String? =
    peer?.displayName?.trim()?.substringBefore(' ')?.takeIf { it.isNotEmpty() }
