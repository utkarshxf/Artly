package com.orion.templete.presentation.chat.thread

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.orion.templete.presentation.chat.components.ChatAvatar
import com.orion.templete.presentation.chat.components.ChatTime
import kotlinx.coroutines.delay

// Back, avatar (active dot) + name + presence, then Instagram's two call buttons (audio, video). While calls can't
// be offered the info button takes their place; it does what a tap on the name does, so nothing is lost when the
// call buttons replace it (three icons would squeeze the name on small phones).
@Composable
fun ThreadTopBar(
    header: ThreadHeaderUi,
    colors: ThreadColors,
    onBack: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
    showCallButtons: Boolean = false,
    onAudioCall: () -> Unit = {},
    onVideoCall: () -> Unit = {},
) {
    // Presence labels age while the screen is open
    val now by produceState(initialValue = System.currentTimeMillis(), header.lastActive) {
        while (true) {
            value = System.currentTimeMillis()
            delay(30_000L)
        }
    }
    val activeLabel = ChatTime.activeStatus(header.lastActive, now)
    val activeNow = header.lastActive != null && now - header.lastActive < 3 * 60_000L

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClickLabel = "View profile", role = Role.Button, onClick = onOpenProfile)
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChatAvatar(
                    url = header.avatar,
                    name = header.name,
                    size = 32.dp,
                    active = activeNow,
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = header.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = activeLabel ?: "@${header.username}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (showCallButtons) {
                IconButton(onClick = onAudioCall) {
                    Icon(
                        imageVector = Icons.Outlined.Call,
                        contentDescription = "Audio call",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(onClick = onVideoCall) {
                    Icon(
                        imageVector = Icons.Outlined.Videocam,
                        contentDescription = "Video call",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(26.dp),
                    )
                }
            } else {
                IconButton(onClick = onOpenProfile) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Conversation details",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        HorizontalDivider(color = colors.divider, thickness = 0.5.dp)
    }
}
