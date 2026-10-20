package com.orion.templete.presentation.chat.thread

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.MissedVideoCall
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * A call in the thread, Instagram-style: a compact card on the caller's side that says what happened and when,
 * with a button that places the same kind of call again ("Call back" / "Call again").
 *
 * The card is inert apart from that button: no long-press menu, no reactions, no swipe-to-reply (the row gives it
 * no gestures). `onAction` = null hides the button (calls are switched off, or the row doesn't know its kind).
 */
@Composable
fun ThreadCallCard(
    call: ThreadCallUi,
    colors: ThreadColors,
    onAction: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    // A call I didn't take is red, like Instagram's missed calls; everything else keeps the bubble's colours
    val accent = if (call.missed) colors.error else colors.peerText
    val icon = when {
        call.missed && call.video -> Icons.AutoMirrored.Filled.MissedVideoCall
        call.missed -> Icons.AutoMirrored.Filled.CallMissed
        call.video -> Icons.Filled.Videocam
        else -> Icons.Filled.Call
    }
    Column(
        modifier = modifier
            .width(ThreadDimens.CallWidth)
            .clip(RoundedCornerShape(ThreadDimens.ImageRadius))
            .background(colors.peerBubble)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (call.missed) colors.error.copy(alpha = 0.14f) else colors.placeholder),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = call.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = call.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        val action = call.action
        if (onAction != null && action != null) {
            Spacer(Modifier.height(10.dp))
            // Same button as the shared-profile card's "View profile"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.placeholder)
                    .clickable(
                        onClickLabel = if (call.video) "Start a video call" else "Start an audio call",
                        role = Role.Button,
                        onClick = onAction,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = action,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.peerText,
                    maxLines = 1,
                )
            }
        }
    }
}
