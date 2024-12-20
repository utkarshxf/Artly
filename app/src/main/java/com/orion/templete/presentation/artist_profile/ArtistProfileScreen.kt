package com.orion.templete.presentation.artist_profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.orion.templete.R
import com.orion.templete.data.model.user_model.UserDTO
import com.orion.templete.presentation.common.ErrorScreen
import com.orion.templete.presentation.ui.theme.AppBarCollapsedHeight
import com.orion.templete.presentation.ui.theme.AppBarExpendedHeight
import com.orion.templete.presentation.ui.theme.TempleteTheme
import com.orion.templete.util.ImageWithText
import kotlin.math.max
import kotlin.math.min


@Composable
fun ArtistProfileScreen(viewModel: ArtistProfileViewModel = hiltViewModel()) {
    LaunchedEffect(key1 = Unit) {
        viewModel.getUserProfile("test" , "test4")
    }
    when (val uiState = viewModel.artistProfileScreenUiState) {
        is ArtistProfileScreenUiState.Loading -> {
            CircularProgressIndicator(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center)
            )
        }

        is ArtistProfileScreenUiState.Success -> {
            ProfileContent(user = uiState.user)
        }

        is ArtistProfileScreenUiState.Error -> {
            ErrorScreen(
                message = uiState.message,
                onRetry = { viewModel.refreshProfile("test" , "test4") }
            )
        }
    }
}

@Composable
private fun ProfileContent(user: UserDTO) {
    val scrollState = rememberLazyGridState()
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
        ButtonSection(user.follow)
        ArtworkContent(scrollState)
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
        ProfileState("32", "Artworks")
        ProfileState("1.2K", "Followers")
        ProfileState("723", "Following")
    }
}

@Composable
private fun ProfileState(number: String, label: String) {
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
private fun ButtonSection( initialFollowState: Boolean , viewModel: ArtistProfileViewModel = hiltViewModel()) {
    var isFollowing by remember { mutableStateOf(initialFollowState) }
    var isLoading by remember { mutableStateOf(false) }
    // Observe follow state
    LaunchedEffect(viewModel.followArtistUiState) {
        when (viewModel.followArtistUiState) {
            is FollowArtistUiState.Ideal -> isLoading = false
            is FollowArtistUiState.Loading -> isLoading = true
            is FollowArtistUiState.Success -> {
                isLoading = false
                isFollowing = true
            }
            is FollowArtistUiState.Error -> {
                isLoading = false
                // Optionally show error message
            }
        }
    }

    // Observe unfollow state
    LaunchedEffect(viewModel.unFollowArtistUiState) {
        when (viewModel.unFollowArtistUiState) {
            is UnFollowArtistUiState.Ideal -> isLoading = false
            is UnFollowArtistUiState.Loading -> isLoading = true
            is UnFollowArtistUiState.Success -> {
                isLoading = false
                isFollowing = false
            }
            is UnFollowArtistUiState.Error -> {
                isLoading = false
                // Optionally show error message
            }
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = {
                if (!isFollowing) {
                    viewModel.followUser("test4", "test")
                } else {
                    viewModel.unfollowArtist("test4", "test")
                }
            },
            modifier = Modifier.weight(1f),
            enabled = !isLoading
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = LocalContentColor.current
                    )
                }
                isFollowing -> Text("Following")
                else -> Text("Follow")
            }
        }
    }
}
@Composable
fun ArtworkContent(scrollState: LazyGridState) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf(
        ImageWithText(R.drawable.ic_gird, "Posted Artwork")
    )
    TabRow(selectedTabIndex = selectedTabIndex) {
        tabs.forEachIndexed { index, imageWithText ->
            Tab(
                selected = selectedTabIndex == index,
                onClick = { selectedTabIndex = index },
                text = {
                    Icon(
                        painter = painterResource(id = imageWithText.image),
                        contentDescription = imageWithText.text,
                        modifier = Modifier.size(32.dp)
                    )
                }
            )
        }
    }

    when (selectedTabIndex) {
        0 -> ArtworkGrid(scrollState)
    }
}
@Composable
private fun ArtworkGrid(scrollState: LazyGridState, viewModel: ArtistProfileViewModel = hiltViewModel() ) {
    LaunchedEffect(key1 = Unit) {
        viewModel.getArtistArtworks("test")
    }
    when (val uiState = viewModel.artWorksUiState) {
        is ArtWorksUiState.Loading -> {
            CircularProgressIndicator(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center)
            )
        }

        is ArtWorksUiState.Success -> {
            LazyVerticalGrid(
                state = scrollState,
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(uiState.artworks) { index ->
                    Image(
                        painter = rememberAsyncImagePainter(model = index.imageUrl),
                        contentDescription = "Artwork",
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { },
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        is ArtWorksUiState.Error -> {
            ErrorScreen(
                message = uiState.message,
                onRetry = { viewModel.refreshProfile("test" , "test4") }
            )
        }
    }

}


@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun prev() {
    TempleteTheme(darkTheme = false) {
        ArtistProfileScreen()
    }

}