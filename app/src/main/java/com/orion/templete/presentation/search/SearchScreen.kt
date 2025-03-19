package com.orion.templete.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.hilt.navigation.compose.hiltViewModel
import com.orion.templete.presentation.common.CustomSearchBar
import com.orion.templete.presentation.common.UserSearchCard
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.orion.templete.data.model.artwork_model.ArtworkDTO
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
    SearchRow(
        header = {},
        search = {
            CustomSearchBar(
                onQueryChange = { query = it },
                query = query,
                active = active,
                onActiveChange = {
                    active = it
                    viewModel.searchArtist(query) },
                placeholder = "Search",
                content = {
                    LazyColumn {
                        items(searchResults.value) {
                            UserSearchCard(it.name ?: "name", it.id ?: "id", it.imageUrl ?: "profilePicture") {
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
                navController
            )
        }
    )
}

val dummyArtworks = listOf(
    ArtworkDTO(
        description = "A beautiful landscape painting",
        id = "1",
        imageUrl = "https://example.com/landscape.jpg",
        title = "Serene Valley"
    ),
    ArtworkDTO(
        description = "Abstract expressionist piece",
        id = "2",
        imageUrl = "https://example.com/abstract.jpg",
        title = "Chaos and Order"
    ),
    ArtworkDTO(
        description = "Portrait of a mysterious woman",
        id = "3",
        imageUrl = "https://example.com/portrait.jpg",
        title = "Enigmatic Gaze"
    ),
    ArtworkDTO(
        description = "Surrealist dreamscape",
        id = "4",
        imageUrl = "https://example.com/surreal.jpg",
        title = "Floating Memories"
    ),
    ArtworkDTO(
        description = "Minimalist geometric composition",
        id = "5",
        imageUrl = "https://example.com/geometric.jpg",
        title = "Shapes in Harmony"
    )
)



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
    navController: NavController
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp , horizontal = 8.dp),
    ) {
        item { PopularArtworks(popularArtworksState , navController) }
        item { NewArtworks(newArtworksState , navController) }
        item { RecommendedForToday(recommendedForTodayState , navController) }
        item { TodaysBiggestHit(todayBiggestHitState , navController) }
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
                    Text(text = "Popular Artworks", style = MaterialTheme.typography.labelLarge)
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
                    Text(text = "New Arrival", style = MaterialTheme.typography.labelLarge)
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
                    Text(text = "Recommended for Today", style = MaterialTheme.typography.labelLarge)
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
                Text(text = "Today's Biggest Hit", style = MaterialTheme.typography.labelLarge)
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

