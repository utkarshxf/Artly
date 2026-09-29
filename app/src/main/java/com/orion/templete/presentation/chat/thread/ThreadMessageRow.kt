package com.orion.templete.presentation.chat.thread

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.orion.templete.data.model.chat.ArtworkRef
import com.orion.templete.data.model.chat.ProfileRef
import com.orion.templete.data.model.chat.MessageType
import com.orion.templete.presentation.chat.components.ChatAvatar
import com.orion.templete.presentation.chat.components.ChatBlue
import com.orion.templete.presentation.chat.components.ChatOwnBubbleBrush
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Everything a row can ask the screen to do. Created once per screen so rows stay skippable.
@Stable
class ThreadRowCallbacks(
    val onLongPress: (message: ThreadMessageUi, bubbleBoundsInRoot: Rect) -> Unit,
    val onDoubleTap: (message: ThreadMessageUi) -> Boolean, // true = a heart was added (plays the pop)
    val onSwipeReply: (message: ThreadMessageUi) -> Unit,
    val onOpenImage: (message: ThreadMessageUi) -> Unit,
    val onOpenArtwork: (artworkId: String) -> Unit,
    val onOpenSharedProfile: (profileId: String) -> Unit,
    val onOpenLink: (url: String) -> Unit,
    val onReplyQuoteClick: (message: ThreadMessageUi) -> Unit,
    val onRetry: (message: ThreadMessageUi) -> Unit,
    val onReactionsClick: (message: ThreadMessageUi) -> Unit,
    val onPeerClick: () -> Unit,
)

// Laid-out text of a bubble, for link hit-testing. Plain holder: writing it never recomposes anything.
class ThreadTextLayout {
    var layout: TextLayoutResult? = null
    var coordinates: LayoutCoordinates? = null

    fun linkAt(bubble: LayoutCoordinates?, tap: Offset): String? {
        val result = layout ?: return null
        val text = coordinates ?: return null
        if (bubble == null || !bubble.isAttached || !text.isAttached) return null
        val local = text.localPositionOf(bubble, tap)
        if (local.y < 0f || local.y > result.size.height) return null
        val line = result.getLineForVerticalPosition(local.y)
        val slop = 6f
        if (local.x < result.getLineLeft(line) - slop || local.x > result.getLineRight(line) + slop) return null
        val offset = result.getOffsetForPosition(local)
        return result.layoutInput.text.getStringAnnotations(LINK_TAG, offset, offset).firstOrNull()?.item
    }

    companion object {
        const val LINK_TAG = "URL"
    }
}

private class CoordinatesHolder {
    var coordinates: LayoutCoordinates? = null

    fun boundsInRoot(): Rect = coordinates?.takeIf { it.isAttached }?.boundsInRoot() ?: Rect.Zero
}

