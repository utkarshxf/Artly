package com.orion.templete.presentation.common


import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter

@Composable
fun ArtistProfileCard(
    username: String,
    userId: String,
    profilePictureUrl: String,
    verified: Boolean = false,
    // null = no Follow button (e.g. your own profile)
    following: Boolean? = null,
    followBusy: Boolean = false,
    onFollowClick: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp , vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Picture
            Image(
                painter = rememberAsyncImagePainter(profilePictureUrl),
                contentDescription = "Profile picture of $username",
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            // User Info
            Column(modifier = Modifier.weight(1f)) {
                NameWithBadge(
                    name = username,
                    verified = verified,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    badgeSize = 14.dp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "@$userId",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            if (following != null) {
                Spacer(modifier = Modifier.width(12.dp))
                FollowPill(following = following, busy = followBusy, onClick = onFollowClick)
            }
        }
    }
}

// Instagram-style compact Follow / Following button
@Composable
private fun FollowPill(following: Boolean, busy: Boolean, onClick: () -> Unit) {
    val container = if (following) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary
    val content = if (following) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary
    Button(
        onClick = onClick,
        enabled = !busy,
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = container,
            disabledContentColor = content
        ),
        modifier = Modifier.height(34.dp)
    ) {
        if (busy) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = content)
        } else {
            Text(
                text = if (following) "Following" else "Follow",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
