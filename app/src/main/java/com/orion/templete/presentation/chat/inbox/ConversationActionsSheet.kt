package com.orion.templete.presentation.chat.inbox

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.MarkChatRead
import androidx.compose.material.icons.outlined.MarkChatUnread
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.orion.templete.presentation.chat.components.ChatAvatar
import com.orion.templete.presentation.chat.components.ChatPeerBubbleDark

// Sheet / dialog container: Instagram's #262626 on dark, the regular surface on light
@Composable
internal fun inboxSheetColor(): Color {
    val surface = MaterialTheme.colorScheme.surface
    return if (surface.luminance() < 0.5f) ChatPeerBubbleDark else surface
}

// Long-press actions for one conversation: mute, mark (un)read, delete for me
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConversationActionsSheet(
    row: InboxRowUi,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleUnread: () -> Unit,
    onDelete: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = inboxSheetColor(),
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChatAvatar(url = row.avatar, name = row.title, size = 44.dp, active = row.active)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "@${row.peer}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = inboxSecondaryText(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        )
        SheetAction(
            icon = if (row.muted) Icons.Outlined.Notifications else Icons.Outlined.NotificationsOff,
            label = if (row.muted) "Unmute messages" else "Mute messages",
            onClick = onToggleMute,
        )
        SheetAction(
            icon = if (row.unread) Icons.Outlined.MarkChatRead else Icons.Outlined.MarkChatUnread,
            label = if (row.unread) "Mark as read" else "Mark as unread",
            onClick = onToggleUnread,
        )
        SheetAction(
            icon = Icons.Outlined.DeleteOutline,
            label = "Delete chat",
            onClick = onDelete,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SheetAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}

// "Delete chat" only hides the conversation for me (clearedAt); the other person keeps it
@Composable
internal fun DeleteChatDialog(
    name: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = inboxSheetColor(),
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Delete chat?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Text(
                text = "This removes the chat and its messages from your inbox only. $name will still be able to see them.",
                style = MaterialTheme.typography.bodyMedium,
                color = inboxSecondaryText(),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "Delete",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
    )
}