@Composable
fun ThreadMessageRow(
    message: ThreadMessageUi,
    peerAvatar: String?,
    peerName: String,
    highlighted: Boolean,
    colors: ThreadColors,
    maxBubbleWidth: Dp,
    callbacks: ThreadRowCallbacks,
    modifier: Modifier = Modifier,
) {
    val current by rememberUpdatedState(message)
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val swipe = remember { Animatable(0f) }
    val thresholdPx = with(density) { ThreadDimens.SwipeThreshold.toPx() }
    val maxSwipePx = with(density) { ThreadDimens.SwipeMax.toPx() }
    val highlight by animateColorAsState(
        targetValue = if (highlighted) colors.highlight else Color.Transparent,
        animationSpec = tween(durationMillis = 350),
        label = "rowHighlight",
    )

    val swipeModifier = if (message.canReact) {
        Modifier.pointerInput(Unit) {
            var crossed = false
            detectHorizontalDragGestures(
                onDragStart = { crossed = false },
                onDragEnd = {
                    if (swipe.value >= thresholdPx) callbacks.onSwipeReply(current)
                    scope.launch {
                        swipe.animateTo(0f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow))
                    }
                },
                onDragCancel = {
                    scope.launch { swipe.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
                },
                onHorizontalDrag = { change, amount ->
                    val resistance = if (swipe.value > thresholdPx) 0.3f else 0.75f
                    val next = (swipe.value + amount * resistance).coerceIn(0f, maxSwipePx)
                    if (next != swipe.value) {
                        change.consume()
                        scope.launch { swipe.snapTo(next) }
                    }
                    if (!crossed && next >= thresholdPx) {
                        crossed = true
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    } else if (crossed && next < thresholdPx) {
                        crossed = false
                    }
                },
            )
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind { if (highlight.alpha > 0f) drawRect(highlight) }
            .padding(top = if (message.groupedWithPrev) 2.dp else 8.dp)
            .then(swipeModifier),
    ) {
        // Reply arrow revealed behind the row while swiping
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 4.dp)
                .size(30.dp)
                .graphicsLayer {
                    val progress = (swipe.value / thresholdPx).coerceIn(0f, 1f)
                    alpha = progress
                    scaleX = 0.5f + 0.5f * progress
                    scaleY = 0.5f + 0.5f * progress
                }
                .clip(CircleShape)
                .background(colors.peerBubble),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = ThreadIcons.Reply,
                contentDescription = null,
                tint = colors.peerText,
                modifier = Modifier.size(16.dp),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = swipe.value },
            horizontalAlignment = if (message.mine) Alignment.End else Alignment.Start,
        ) {
            message.replyTo?.let { reply ->
                ThreadReplyQuote(
                    reply = reply,
                    mine = message.mine,
                    colors = colors,
                    maxBubbleWidth = maxBubbleWidth,
                    onClick = { callbacks.onReplyQuoteClick(current) },
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                if (!message.mine) {
                    Box(modifier = Modifier.size(ThreadDimens.AvatarSize)) {
                        if (message.showAvatar) {
                            ChatAvatar(
                                url = peerAvatar,
                                name = peerName,
                                size = ThreadDimens.AvatarSize,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable(onClickLabel = "View profile", onClick = callbacks.onPeerClick),
                            )
                        }
                    }
                    Spacer(Modifier.width(ThreadDimens.AvatarGap))
                }
                ThreadBubble(
                    message = message,
                    colors = colors,
                    maxBubbleWidth = maxBubbleWidth,
                    callbacks = callbacks,
                )
            }
            message.status?.let { status ->
                val failed = status.kind == ThreadStatusKind.FAILED
                Text(
                    text = status.label,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = if (failed) colors.error else colors.secondary,
                    fontWeight = if (failed) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier
                        .padding(top = 3.dp, end = 4.dp, start = 4.dp)
                        .then(
                            if (failed) Modifier.clickable(onClickLabel = "Retry") { callbacks.onRetry(current) }
                            else Modifier
                        ),
                )
            }
        }
    }
}

