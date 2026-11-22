package com.orion.templete.presentation.chat.thread

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.orion.templete.presentation.chat.components.ChatPeerBubbleDark
import com.orion.templete.presentation.chat.components.ChatPeerBubbleLight

// Colours of the conversation screen, resolved once per theme
@Immutable
data class ThreadColors(
    val isDark: Boolean,
    val background: Color,
    val peerBubble: Color,
    val peerText: Color,
    val mineText: Color,
    val secondary: Color,
    val quoteBackground: Color,
    val quoteText: Color,
    val chipBackground: Color,
    val chipBorder: Color,
    val composerPill: Color,
    val menuBackground: Color,
    val divider: Color,
    val placeholder: Color,
    val highlight: Color,
    val error: Color,
)

@Composable
fun rememberThreadColors(): ThreadColors {
    val scheme = MaterialTheme.colorScheme
    return remember(scheme) {
        val dark = scheme.background.luminance() < 0.5f
        val peerBubble = if (dark) ChatPeerBubbleDark else ChatPeerBubbleLight
        ThreadColors(
            isDark = dark,
            background = scheme.background,
            peerBubble = peerBubble,
            peerText = scheme.onSurface,
            mineText = Color.White,
            // The dark theme's onSurfaceVariant is plain white, so secondary text is dimmed to stand apart
            secondary = scheme.onSurfaceVariant.copy(alpha = if (dark) 0.62f else 0.85f),
            quoteBackground = if (dark) Color(0xFF1E1E1E) else Color(0xFFF3F3F3),
            quoteText = scheme.onSurface.copy(alpha = 0.72f),
            chipBackground = if (dark) Color(0xFF262626) else Color(0xFFEFEFEF),
            chipBorder = scheme.background,
            composerPill = if (dark) Color(0xFF1F1F1F) else Color(0xFFEFEFEF),
            menuBackground = if (dark) Color(0xFF262626) else Color.White,
            divider = scheme.onSurface.copy(alpha = if (dark) 0.12f else 0.08f),
            placeholder = if (dark) Color(0xFF1C1C1C) else Color(0xFFE8E8E8),
            highlight = scheme.onSurface.copy(alpha = 0.10f),
            error = scheme.error,
        )
    }
}

// Instagram corner shaping: 20 dp corners, 4 dp on the grouped side where a bubble touches its neighbour
fun threadBubbleShape(
    mine: Boolean,
    groupedWithPrev: Boolean,
    groupedWithNext: Boolean,
    radius: Dp = 20.dp,
    inner: Dp = 4.dp,
): Shape {
    val top = if (groupedWithPrev) inner else radius
    val bottom = if (groupedWithNext) inner else radius
    return if (mine) {
        RoundedCornerShape(topStart = radius, topEnd = top, bottomEnd = bottom, bottomStart = radius)
    } else {
        RoundedCornerShape(topStart = top, topEnd = radius, bottomEnd = radius, bottomStart = bottom)
    }
}

object ThreadDimens {
    val AvatarSize = 28.dp
    val AvatarGap = 8.dp
    val ImageWidth = 220.dp
    val ArtworkWidth = 240.dp
    val CallWidth = 220.dp
    val ImageRadius = 18.dp
    val SwipeThreshold = 64.dp
    val SwipeMax = 96.dp
    val ReactionOverlap = 8.dp
    val HorizontalPadding = 12.dp
}
