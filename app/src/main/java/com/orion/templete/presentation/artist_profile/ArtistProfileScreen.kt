package com.orion.templete.presentation.artist_profile

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.orion.templete.data.model.artwork_model.ArtworkDTO
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
private fun ProfileContent(user: UserDTO ,  viewModel: ArtistProfileViewModel = hiltViewModel()) {
    LaunchedEffect(key1 = Unit) {
        viewModel.getArtistArtworks("test")
    }
    val scrollGridState = rememberLazyGridState()
    val scrollListState = rememberLazyListState()
    val imageHeight = AppBarExpendedHeight - AppBarCollapsedHeight
    val maxOffset = with(LocalDensity.current) {
        imageHeight.roundToPx()
    } - WindowInsets.systemBars.getTop(LocalDensity.current)

    // Calculate total offset based on which state is active
    val selectedTabIndex = remember { mutableStateOf(0) }
    val totalScrollOffset = remember(scrollGridState, scrollListState, selectedTabIndex.value) {
        derivedStateOf {
            when (selectedTabIndex.value) {
                0 -> {
                    val firstVisibleItem = scrollGridState.firstVisibleItemIndex
                    val firstVisibleItemOffset = scrollGridState.firstVisibleItemScrollOffset
                    (firstVisibleItem * maxOffset) + firstVisibleItemOffset
                }
                else -> {
                    val firstVisibleItem = scrollListState.firstVisibleItemIndex
                    val firstVisibleItemOffset = scrollListState.firstVisibleItemScrollOffset
                    (firstVisibleItem * maxOffset) + firstVisibleItemOffset
                }
            }
        }
    }.value

    val offset = min(totalScrollOffset, maxOffset)

    val animatedHeight by animateFloatAsState(
        targetValue = max(0f, (AppBarExpendedHeight.value - offset)),
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = Spring.StiffnessLow
        ),
        label = "height"
    )

    val animatedOffset = animateIntAsState(
        targetValue = -offset,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = Spring.StiffnessLow
        ),
        label = "offset"
    )

    Column {
        Column(
            modifier = Modifier
                .height(animatedHeight.dp)
                .padding(horizontal = 12.dp)
                .offset { IntOffset(x = 0, y = animatedOffset.value) },
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            ProfileHeader(user)
            Spacer(modifier = Modifier.height(12.dp))
            StatSection()
            Spacer(modifier = Modifier.height(12.dp))
            GenreSection()
            Spacer(modifier = Modifier.height(12.dp))
            ProfileDescriptionSection(
                description = "Passionate artist exploring the boundaries of creativity. Join me on this artistic journey!",
                url = "https://www.instagram.com/${user.name.lowercase().replace(" ", "")}/",
                followedBy = listOf("artlover", "gallery123")
            )
            Spacer(modifier = Modifier.height(12.dp))
            ButtonSection(user.follow)
        }
        ArtworkContent(
            selectedTabIndex = selectedTabIndex
        )
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
                when (selectedTabIndex.value) {
                    0 -> ArtworkGrid(scrollGridState , uiState.artworks)
                    1 -> ArtworkColumnList(scrollListState , uiState.artworks)
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
}


@Composable
fun ArtworkColumnList(scrollState: LazyListState, artworks: List<ArtworkDTO>) {
    LazyColumn(
        userScrollEnabled = true,
        state = scrollState,
        modifier = Modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(artworks) { index ->
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
fun ArtworkContent(selectedTabIndex: MutableState<Int>) {

    val tabs = listOf(
        ImageWithText(R.drawable.ic_gird, "Posted Artwork"),
        ImageWithText(R.drawable.ic_list, "Posted Artwork")
    )
    TabRow(selectedTabIndex = selectedTabIndex.value) {
        tabs.forEachIndexed { index, imageWithText ->
            Tab(
                selected = selectedTabIndex.value == index,
                onClick = { selectedTabIndex.value = index },
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
}
@Composable
private fun ArtworkGrid(scrollState: LazyGridState, artworks: List<ArtworkDTO>) {
    LazyVerticalGrid(
        userScrollEnabled = true,
        state = scrollState,
        modifier = Modifier.fillMaxHeight(),
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(artworks) { index ->
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


@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun prev() {
    TempleteTheme(darkTheme = false) {
        ArtistProfileScreen()
    }

}