@Composable
private fun ThreadBubble(
    message: ThreadMessageUi,
    colors: ThreadColors,
    maxBubbleWidth: Dp,
    callbacks: ThreadRowCallbacks,
) {
    val current by rememberUpdatedState(message)
    val haptics = LocalHapticFeedback.current
    val textLayout = remember { ThreadTextLayout() }
    val bubble = remember { CoordinatesHolder() }
    var popKey by remember { mutableIntStateOf(0) }
    val shape = remember(message.mine, message.groupedWithPrev, message.groupedWithNext, message.type) {
        threadBubbleShape(
            mine = message.mine,
            groupedWithPrev = message.groupedWithPrev,
            groupedWithNext = message.groupedWithNext,
            radius = if (message.type == MessageType.IMAGE) ThreadDimens.ImageRadius else 20.dp,
        )
    }

    Column(horizontalAlignment = if (message.mine) Alignment.End else Alignment.Start) {
        Box {
            ThreadBubbleContent(
                message = message,
                colors = colors,
                maxBubbleWidth = maxBubbleWidth,
                shape = shape,
                textLayout = textLayout,
                modifier = Modifier
                    .onGloballyPositioned { bubble.coordinates = it }
                    .pointerInput(message.canReact) {
                        detectTapGestures(
                            onTap = { offset ->
                                val msg = current
                                val artwork = msg.artwork
                                val profile = msg.profile
                                when {
                                    msg.failed -> callbacks.onRetry(msg)
                                    msg.type == MessageType.IMAGE && msg.image != null -> callbacks.onOpenImage(msg)
                                    msg.type == MessageType.ARTWORK && artwork != null -> callbacks.onOpenArtwork(artwork.id)
                                    msg.type == MessageType.PROFILE && profile != null -> callbacks.onOpenSharedProfile(profile.id)
                                    msg.links.isNotEmpty() -> textLayout.linkAt(bubble.coordinates, offset)
                                        ?.let(callbacks.onOpenLink)
                                }
                            },
                            onDoubleTap = if (message.canReact) {
                                { _ -> if (callbacks.onDoubleTap(current)) popKey++ }
                            } else {
                                null
                            },
                            onLongPress = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                callbacks.onLongPress(current, bubble.boundsInRoot())
                            },
                        )
                    },
            )
            // The heart must not change the row's size: it is drawn over the bubble and may overflow it
            Box(modifier = Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                ThreadHeartPop(trigger = popKey)
            }
        }
        message.reactions?.let { reactions ->
            ThreadReactionChip(
                reactions = reactions,
                colors = colors,
                onClick = { callbacks.onReactionsClick(current) },
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .offset(y = -ThreadDimens.ReactionOverlap),
            )
        }
    }
}

// The visual part of a message (also drawn, without gestures, in the long-press overlay)
@Composable
fun ThreadBubbleContent(
    message: ThreadMessageUi,
    colors: ThreadColors,
    maxBubbleWidth: Dp,
    shape: Shape,
    textLayout: ThreadTextLayout?,
    modifier: Modifier = Modifier,
) {
    val image = message.image
    val artwork = message.artwork
    val profile = message.profile
    when {
        message.type == MessageType.LIKE -> Text(
            text = THREAD_HEART,
            fontSize = 48.sp,
            lineHeight = 56.sp,
            modifier = modifier
                .padding(horizontal = 4.dp)
                .semantics { contentDescription = "Like" },
        )
        message.type == MessageType.TEXT && message.emojiOnly -> Text(
            text = message.text.trim(),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 36.sp, lineHeight = 44.sp),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = modifier.padding(horizontal = 2.dp),
        )
        message.type == MessageType.IMAGE && image != null -> ThreadImageBubble(
            message = message,
            image = image,
            colors = colors,
            shape = shape,
            maxBubbleWidth = maxBubbleWidth,
            modifier = modifier,
        )
        message.type == MessageType.ARTWORK && artwork != null -> ThreadArtworkCard(
            artwork = artwork,
            colors = colors,
            modifier = modifier,
        )
        message.type == MessageType.PROFILE && profile != null -> ThreadProfileCard(
            profile = profile,
            colors = colors,
            modifier = modifier,
        )
        else -> ThreadTextBubble(
            message = message,
            colors = colors,
            maxBubbleWidth = maxBubbleWidth,
            shape = shape,
            textLayout = textLayout,
            modifier = modifier,
        )
    }
}

