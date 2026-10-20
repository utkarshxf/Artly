package com.orion.templete.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val VerifiedBlue = Color(0xFF3897F0)

// Instagram-style blue tick. Shown on real artists: artist profiles that are not an Artistry user's own profile.
@Composable
fun VerifiedBadge(modifier: Modifier = Modifier, size: Dp = 16.dp) {
    Icon(
        imageVector = Icons.Filled.Verified,
        contentDescription = "Verified artist",
        tint = VerifiedBlue,
        modifier = modifier.size(size)
    )
}

// A name followed by the verified tick; a long name is shortened but the tick always stays visible
@Composable
fun NameWithBadge(
    name: String,
    verified: Boolean,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    fontWeight: FontWeight? = null,
    color: Color = Color.Unspecified,
    maxLines: Int = 1,
    textAlign: TextAlign? = null,
    badgeSize: Dp = 16.dp,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = horizontalArrangement
    ) {
        Text(
            text = name,
            style = style,
            fontWeight = fontWeight,
            color = color,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            textAlign = textAlign,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (verified) {
            Spacer(modifier = Modifier.width(4.dp))
            VerifiedBadge(size = badgeSize)
        }
    }
}
