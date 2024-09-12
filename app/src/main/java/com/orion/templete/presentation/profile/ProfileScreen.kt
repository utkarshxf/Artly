package com.orion.templete.presentation.profile


import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

import androidx.compose.ui.text.font.FontWeight.Companion.Medium

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orion.templete.presentation.components.AppIconButton
import com.orion.templete.data.model.UserDTO
import com.orion.templete.presentation.ui.theme.TempleteTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.android.material.chip.Chip

@Composable
fun ProfileScreen() {
    val user = UserDTO(
        artist = true,
        id = "12345",
        name = "John Doe",
        profilePicture = "https://example.com/profile.jpg",
        dob = "2024-09-03",
        gender = "Male",
        language = "English",
        countryIso2 = "US"
    )
    ProfileContent(user = user)
}

@Composable
private fun ProfileContent(user: UserDTO) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ProfileHeader(user)
        Spacer(modifier = Modifier.height(16.dp))
        StatSection()
        Spacer(modifier = Modifier.height(16.dp))
        GenreSection()
        Spacer(modifier = Modifier.height(16.dp))
        ProfileDescriptionSection(
            description = "Passionate artist exploring the boundaries of creativity. Join me on this artistic journey!",
            url = "https://www.instagram.com/${user.name.lowercase().replace(" ", "")}/",
            followedBy = listOf("artlover", "gallery123")
        )
        Spacer(modifier = Modifier.height(16.dp))
        ButtonSection()
        Spacer(modifier = Modifier.height(24.dp))
        ArtworkTabs(user)
    }
}

@Composable
private fun ProfileHeader(user: UserDTO) {
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
private fun StatSection() {
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
private fun GenreSection() {
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
private fun CustomChip(
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
private fun ProfileDescriptionSection(
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
private fun ButtonSection() {
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
private fun ArtworkTabs(user: UserDTO) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = if (user.artist) {
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
            1 -> if (user.artist) ArtworkGrid(isPosted = true)
        }
    }
}

@Composable
private fun ArtworkGrid(isPosted: Boolean) {
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


@Preview(showBackground = true , showSystemUi = true , )
@Composable
private fun prev() {
    TempleteTheme(darkTheme = true) {
        ProfileScreen()
    }
    
}