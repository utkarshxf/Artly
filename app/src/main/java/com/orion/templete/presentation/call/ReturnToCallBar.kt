package com.orion.templete.presentation.call

import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orion.templete.data.model.call.CallPhase
import com.orion.templete.data.model.call.CallUiState
import com.orion.templete.domain.call.CallIntents
import com.orion.templete.domain.call.CallManager

private const val TAG = "ReturnToCallBar"

/**
 * The green strip at the top of the app while a call goes on behind it: "Tap to return to call · 02:15". It takes
 * no room when there is no call, and hides while the call screen itself is on screen (also as a picture-in-picture
 * window): that screen is then the way back.
 */
@Composable
fun ReturnToCallBar(callManager: CallManager, modifier: Modifier = Modifier) {
    val state by callManager.state.collectAsStateWithLifecycle()
    val screenVisible by CallScreenPresence.visible.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val visible = state.phase.isActive && !screenVisible
    // The clock only ticks while the bar is up
    val seconds = rememberCallSeconds(
        if (visible && state.phase == CallPhase.CONNECTED) state.connectedAtElapsed else null
    )
    // The last call's wording stays while the bar slides away, when the state no longer describes a call
    val shown = remember { ReturnBarContent() }
    if (state.phase.isActive) {
        shown.label = returnLabel(state, seconds)
        shown.video = state.showsVideo
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CallColors.ReturnBar)
                .clickable(onClickLabel = "Return to call", role = Role.Button) { openCallScreen(context) }
                .heightIn(min = 40.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (shown.video) Icons.Rounded.Videocam else Icons.Rounded.Call,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = shown.label,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// Plain holder (never triggers recomposition)
private class ReturnBarContent {
    var label: String = "Tap to return to call"
    var video: Boolean = false
}

private fun returnLabel(state: CallUiState, seconds: Long?): String {
    val detail = when (state.phase) {
        CallPhase.CONNECTED -> seconds?.let { formatCallClock(it) }
        CallPhase.INCOMING_RINGING -> "Incoming call"
        // "Calling…", "Ringing…", "Connecting…", "Reconnecting…"
        else -> callStatusText(state, seconds).takeIf { it.isNotBlank() }
    }
    return if (detail == null) "Tap to return to call" else "Tap to return to call · $detail"
}

private fun openCallScreen(context: Context) {
    try {
        context.startActivity(CallIntents.show(context))
    } catch (e: RuntimeException) {
        Log.w(TAG, "Couldn't open the call screen", e)
    }
}
