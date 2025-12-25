package com.orion.templete.presentation.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.hilt.navigation.compose.hiltViewModel
import com.orion.templete.presentation.common.CustomSearchBar
import com.orion.templete.presentation.common.ArtistProfileCard
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.orion.templete.R
import com.orion.templete.data.model.artist_model.TopArtistProjection
import com.orion.templete.data.model.artwork_model.ArtworkDTO
import com.orion.templete.data.model.user_model.TopUserProjection
import com.orion.templete.data.model.user_model.TopCreatorProjection
import com.orion.templete.presentation.common.ArtworkItem
import com.orion.templete.presentation.common.Screens

@Composable
fun SearchScreen(navController: NavController , viewModel: SearchScreenViewModel = hiltViewModel()) {
    var query by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(false) }
    val searchResults = viewModel.searchResults.collectAsState()
    val popularArtworksState by viewModel.popularArtworks.collectAsState()
    val newArtworksState by viewModel.newArtworks.collectAsState()
    val recommendedForTodayState by viewModel.recommendedForToday.collectAsState()
    val todayBiggestHitState by viewModel.todayBiggestHit.collectAsState()
    val topUsersState by viewModel.topUsers.collectAsState()
    val topArtistsState by viewModel.topArtists.collectAsState()
    val topCreatorsState by viewModel.topCreators.collectAsState()
    SearchRow(
        header = {},
        search = {
            CustomSearchBar(
                onQueryChange = {
                    query = it
                    viewModel.searchArtist(it.replaceFirstChar { char -> char.uppercase() }) },
                query = query,
                active = active,
                onActiveChange = {
                    viewModel.searchArtist("")
                    active = it },
                placeholder = "Search your favorite artists",
                content = {
                    LazyColumn {
                        items(searchResults.value) {
                            ArtistProfileCard(it.name ?: "name", it.id ?: "id", it.imageUrl ?: "profilePicture") {
                                viewModel.artistSearched(it.name)
                                navController.currentBackStackEntry?.savedStateHandle?.set(key = "UserID", value = it.id)
                                navController.navigate(Screens.UserProfile.route)
                            }
                        }
                    }
                }
            )
        },
        content = {
            ArtworkFeed(
                popularArtworksState,
                newArtworksState,
                recommendedForTodayState,
                todayBiggestHitState,
                topUsersState,
                topArtistsState,
                topCreatorsState,
                navController
            )
        }
    )
}

@Composable
fun SearchRow(
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    search: (@Composable () -> Unit)? = null,
    content: (@Composable () -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxSize()) {
        header?.invoke()
        search?.invoke()
        content?.invoke()
    }
}

@Composable
fun ArtworkFeed(
    popularArtworksState: PopularArtworksUiState,
    newArtworksState: NewArtworksUiState,
    recommendedForTodayState: RecommendedForTodayUiState,
    todayBiggestHitState: TodayBiggestHitUiState,
    topUsersState: TopUsersUiState,
    topArtistsState: TopArtistsUiState,
    topCreatorsState: TopCreatorsUiState,
    navController: NavController
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp , horizontal = 8.dp),
    ) {
        item { TopUsers(topUsersState, navController) }
        item { PopularArtworks(popularArtworksState , navController) }
        item { TopArtists(topArtistsState, navController) }
        item { TopCreators(topCreatorsState, navController) }
        item { NewArtworks(newArtworksState , navController) }
        item { RecommendedForToday(recommendedForTodayState , navController) }
        item { TodaysBiggestHit(todayBiggestHitState , navController) }
    }
}

