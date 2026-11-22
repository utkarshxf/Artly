package com.orion.templete.presentation.call

import android.view.Display
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ScreenLockPortrait
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.orion.templete.presentation.chat.components.ChatBlue
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------------------------------- permissions

// Why a call can't go on as asked: a permission it needs was refused
@Immutable
internal data class CallPermissionNotice(
    // false = the microphone: without it there is no call at all
    val camera: Boolean,
    // Refused for good: the system no longer asks, only its settings can change it
    val permanent: Boolean,
    val peerName: String?,
)

// Short explanation after a refusal: "Open settings" when only Settings can fix it, otherwise "Try again"
@Composable
internal fun CallPermissionNoticeDialog(
    notice: CallPermissionNotice,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    val who = notice.peerName?.takeIf { it.isNotBlank() } ?: "people"
    val title = if (notice.camera) "Camera access needed" else "Microphone access needed"
    val body = when {
        notice.camera -> "To turn on your video, allow Artistry to use the camera in Settings."
        notice.permanent -> "To call $who, allow Artistry to use the microphone in Settings."
        else -> "Artistry needs the microphone so $who can hear you."
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CallColors.Sheet,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = CallColors.TextPrimary,
            )
        },
        text = {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = CallColors.TextSecondary,
            )
        },
        confirmButton = {
            TextButton(onClick = if (notice.permanent) onOpenSettings else onRetry) {
                Text(
                    text = if (notice.permanent) "Open settings" else "Try again",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = ChatBlue,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Not now",
                    style = MaterialTheme.typography.bodyLarge,
                    color = CallColors.TextPrimary,
                )
            }
        },
    )
}

// ---------------------------------------------------------------------------------------------------- lock screen

@Stable
internal class CallLockScreenHintActions(
    val onAllow: () -> Unit,
    val onDismiss: () -> Unit,
    // The card has really been in front of the user for a while: it counts as offered
    val onSeen: () -> Unit,
)

private const val HINT_SEEN_MS = 4_000L
private const val HINT_TICK_MS = 500L

/**
 * Android 14+ can withhold full-screen notifications from an app; an incoming call then shows as a small banner
 * instead of ringing over the lock screen. A one-time, dismissible offer to switch them on, shown while the first
 * outgoing call rings (there is nothing else to do in that moment). It never blocks the call.
 */
@Composable
internal fun CallLockScreenHintCard(actions: CallLockScreenHintActions, modifier: Modifier = Modifier) {
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(actions) {
        // Only time in front of the user counts: at the ear the proximity sensor has the display off
        var shown = 0L
        while (shown < HINT_SEEN_MS) {
            delay(HINT_TICK_MS)
            val resumed = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            if (resumed && view.display?.state == Display.STATE_ON) shown += HINT_TICK_MS
        }
        actions.onSeen()
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CallColors.Card)
            .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.ScreenLockPortrait,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Never miss a call",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = CallColors.TextPrimary,
            )
            Text(
                text = "Let Artistry show incoming calls on your lock screen.",
                style = MaterialTheme.typography.bodySmall,
                color = CallColors.TextSecondary,
            )
        }
        TextButton(onClick = actions.onAllow) {
            Text(
                text = "Turn on",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = ChatBlue,
            )
        }
        IconButton(onClick = actions.onDismiss, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Dismiss",
                tint = CallColors.TextSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
