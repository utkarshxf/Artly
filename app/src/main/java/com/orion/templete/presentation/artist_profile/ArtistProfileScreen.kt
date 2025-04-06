package com.orion.templete.presentation.artist_profile

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.orion.templete.R
import com.orion.templete.data.model.artist_model.ArtistDTO
import com.orion.templete.data.model.artwork_model.ArtworkDTO
import com.orion.templete.presentation.common.ArtworkItem
import com.orion.templete.presentation.common.ErrorScreen
import com.orion.templete.presentation.common.ProfileHeader
import com.orion.templete.presentation.common.Screens
import com.orion.templete.presentation.components.GenreSection
import com.orion.templete.presentation.ui.theme.AppBarCollapsedHeight
import com.orion.templete.presentation.ui.theme.AppBarExpendedHeight
import com.orion.templete.util.ImageWithText
import com.orion.templete.util.SecureStorage
import com.orion.templete.util.extractYear
import kotlin.math.max
import kotlin.math.min
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun ArtistProfileScreen(artistId : String ,navController: NavController ,  viewModel: ArtistProfileViewModel = hiltViewModel()) {
    LaunchedEffect(key1 = Unit) {
        viewModel.getUserProfile(artistId)
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
            ProfileContent(artist = uiState.artist , navController)
        }

        is ArtistProfileScreenUiState.Error -> {
            ErrorScreen(
                message = uiState.message,
                onRetry = { viewModel.refreshProfile(artistId ) }
            )
        }
    }
}

@Composable
private fun ProfileContent(artist: ArtistDTO, navController: NavController, viewModel: ArtistProfileViewModel = hiltViewModel()) {
    LaunchedEffect(key1 = Unit) {
        viewModel.getArtistArtworks(artistId = artist.id)
    }
    val scrollGridState = rememberLazyGridState()
    val scrollListState = rememberLazyListState()
    val imageHeight = AppBarExpendedHeight - AppBarCollapsedHeight
    val maxOffset = with(LocalDensity.current) {
        imageHeight.roundToPx()
    } - WindowInsets.systemBars.getTop(LocalDensity.current)
    val showImagePopup = remember { mutableStateOf(false) }

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
    val context =  LocalContext.current
    if (showImagePopup.value) {
        ImagePopup(imageUrl = artist.image_url) {
            showImagePopup.value = false
        }
    }
    Column {
        Column(
            modifier = Modifier
                .height(animatedHeight.dp)
                .padding(horizontal = 12.dp)
                .offset { IntOffset(x = 0, y = animatedOffset.value) },
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            ProfileHeader(artist){
                showImagePopup.value = true
            }
            Spacer(modifier = Modifier.height(12.dp))
            ProfileDescriptionSection(
                artist = artist,
            )
            Spacer(modifier = Modifier.height(12.dp))
            ButtonSection(artistId = artist.id , artist.follow)
        }
        ArtworkContent(
            selectedTabIndex = selectedTabIndex
        )
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
                    0 -> ArtworkGrid(scrollGridState , uiState.artworks){
                        navController.currentBackStackEntry?.savedStateHandle?.set(key = "artworkId", value = it)
                        navController.navigate(Screens.ArtworkDetail.route)
                    }
                    1 -> ArtworkColumnList(scrollListState , uiState.artworks){
                        navController.currentBackStackEntry?.savedStateHandle?.set(key = "artworkId", value = it)
                        navController.navigate(Screens.ArtworkDetail.route)
                    }
                }
            }

            is ArtWorksUiState.Error -> {
                ErrorScreen(
                    message = uiState.message,
                    onRetry = { viewModel.refreshProfile(artist.id) }
                )
            }
        }
    }
}
@Composable
fun ImagePopup(imageUrl: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Artist Profile Image",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun ArtworkColumnList(scrollState: LazyListState, artworks: List<ArtworkDTO>, onItemClick: (String) -> Unit = {}) {
    LazyColumn(
        userScrollEnabled = true,
        state = scrollState,
        modifier = Modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(artworks) { index ->
            Image(
                painter = rememberAsyncImagePainter(model = index.imageUrl,
                    placeholder = painterResource(id = R.drawable.placeholder),
                    error = painterResource(id = R.drawable.placeholder),
                    fallback = painterResource(id = R.drawable.placeholder)),
                contentDescription = "Artwork",
                modifier = Modifier
                    .aspectRatio(1f)
                    .padding(4.dp)
                    .clickable {
                        index.id?.let { onItemClick(it) }
                    },
                contentScale = ContentScale.Fit
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = index.title ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = index.medium?:index.artist?:"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
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
            style = MaterialTheme.typography.bodyLarge,
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
private fun ProfileDescriptionSection(
    artist: ArtistDTO,
) {
    val context = LocalContext.current
    val intent = remember { Intent(Intent.ACTION_VIEW, Uri.parse(artist.wikipedia_url)) }
    Column(modifier = Modifier.fillMaxWidth()) {
        GenreSection(artist.art_movement)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = artist.description,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 6,
            overflow  = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = artist.wikipedia_url,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .clickable { context.startActivity(intent) }
        )
    }
}

@Composable
private fun ButtonSection(artistId: String, initialFollowState: Boolean , viewModel: ArtistProfileViewModel = hiltViewModel()) {
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
                    viewModel.followUser( artistId)
                } else {
                    viewModel.unfollowArtist( artistId)
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
    TabRow(
        selectedTabIndex = selectedTabIndex.value,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex.value]),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    ) {
        tabs.forEachIndexed { index, imageWithText ->
            Tab(
                selected = selectedTabIndex.value == index,
                onClick = { selectedTabIndex.value = index },
                text = {
                    Icon(
                        painter = painterResource(id = imageWithText.image),
                        contentDescription = imageWithText.text,
                        modifier = Modifier.size(28.dp)
                    )
                }
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}
@Composable
private fun ArtworkGrid(scrollState: LazyGridState, artworks: List<ArtworkDTO> , onItemClick: (String) -> Unit = {}) {
    LazyVerticalGrid(
        userScrollEnabled = true,
        state = scrollState,
        modifier = Modifier.fillMaxHeight(),
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(artworks) { index ->
//            Image(
//                painter = rememberAsyncImagePainter(model = index.image_url_compressed),
//                contentDescription = "Artwork",
//                modifier = Modifier
//                    .aspectRatio(1f)
//                    .clip(RoundedCornerShape(8.dp))
//                    .clickable {
//                        index.id?.let { onItemClick(it) }
//                    },
//                contentScale = ContentScale.Crop
//            )
            ArtworkItem(index , false){
                index.id?.let { onItemClick(it) }
            }
        }
    }
}
