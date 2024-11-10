package com.orion.templete.presentation.artist_profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.orion.templete.data.model.user_model.UserDTO
import com.orion.templete.presentation.common.ErrorScreen
import com.orion.templete.presentation.common.LoadingScreen


@Composable
fun ArtistProfileScreen(
    userId: String? = null,
    viewModel: ArtistProfileViewModel = hiltViewModel()
) {
    LaunchedEffect(userId) {
        userId?.let { viewModel.getUserProfile(it) }
    }

    when (val uiState = viewModel.artistProfileScreenUiState) {
        is ArtistProfileScreenUiState.Loading -> LoadingScreen()
        is ArtistProfileScreenUiState.Success -> {}
        is ArtistProfileScreenUiState.Error -> ErrorScreen(
            message = uiState.message,
            onRetry = { userId?.let { viewModel.refreshProfile(it) } }
        )
    }
}

@Composable
 fun ProfileHeader(user: UserDTO) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = user.profilePicture,
            contentDescription = "Profile picture",
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = user.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "@${user.name.lowercase().replace(" ", "")}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
 fun StatSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        ProfileStat("32", "Artworks")
        ProfileStat("1.2K", "Followers")
        ProfileStat("723", "Following")
    }
}

@Composable
private fun ProfileStat(number: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = number,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
 fun GenreSection() {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(listOf("Abstract", "Surrealism", "Pop Art")) { genre ->
            CustomChip(
                text = genre,
                onClick = { /* Handle genre click */ }
            )
        }
    }
}
@Composable
 fun CustomChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier.clip(RoundedCornerShape(16.dp))
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
 fun ProfileDescriptionSection(
    description: String,
    url: String,
    followedBy: List<String>
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = description, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = url,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Followed by ${followedBy.joinToString(", ")} and 18 others",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
 fun ButtonSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = { /* Handle follow */ },
            modifier = Modifier.weight(1f)
        ) {
            Text("Follow")
        }
    }
}

@Composable
 fun ArtworkTabs(user: UserDTO) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = if (true) {
        listOf("Saved Artwork", "Posted Artwork")
    } else {
        listOf("Saved Artwork")
    }

    Column {
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        when (selectedTabIndex) {
            0 -> ArtworkGrid(isPosted = false)
            1 -> if (true) ArtworkGrid(isPosted = true)
        }
    }
}

@Composable
 fun ArtworkGrid(isPosted: Boolean) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(9) { index ->
            AsyncImage(
                model = if (isPosted) "https://example.com/posted_artwork_$index.jpg"
                else "https://example.com/saved_artwork_$index.jpg",
                contentDescription = "Artwork",
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}