@Composable
private fun ThreadTextBubble(
    message: ThreadMessageUi,
    colors: ThreadColors,
    maxBubbleWidth: Dp,
    shape: Shape,
    textLayout: ThreadTextLayout?,
    modifier: Modifier = Modifier,
) {
    val mine = message.mine
    val text = message.text.ifEmpty { " " }
    val annotated = remember(text, message.links, mine) {
        linkedText(text, message.links, if (mine) Color.White else ChatBlue)
    }
    val hasLinks = message.links.isNotEmpty() && textLayout != null
    Box(
        modifier = modifier
            .widthIn(max = maxBubbleWidth)
            .then(
                if (mine) Modifier.background(ChatOwnBubbleBrush, shape)
                else Modifier.background(colors.peerBubble, shape)
            )
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Text(
            text = annotated,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 20.sp),
            color = if (mine) colors.mineText else colors.peerText,
            onTextLayout = { result -> if (hasLinks) textLayout?.layout = result },
            modifier = if (hasLinks) Modifier.onGloballyPositioned { textLayout?.coordinates = it } else Modifier,
        )
    }
}

private fun linkedText(text: String, links: List<ThreadLink>, linkColor: Color): AnnotatedString {
    if (links.isEmpty()) return AnnotatedString(text)
    return buildAnnotatedString {
        append(text)
        val style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)
        links.forEach { link ->
            val start = link.start.coerceIn(0, text.length)
            val end = link.end.coerceIn(start, text.length)
            if (end > start) {
                addStyle(style, start, end)
                addStringAnnotation(ThreadTextLayout.LINK_TAG, link.url, start, end)
            }
        }
    }
}

@Composable
private fun ThreadImageBubble(
    message: ThreadMessageUi,
    image: ThreadImageUi,
    colors: ThreadColors,
    shape: Shape,
    maxBubbleWidth: Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val knownSize = (image.width ?: 0) > 0 && (image.height ?: 0) > 0
    // A picked photo has no size yet: take it from the decoded preview
    var loadedAspect by remember(image.model) { mutableStateOf<Float?>(null) }
    val aspect = if (knownSize) image.aspectRatio else loadedAspect ?: 1f
    val request = remember(image.model, image.memoryKey, image.placeholderKey) {
        ImageRequest.Builder(context)
            .data(image.model)
            .crossfade(true)
            .apply {
                image.memoryKey?.let { memoryCacheKey(it) }
                image.placeholderKey?.let { placeholderMemoryCacheKey(it) }
            }
            .build()
    }
    Column(
        modifier = modifier,
        horizontalAlignment = if (message.mine) Alignment.End else Alignment.Start,
    ) {
        Box(
            modifier = Modifier
                .width(ThreadDimens.ImageWidth)
                .aspectRatio(aspect)
                .clip(shape)
                .background(colors.placeholder),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = request,
                contentDescription = if (message.mine) "Photo you sent" else "Photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onSuccess = { state ->
                    if (!knownSize) {
                        val size = state.painter.intrinsicSize
                        if (size != Size.Unspecified && size.width > 0f && size.height > 0f) {
                            loadedAspect = (size.width / size.height).coerceIn(0.5f, 2f)
                        }
                    }
                },
            )
            val progress = message.uploadProgress
            if (progress != null || message.failed) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (message.failed) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Retry",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp),
                        )
                    } else if (progress != null) {
                        ThreadProgressRing(progress = progress)
                    }
                }
            }
        }
        if (message.text.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            ThreadTextBubble(
                message = message,
                colors = colors,
                maxBubbleWidth = maxBubbleWidth,
                shape = RoundedCornerShape(20.dp),
                textLayout = null,
            )
        }
    }
}

@Composable
private fun ThreadArtworkCard(
    artwork: ArtworkRef,
    colors: ThreadColors,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val imageUrl = artwork.imageUrl?.takeIf { it.isNotBlank() }
    val request = remember(imageUrl) {
        ImageRequest.Builder(context).data(imageUrl).crossfade(true).build()
    }
    Column(
        modifier = modifier
            .width(ThreadDimens.ArtworkWidth)
            .clip(RoundedCornerShape(ThreadDimens.ImageRadius))
            .background(colors.peerBubble),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(colors.placeholder),
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = request,
                    contentDescription = artwork.title ?: "Shared artwork",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = artwork.title?.takeIf { it.isNotBlank() } ?: "Artwork",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = colors.peerText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            artwork.artistName?.takeIf { it.isNotBlank() }?.let { artist ->
                Text(
                    text = "by $artist",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Artistry",
                style = MaterialTheme.typography.labelSmall,
                color = colors.secondary,
            )
        }
    }
}

