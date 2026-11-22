package com.orion.templete.presentation.chat.thread

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orion.templete.data.model.chat.MessageType
import com.orion.templete.presentation.chat.components.ChatAvatar
import com.orion.templete.presentation.chat.components.ChatTime
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.roundToInt

// A long-pressed message: `bounds` is the bubble's position inside the screen's root box
@Immutable
data class ThreadSelection(val message: ThreadMessageUi, val bounds: Rect)

// Instagram long-press: dimmed (and, on Android 12+, blurred) screen, the message lifted in place, the reaction
// bar above it and the actions card below it.
@Composable
fun ThreadMessageOverlay(
    selection: ThreadSelection,
    colors: ThreadColors,
    maxBubbleWidth: Dp,
    onReact: (emoji: String?) -> Unit,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onUnsend: () -> Unit,
    onRetry: () -> Unit,
    onDeleteLocal: () -> Unit,
    onDismiss: () -> Unit,
) {
    val message = selection.message
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        appear.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = 700f))
    }
    BackHandler(onBack = onDismiss)

    val shape = remember(message.mine, message.type) {
        threadBubbleShape(
            mine = message.mine,
            groupedWithPrev = false,
            groupedWithNext = false,
            radius = if (message.type == MessageType.IMAGE) ThreadDimens.ImageRadius else 20.dp,
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = appear.value.coerceIn(0f, 1f) }
            .background(Color.Black.copy(alpha = if (colors.isDark) 0.62f else 0.42f))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
    ) {
        OverlayLayout(bounds = selection.bounds, mine = message.mine) {
            // 0: reaction bar
            if (message.canReact) {
                ReactionBar(
                    current = message.reactions?.mine,
                    colors = colors,
                    onReact = onReact,
                    modifier = Modifier.graphicsLayer {
                        val t = appear.value
                        scaleX = 0.85f + 0.15f * t
                        scaleY = 0.85f + 0.15f * t
                    },
                )
            } else {
                Spacer(Modifier.size(0.dp))
            }
            // 1: the message itself
            ThreadBubbleContent(
                message = message,
                colors = colors,
                maxBubbleWidth = maxBubbleWidth,
                shape = shape,
                textLayout = null,
                modifier = Modifier.graphicsLayer {
                    val t = appear.value
                    scaleX = 0.97f + 0.03f * t
                    scaleY = 0.97f + 0.03f * t
                },
            )
            // 2: actions
            ActionsCard(
                message = message,
                colors = colors,
                onReply = onReply,
                onCopy = onCopy,
                onUnsend = onUnsend,
                onRetry = onRetry,
                onDeleteLocal = onDeleteLocal,
            )
        }
    }
}

// Places [bar, bubble, menu] around the bubble's original position, shifted to stay on screen
@Composable
private fun OverlayLayout(
    bounds: Rect,
    mine: Boolean,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = Modifier.fillMaxSize()) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val margin = 12.dp.roundToPx()
        val gap = 8.dp.roundToPx()
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val bar = measurables[0].measure(loose)
        val menu = measurables[2].measure(loose)
        val barHeight = if (bar.height > 0) bar.height + gap else 0
        val available = (height - 2 * margin - barHeight - gap - menu.height).coerceAtLeast(1)
        val bubble = measurables[1].measure(
            Constraints(minWidth = 0, maxWidth = width, minHeight = 0, maxHeight = available)
        )
        val total = barHeight + bubble.height + gap + menu.height
        val desiredTop = bounds.top.roundToInt() - barHeight
        val top = desiredTop.coerceIn(margin, max(margin, height - margin - total))
        val bubbleX = bounds.left.roundToInt().coerceIn(0, max(0, width - bubble.width))

        fun aligned(childWidth: Int): Int {
            val x = if (mine) bubbleX + bubble.width - childWidth else bubbleX
            return x.coerceIn(margin, max(margin, width - margin - childWidth))
        }

        layout(width, height) {
            bar.place(aligned(bar.width), top)
            bubble.place(bubbleX, top + barHeight)
            menu.place(aligned(menu.width), top + barHeight + bubble.height + gap)
        }
    }
}

