package com.orion.templete.presentation.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage

val ChatActiveGreen = Color(0xFF34C759)
val ChatBlue = Color(0xFF3897F0)
val ChatOwnBubbleBrush = Brush.horizontalGradient(listOf(Color(0xFF91002F), Color(0xFFC2185B)))
val ChatPeerBubbleDark = Color(0xFF262626)
val ChatPeerBubbleLight = Color(0xFFEFEFEF)

// Round avatar with the Instagram-style green "active now" dot. Falls back to the first letter of the name.
@Composable
fun ChatAvatar(
    url: String?,
    name: String,
    size: Dp,
    modifier: Modifier = Modifier,
    active: Boolean = false,
) {
    Box(modifier = modifier.size(size)) {
        SubcomposeAsyncImage(
            model = url?.takeIf { it.isNotBlank() },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            loading = { AvatarInitial(name, size) },
            error = { AvatarInitial(name, size) },
        )
        if (active) {
            val dot = (size.value * 0.26f).coerceIn(10f, 16f).dp
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(dot)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .clip(CircleShape)
                    .background(ChatActiveGreen)
            )
        }
    }
}

@Composable
private fun AvatarInitial(name: String, size: Dp) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.trim().firstOrNull()?.uppercase() ?: "?",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            fontSize = (size.value * 0.4f).sp
        )
    }
}
