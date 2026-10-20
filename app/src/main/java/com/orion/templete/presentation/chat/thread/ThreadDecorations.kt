package com.orion.templete.presentation.chat.thread

import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orion.templete.presentation.chat.components.ChatAvatar
import com.orion.templete.presentation.chat.components.ChatBlue

// Centered time label between message groups ("Today 10:42")
@Composable
fun ThreadTimeSeparator(label: String, colors: ThreadColors, modifier: Modifier = Modifier) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
        color = colors.secondary,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 10.dp),
    )
}

// Peer bubble with three bouncing dots
@Composable
fun ThreadTypingIndicator(
    avatarUrl: String?,
    name: String,
    colors: ThreadColors,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .semantics { contentDescription = "$name is typing" },
        verticalAlignment = Alignment.Bottom,
    ) {
        ChatAvatar(url = avatarUrl, name = name, size = ThreadDimens.AvatarSize)
        Spacer(Modifier.width(ThreadDimens.AvatarGap))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(colors.peerBubble)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val transition = rememberInfiniteTransition(label = "typing")
            val dotColor = colors.peerText.copy(alpha = 0.55f)
            repeat(3) { index ->
                val lift by transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = keyframes {
                            durationMillis = 1_200
                            0f at 0
                            -5f at 200
                            0f at 400
                            0f at 1_200
                        },
                        initialStartOffset = StartOffset(index * 160),
                    ),
                    label = "dot$index",
                )
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .graphicsLayer {
                            translationY = lift * density
                            alpha = 0.55f + (-lift / 5f) * 0.45f
                        }
                        .clip(CircleShape)
                        .background(dotColor),
                )
            }
        }
    }
}

// Shown at the top once the whole conversation is loaded
@Composable
fun ThreadProfileHeader(
    header: ThreadHeaderUi,
    colors: ThreadColors,
    onViewProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 28.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ChatAvatar(
            url = header.avatar,
            name = header.name,
            size = 88.dp,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClickLabel = "View profile", onClick = onViewProfile),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = header.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "@${header.username} · Artistry",
            style = MaterialTheme.typography.bodySmall,
            color = colors.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(14.dp))
        FilledTonalButton(onClick = onViewProfile) {
            Text(text = "View profile", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun ThreadLoadingOlder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
}

// Determinate ring drawn over an uploading photo
@Composable
fun ThreadProgressRing(progress: Float, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        label = "uploadProgress",
    )
    Canvas(modifier = modifier.size(40.dp)) {
        val stroke = 3.dp.toPx()
        val inset = stroke / 2f
        drawCircle(
            color = Color.White.copy(alpha = 0.3f),
            radius = size.minDimension / 2f - inset,
            style = Stroke(width = stroke),
        )
        drawArc(
            color = Color.White,
            startAngle = -90f,
            sweepAngle = 360f * animated.coerceAtLeast(0.03f),
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(size.width - stroke, size.height - stroke),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}

@Composable
fun ThreadCenteredLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            strokeWidth = 2.5.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
    }
}

// Sign-in / listener failure with Retry ("Messages are coming soon" while the backend isn't configured)
@Composable
fun ThreadErrorState(
    error: ThreadContent.Error,
    colors: ThreadColors,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(colors.peerBubble),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (error.notConfigured) ThreadIcons.Chat else ThreadIcons.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = error.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = error.message,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.secondary,
            textAlign = TextAlign.Center,
        )
        if (error.retryable) {
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onRetry) {
                Text(
                    text = "Try again",
                    color = ChatBlue,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