@Composable
fun TopUsers(state: TopUsersUiState, navController: NavController) {
    Column {
        when (state) {
            is TopUsersUiState.Loading -> {}
            is TopUsersUiState.Error -> Text("Error: ${state.message}")
            is TopUsersUiState.Success -> {
                if (state.users.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Today's Leaderboard",
                            style =  MaterialTheme.typography.bodyMedium,
                        )
                        TextButton(onClick = {
                            navController.navigate(Screens.Leaderboard.route)
                        }) {
                            Text("More",style =  MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    TopUserCard(
                        users = state.users
                    ) {
//                        navController.currentBackStackEntry?.savedStateHandle?.set(
//                            key = "UserID",
//                            value = it.userId
//                        )
//                        navController.navigate(Screens.UserProfile.route)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun TopUserCard(
    users: List<TopUserProjection>,
    onUserClick: (TopUserProjection) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank #2 (left, medium size, slightly lower)
        users.getOrNull(1)?.let {
            PodiumUserCard(
                user = it,
                rank = 2,
                cardWidth = 110.dp,
                cardHeight = 170.dp,
                verticalOffset = 12.dp,
                onClick = { onUserClick(it) }
            )
        }

        // Rank #1 (center, biggest, highest)
        users.getOrNull(0)?.let {
            PodiumUserCard(
                user = users[0],
                rank = 1,
                cardWidth = 150.dp,   // bigger width
                cardHeight = 220.dp,  // bigger height
                verticalOffset = 0.dp,
                onClick = { onUserClick(users[0]) }
            )
        }

        // Rank #3 (right, medium-small, slightly lower)
        users.getOrNull(2)?.let {
            PodiumUserCard(
                user = it,
                rank = 3,
                cardWidth = 110.dp,
                cardHeight = 170.dp,
                verticalOffset = 12.dp,
                onClick = { onUserClick(it) }
            )
        }
    }
}

@Composable
private fun PodiumUserCard(
    user: TopUserProjection,
    rank: Int,
    cardWidth: Dp,
    cardHeight: Dp,
    verticalOffset: Dp,
    onClick: () -> Unit
) {
    val modif = Modifier.padding(4.dp).background(
        color = if (rank == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
    )
    Box(
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight)
            .offset(y = verticalOffset)
    ) {
        Column(
            modifier = modif
                .fillMaxSize()
                .clickable { onClick()},
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if(rank == 1) "\uD83D\uDC51" else if(rank == 2) "🥈" else "🥉",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(8.dp))

            AsyncImage(
                model = user.profilePicture ?: "",
                contentDescription = user.name,
                modifier = Modifier
                    .size(if (rank == 1) 90.dp else 60.dp)
                .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = user.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )

            Text(
                text = "${formatViewCount(user.viewCount)} swipes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}



private fun formatViewCount(count: Long): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}

@Composable
fun TopArtists(state: TopArtistsUiState, navController: NavController) {
    Column {
        when (state) {
            is TopArtistsUiState.Loading -> {}
            is TopArtistsUiState.Error -> Text("Error: ${state.message}")
            is TopArtistsUiState.Success -> {
                if (state.artists.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Top Artists",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        TextButton(onClick = {
                            navController.navigate(Screens.TopArtists.route)
                        }) {
                            Text("More", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                LazyRow {
                    items(state.artists) { artist ->
                        TopArtistCard(artist) {
                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                key = "UserID",
                                value = artist.artistId
                            )
                            navController.navigate(Screens.UserProfile.route)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopArtistCard(
    artist: TopArtistProjection,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .padding(8.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (artist.imageUrl == null) {
            Image(
                painter = painterResource(R.drawable.placeholder),
                contentDescription = artist.name,
                modifier = Modifier
                    .size(100.dp),
                contentScale = ContentScale.Crop
            )
        } else {
            AsyncImage(
                model = artist.imageUrl,
                contentDescription = artist.name,
                modifier = Modifier
                    .size(100.dp),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = artist.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            textAlign = TextAlign.Center
        )
    }
}


@Composable
fun TopCreators(state: TopCreatorsUiState, navController: NavController) {
    Column {
        when (state) {
            is TopCreatorsUiState.Loading -> {}
            is TopCreatorsUiState.Error -> Text("Error: ${state.message}")
            is TopCreatorsUiState.Success -> {
                if (state.creators.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Creators",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        TextButton(onClick = {
                            navController.navigate(Screens.TopCreators.route)
                        }) {
                            Text("More", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                LazyRow {
                    items(state.creators) { creator ->
                        TopCreatorCard(creator) {
                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                key = "UserID",
                                value = creator.userId
                            )
                            navController.navigate(Screens.UserProfile.route)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopCreatorCard(
    creator: TopCreatorProjection,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .padding(8.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (creator.profilePicture == null) {
            Image(
                painter = painterResource(R.drawable.placeholder),
                contentDescription = creator.name,
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            AsyncImage(
                model = creator.profilePicture,
                contentDescription = creator.name,
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = creator.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            textAlign = TextAlign.Center
        )

        Text(
            text = creator.artistName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            maxLines = 1
        )
    }
}


@Composable
fun PopularArtworks(state: PopularArtworksUiState , navController  :NavController) {
    Column {
        when (state) {
            is PopularArtworksUiState.Loading ->  {}
            is PopularArtworksUiState.Error -> Text("Error: ${state.message}")
            is PopularArtworksUiState.Success -> {
                if(state.artworks.isNotEmpty()){
                    Text(text = "Popular Artworks", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                LazyRow(
                ) {
                    items(state.artworks) {
                        artwork -> ArtworkItem(artwork){ id->
                        navController.currentBackStackEntry?.savedStateHandle?.set(key = "artworkId", value = id )
                        navController.navigate(Screens.ArtworkDetail.route)
                    }
                    }
                }
            }
        }
    }
}

@Composable
fun NewArtworks(state: NewArtworksUiState, navController: NavController) {
    Column {
        when (state) {
            is NewArtworksUiState.Loading ->  {}
            is NewArtworksUiState.Error -> Text("Error: ${state.message}")
            is NewArtworksUiState.Success -> {
                if(state.listArtwork.isNotEmpty()){
                    Text(text = "New Arrival", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                LazyRow(
                ) {
                    items(state.listArtwork) { artwork -> ArtworkItem(artwork){
                        id->
                        navController.currentBackStackEntry?.savedStateHandle?.set(key = "artworkId", value = id )
                        navController.navigate(Screens.ArtworkDetail.route)
                    } }
                }
            }
        }
    }
}

@Composable
fun RecommendedForToday(state: RecommendedForTodayUiState, navController: NavController) {
    Column {
        when (state) {
            is RecommendedForTodayUiState.Loading -> {}
            is RecommendedForTodayUiState.Error -> Text("Error: ${state.message}")
            is RecommendedForTodayUiState.Success -> {
                if(state.listArtwork.isNotEmpty()){
                    Text(text = "Recommended for Today", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                LazyRow() {
                    items(state.listArtwork) { artwork -> ArtworkItem(artwork){
                        id->
                        navController.currentBackStackEntry?.savedStateHandle?.set(key = "artworkId", value = id )
                        navController.navigate(Screens.ArtworkDetail.route)
                    } }
                }
            }
        }
    }
}

@Composable
fun TodaysBiggestHit(state: TodayBiggestHitUiState, navController: NavController) {
    Column {
        when (state) {
            is TodayBiggestHitUiState.Loading -> {}
            is TodayBiggestHitUiState.Error -> Text("Error: ${state.message}")
            is TodayBiggestHitUiState.Success -> {
                Text(text = "Today's Biggest Hit", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxSize()
                    .height(200.dp)
                    .clickable {
                        navController.currentBackStackEntry?.savedStateHandle?.set(key = "artworkId", value = state.artwork.id )
                        navController.navigate(Screens.ArtworkDetail.route)
                    }
                ) {
                    AsyncImage(model = state.artwork.image_url_compressed,
                        contentDescription = state.artwork.description,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        Pair(0.4f, Transparent),
                                        Pair(1f, MaterialTheme.colorScheme.surface)
                                    )
                                )
                            )
                    )
                    Column(modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp), verticalArrangement = Arrangement.Bottom) {
                        state.artwork.title?.let { Text(text = it, style = MaterialTheme.typography.headlineSmall) }
                        state.artwork.artist?.let { Text(text = it, style = MaterialTheme.typography.bodyLarge) }
                    }
                }
            }
        }
    }
}
