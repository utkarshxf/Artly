package com.orion.templete.presentation.top_artists

import com.orion.templete.presentation.common.NameWithBadge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.orion.templete.data.model.artist_model.TopArtistProjection
import com.orion.templete.presentation.common.ArtistProfileCard
import com.orion.templete.presentation.common.CustomSearchBar
import com.orion.templete.presentation.common.Screens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopArtistsScreen(
    navController: NavController,
    viewModel: TopArtistsViewModel = hiltViewModel()
) {
    val topArtists = viewModel.topArtists.collectAsLazyPagingItems()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var active by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf(searchQuery) }
    val searchResults = viewModel.searchResults.collectAsState()

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
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

            Spacer(modifier = Modifier.height(16.dp))

            // Content
            Box(modifier = Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(topArtists.itemCount) { index ->
                        topArtists[index]?.let { artist ->
                            TopArtistGridCard(artist = artist) {
                                navController.currentBackStackEntry?.savedStateHandle?.set(
                                    key = "UserID",
                                    value = artist.artistId
                                )
                                navController.navigate(Screens.UserProfile.route)
                            }
                        }
                    }

                    // Loading state at the bottom
                    when (topArtists.loadState.append) {
                        is LoadState.Loading -> {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                        is LoadState.Error -> {
                            item {
                                Text(
                                    text = "Error loading more artists",
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        else -> {}
                    }
                }

                // Initial loading state
                if (topArtists.loadState.refresh is LoadState.Loading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                // Error state
                if (topArtists.loadState.refresh is LoadState.Error) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Error loading artists",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                // Empty state
                if (topArtists.loadState.refresh is LoadState.NotLoading && topArtists.itemCount == 0) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isEmpty()) "No artists found" else "No artists match your search",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TopArtistGridCard(
    artist: TopArtistProjection,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = artist.imageUrl ?: "",
                contentDescription = artist.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(Modifier.height(8.dp))

        NameWithBadge(
            name = artist.name,
            verified = artist.verified == true,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            textAlign = TextAlign.Center,
            badgeSize = 14.dp,
            horizontalArrangement = Arrangement.Center
        )
    }
}

private fun formatLikes(count: Long): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}
