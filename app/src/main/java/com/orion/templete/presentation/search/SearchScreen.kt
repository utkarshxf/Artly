package com.orion.templete.presentation.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.orion.templete.presentation.common.CustomSearchBar
import com.orion.templete.presentation.common.UserSearchCard
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.orion.templete.data.model.artwork_model.ArtworkDTO
import com.orion.templete.presentation.common.ArtworkItem

@Composable
fun SearchScreen(viewModel: SearchScreenViewModel = hiltViewModel() , onUserClick: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(false) }
    val searchResults = viewModel.searchResults.collectAsState()
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
                            UserSearchCard(it.name ?: "name", it.id ?: "id", it.profilePicture ?: "profilePicture") {
                                onUserClick(it.id ?: "id")
                            }
                        }
                    }
                }
            )
        },
        content = {ArtworkFeed()}
    )
}

val dummyArtworks = listOf(
    ArtworkDTO(
        description = "A beautiful landscape painting",
        id = "1",
        imageUrl = "https://example.com/landscape.jpg",
        madeWith = "Oil on canvas",
        name = "Serene Valley"
    ),
    ArtworkDTO(
        description = "Abstract expressionist piece",
        id = "2",
        imageUrl = "https://example.com/abstract.jpg",
        madeWith = "Acrylic on wood",
        name = "Chaos and Order"
    ),
    ArtworkDTO(
        description = "Portrait of a mysterious woman",
        id = "3",
        imageUrl = "https://example.com/portrait.jpg",
        madeWith = "Watercolor",
        name = "Enigmatic Gaze"
    ),
    ArtworkDTO(
        description = "Surrealist dreamscape",
        id = "4",
        imageUrl = "https://example.com/surreal.jpg",
        madeWith = "Digital painting",
        name = "Floating Memories"
    ),
    ArtworkDTO(
        description = "Minimalist geometric composition",
        id = "5",
        imageUrl = "https://example.com/geometric.jpg",
        madeWith = "Screen printing",
        name = "Shapes in Harmony"
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
fun ArtworkFeed() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item { PopularArtworks() }
        item { NewArtworks() }
        item { RecommendedForToday() }
        item { TodaysBiggestHit() }
    }
}

@Composable
fun PopularArtworks() {
    Column {
        Text(
            text = "Popular Artworks",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(dummyArtworks) { artwork ->
                ArtworkItem(artwork)
            }
        }
    }
}

@Composable
fun NewArtworks() {
    Column {
        Text(
            text = "New Arrival",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(dummyArtworks.shuffled()) { artwork ->
                ArtworkItem(artwork)
            }
        }
    }
}

@Composable
fun RecommendedForToday() {
    Column {
        Text(
            text = "Recommended for Today",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(dummyArtworks.shuffled()) { artwork ->
                ArtworkItem(artwork)
            }
        }
    }
}

@Composable
fun TodaysBiggestHit() {
    Column {
        Text(
            text = "Today's Biggest Hit",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(200.dp)
        ) {
            val biggestHit = dummyArtworks.random()
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = biggestHit.imageUrl,
                    contentDescription = biggestHit.description,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = biggestHit.name ?: "Untitled",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = biggestHit.madeWith ?: "Unknown medium",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

