package com.orion.templete.presentation.chat.thread

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orion.templete.presentation.chat.components.ChatBlue
import com.orion.templete.presentation.chat.components.ChatOwnBubbleBrush

// "Replying to …" bar above the composer
@Immutable
data class ThreadReplyDraftUi(val title: String, val preview: String)

// Instagram composer: gradient camera/gallery button, multiline pill field, and either
// gallery + ❤️ (empty field) or a blue "Send" (text typed). Reply bar slides in above it.
@Composable
fun ThreadComposer(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onLike: () -> Unit,
    onPickPhoto: () -> Unit,
    reply: ThreadReplyDraftUi?,
    onCancelReply: () -> Unit,
    colors: ThreadColors,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val hasText = text.isNotBlank()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .navigationBarsPadding()
            .imePadding(),
    ) {
        AnimatedVisibility(
            visible = reply != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            // Keep showing the last reply while the bar animates out
            val shown = remember { ReplyHolder() }
            if (reply != null) shown.value = reply
            shown.value?.let { ReplyBar(reply = it, colors = colors, onCancel = onCancelReply) }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(colors.composerPill)
                .heightIn(min = 44.dp)
                .padding(start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Gradient camera button (opens the photo picker)
            Box(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(ChatOwnBubbleBrush)
                    .clickable(enabled = enabled, onClickLabel = "Send a photo", role = Role.Button, onClick = onPickPhoto),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = ThreadIcons.Camera,
                    contentDescription = "Send a photo",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            val textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 15.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                enabled = enabled,
                textStyle = textStyle,
                maxLines = 5,
                cursorBrush = SolidColor(ChatBlue),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 10.dp)
                    .focusRequester(focusRequester),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (text.isEmpty()) {
                            Text(
                                text = "Message…",
                                style = textStyle,
                                color = colors.secondary,
                                maxLines = 1,
                            )
                        }
                        inner()
                    }
                },
            )
            if (hasText) {
                Text(
                    text = "Send",
                    color = ChatBlue,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(enabled = enabled, role = Role.Button, onClick = onSend)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                )
            } else {
                IconButton(onClick = onPickPhoto, enabled = enabled, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = ThreadIcons.Gallery,
                        contentDescription = "Gallery",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp),
                    )
                }
                IconButton(onClick = onLike, enabled = enabled, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = "Send a like",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(25.dp),
                    )
                }
            }
        }
    }
}

private class ReplyHolder {
    var value: ThreadReplyDraftUi? = null
}

@Composable
private fun ReplyBar(reply: ThreadReplyDraftUi, colors: ThreadColors, onCancel: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = colors.divider, thickness = 0.5.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reply.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = reply.preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Cancel reply",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
