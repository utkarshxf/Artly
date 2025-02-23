package com.orion.templete.presentation.favorites

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.orion.templete.presentation.ui.theme.TempleteTheme

data class Artwork(
    val id: String,
    val imageUrl: String,
    val title: String,
    val artist: String
)

data class CollectionDetail(
    val id: String,
    val name: String,
    val artworkCount: Int,
    val artworks: List<Artwork>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionArtworksScreen(
    collection: CollectionDetail,
    onBackClick: () -> Unit,
    onArtworkClick: (Artwork) -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = collection.name,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "${collection.artworkCount} artworks",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    CollectionMoreMenu(
                        showMenu = showMenu,
                        onDismiss = { showMenu = false },
                        onRename = { /* Handle rename */ },
                        onShare = { /* Handle share */ },
                        onDownload = { /* Handle download */ },
                        onAddToAlbum = { /* Handle add to album */ },
                        onRemoveFromSaved = { /* Handle remove from saved */ },
                        onDelete = { /* Show delete confirmation dialog */ }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            items(collection.artworks) { artwork ->
                ArtworkCard(
                    artwork = artwork,
                    onClick = { onArtworkClick(artwork) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArtworkCard(
    artwork: Artwork,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = artwork.imageUrl,
                contentDescription = artwork.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Gradient overlay for text visibility (optional)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            ) {
                Column(
                    modifier = Modifier
                        .padding(8.dp)
                ) {
                    Text(
                        text = artwork.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = artwork.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun CollectionArtworksScreenPreview() {
    val sampleCollection = CollectionDetail(
        id = "1",
        name = "My Favorite Artworks",
        artworkCount = 6,
        artworks = listOf(
            Artwork("1", "url1", "Starry Night", "Vincent van Gogh"),
            Artwork("2", "url2", "Mona Lisa", "Leonardo da Vinci"),
            Artwork("3", "url3", "The Scream", "Edvard Munch"),
            Artwork("4", "url4", "Girl with a Pearl Earring", "Johannes Vermeer"),
            Artwork("5", "url5", "The Persistence of Memory", "Salvador Dalí"),
            Artwork("6", "url6", "The Kiss", "Gustav Klimt")
        )
    )

    TempleteTheme {
        CollectionArtworksScreen(
            collection = sampleCollection,
            onBackClick = {},
            onArtworkClick = {},
            onMoreClick = {}
        )
    }
}