// Instagram-style shared profile: avatar, name, subtitle and a "View profile" button (the whole card opens it)
@Composable
private fun ThreadProfileCard(
    profile: ProfileRef,
    colors: ThreadColors,
    modifier: Modifier = Modifier,
) {
    val name = profile.name?.takeIf { it.isNotBlank() } ?: profile.id
    Column(
        modifier = modifier
            .width(ThreadDimens.ArtworkWidth)
            .clip(RoundedCornerShape(ThreadDimens.ImageRadius))
            .background(colors.peerBubble)
            .padding(horizontal = 12.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ChatAvatar(url = profile.avatar, name = name, size = 72.dp)
        Spacer(Modifier.height(10.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = colors.peerText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        profile.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.placeholder),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "View profile",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.peerText,
            )
        }
    }
}

@Composable
private fun ThreadReplyQuote(
    reply: ThreadReplyUi,
    mine: Boolean,
    colors: ThreadColors,
    maxBubbleWidth: Dp,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.padding(
            start = if (mine) 0.dp else ThreadDimens.AvatarSize + ThreadDimens.AvatarGap,
            end = if (mine) 2.dp else 0.dp,
            bottom = 2.dp,
        ),
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        ) {
            Icon(
                imageVector = ThreadIcons.Reply,
                contentDescription = null,
                tint = colors.secondary,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = reply.label,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = colors.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val thumbnail = reply.thumbnailUrl
        if (thumbnail != null) {
            val request = remember(thumbnail) {
                ImageRequest.Builder(context).data(thumbnail).crossfade(true).build()
            }
            AsyncImage(
                model = request,
                contentDescription = reply.preview,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(84.dp)
                    .height(112.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.placeholder)
                    .graphicsLayer { alpha = 0.75f }
                    .clickable(onClickLabel = "Show original message", onClick = onClick),
            )
        } else {
            Box(
                modifier = Modifier
                    .widthIn(max = maxBubbleWidth * 0.85f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.quoteBackground)
                    .clickable(
                        enabled = !reply.unavailable,
                        onClickLabel = "Show original message",
                        onClick = onClick,
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = reply.preview,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    fontStyle = if (reply.unavailable) FontStyle.Italic else FontStyle.Normal,
                    color = colors.quoteText,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ThreadReactionChip(
    reactions: ThreadReactionsUi,
    colors: ThreadColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pill = RoundedCornerShape(50)
    val description = remember(reactions) {
        "Reactions: " + reactions.byUser.joinToString { it.emoji }
    }
    Row(
        modifier = modifier
            .background(colors.chipBorder, pill)
            .padding(2.dp)
            .clip(pill)
            .background(colors.chipBackground)
            .clickable(onClickLabel = "See reactions", onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 1.dp)
            .semantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = reactions.emojis.joinToString(separator = ""),
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
        if (reactions.count >= 2) {
            Spacer(Modifier.width(3.dp))
            Text(
                text = reactions.count.toString(),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = colors.peerText,
            )
        }
    }
}

// ❤️ that pops over a bubble when it is double-tapped
@Composable
private fun ThreadHeartPop(trigger: Int) {
    if (trigger == 0) return
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        alpha.snapTo(1f)
        scale.snapTo(0.2f)
        scale.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 520f))
        delay(260)
        launch { scale.animateTo(1.3f, tween(durationMillis = 180)) }
        alpha.animateTo(0f, tween(durationMillis = 180))
    }
    Text(
        text = THREAD_HEART,
        fontSize = 44.sp,
        modifier = Modifier
            .wrapContentSize(unbounded = true)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = alpha.value
            },
    )
}