@Composable
private fun ReactionBar(
    current: String?,
    colors: ThreadColors,
    onReact: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pill = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .shadow(elevation = 8.dp, shape = pill)
            .clip(pill)
            .background(colors.menuBackground)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        THREAD_QUICK_REACTIONS.forEachIndexed { index, emoji ->
            val selected = emoji == current
            val pop = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                delay(index * 28L)
                pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 650f))
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .graphicsLayer {
                        scaleX = pop.value
                        scaleY = pop.value
                    }
                    .clip(CircleShape)
                    .background(if (selected) colors.highlight.copy(alpha = 0.22f) else Color.Transparent)
                    .clickable(
                        onClickLabel = if (selected) "Remove reaction" else "React with $emoji",
                        role = Role.Button,
                    ) { onReact(if (selected) null else emoji) },
                contentAlignment = Alignment.Center,
            ) {
                Text(text = emoji, fontSize = 27.sp)
            }
        }
    }
}

@Composable
private fun ActionsCard(
    message: ThreadMessageUi,
    colors: ThreadColors,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onUnsend: () -> Unit,
    onRetry: () -> Unit,
    onDeleteLocal: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    val uploading = message.uploadProgress != null
    Column(
        modifier = Modifier
            .width(224.dp)
            .shadow(elevation = 8.dp, shape = shape)
            .clip(shape)
            .background(colors.menuBackground)
            // Swallow taps between items so they don't dismiss through the scrim
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        Text(
            text = ChatTime.separator(message.createdAt),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = colors.secondary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 6.dp),
        )
        val items = buildList {
            if (message.failed) {
                add(MenuAction("Retry", Icons.Filled.Refresh, destructive = false, onRetry))
                add(MenuAction("Delete", ThreadIcons.Delete, destructive = true, onDeleteLocal))
            } else {
                if (message.canReact) add(MenuAction("Reply", ThreadIcons.Reply, destructive = false, onReply))
                if (message.copyText != null) add(MenuAction("Copy", ThreadIcons.Copy, destructive = false, onCopy))
                if (message.mine && message.canReact) {
                    add(MenuAction("Unsend", ThreadIcons.Unsend, destructive = true, onUnsend))
                }
                if (message.isLocal && uploading) {
                    add(MenuAction("Cancel", ThreadIcons.Delete, destructive = true, onDeleteLocal))
                }
            }
        }
        items.forEachIndexed { index, action ->
            HorizontalDivider(color = colors.divider, thickness = 0.5.dp)
            MenuRow(action = action, colors = colors)
            if (index == items.lastIndex) Spacer(Modifier.height(2.dp))
        }
    }
}

private class MenuAction(
    val label: String,
    val icon: ImageVector,
    val destructive: Boolean,
    val onClick: () -> Unit,
)

@Composable
private fun MenuRow(action: MenuAction, colors: ThreadColors) {
    val tint = if (action.destructive) colors.error else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = action.onClick)
            .height(48.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = action.label,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
            color = tint,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = action.icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
fun ThreadUnsendDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Unsend message?", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                text = "Unsending will remove the message for everyone. People may have seen it already.",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Unsend", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = MaterialTheme.colorScheme.onSurface)
            }
        },
    )
}

// Who reacted (tap on the reactions chip); tapping your own reaction removes it
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadReactionsSheet(
    reactions: ThreadReactionsUi,
    me: String,
    header: ThreadHeaderUi,
    colors: ThreadColors,
    onRemoveMine: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.menuBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Text(
                text = "Reactions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            )
            reactions.byUser.forEach { reaction ->
                val isMe = reaction.username == me
                val name = when {
                    isMe -> "You"
                    reaction.username == header.username -> header.name
                    else -> reaction.username
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = isMe, onClickLabel = "Remove reaction") { onRemoveMine() }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ChatAvatar(
                        url = if (reaction.username == header.username) header.avatar else null,
                        name = if (isMe) me else name,
                        size = 44.dp,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (isMe) {
                            Text(
                                text = "Tap to remove",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.secondary,
                            )
                        }
                    }
                    Text(text = reaction.emoji, fontSize = 26.sp)
                }
            }
        }
    }
}
