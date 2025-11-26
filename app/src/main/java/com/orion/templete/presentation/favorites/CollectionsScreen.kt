package com.orion.templete.presentation.favorites

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.orion.templete.data.model.favorits.favoritesDTO
import com.orion.templete.presentation.common.Screens
import com.orion.templete.presentation.components.AnimatedPreloader
import com.orion.templete.presentation.ui.theme.TempleteTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionsScreen(
    navController: NavController,
    viewModel: CollectionViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val favoritesUiState = viewModel.favoritesUiState
    var addCollectionDialog by remember { mutableStateOf(false) }
    // Remember to fetch the favorites when the screen launches
    LaunchedEffect(key1 = viewModel.createFavoritesUiState) {
        if (viewModel.createFavoritesUiState is CreateFavoritesUiState.Success || viewModel.createFavoritesUiState is CreateFavoritesUiState.Error) {
            viewModel.getFavoritesByUserId()
            viewModel.resetState()
        }
    }
    LaunchedEffect(Unit) {
        viewModel.getFavoritesByUserId()
        viewModel.resetState()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Collections") },
//                navigationIcon = {
//                    IconButton(
//                        onClick = {navController.popBackStack()}
//                    ) {
//                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
//                    }
//                },
                actions = {
                    IconButton(onClick = {
                        addCollectionDialog = !addCollectionDialog
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add collection")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (favoritesUiState) {
                is FavoritesUiState.Loading -> {
                    AnimatedPreloader()
                }
                is FavoritesUiState.Success -> {
                    if (favoritesUiState.favorites.isEmpty()) {
                        EmptyCollectionsState(
                            onCreateCollection = {
                                addCollectionDialog = !addCollectionDialog
                            },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(favoritesUiState.favorites) { collection ->
                                CollectionCard(
                                    collection = collection,
                                    onClick = {
                                        navController.currentBackStackEntry?.savedStateHandle?.set(key = "collectionId", value = collection.id)
                                        navController.navigate(Screens.CollectionArtworksScreen.route)
                                    }
                                )
                            }
                        }
                    }
                }
                is FavoritesUiState.Error -> {
                    ErrorState(
                        message = favoritesUiState.message,
                        onRetry = { viewModel.getFavoritesByUserId() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
    if (addCollectionDialog){
        var isCreating by remember {
            mutableStateOf(false)
        }
        isCreating = when(viewModel.createFavoritesUiState){
            is CreateFavoritesUiState.Error -> false
            is CreateFavoritesUiState.Loading -> true
            is CreateFavoritesUiState.Ideal -> false
            is CreateFavoritesUiState.Success -> false
        }
        AddCollectionDialog(
            onDismiss = {
                addCollectionDialog = !addCollectionDialog
            },
            onCreateCollection = {
                viewModel.createNewFavorites(it)
            },
            isCreating = isCreating,
        )
    }
}
@Composable
fun AddCollectionDialog(
    onDismiss: () -> Unit,
    onCreateCollection: (favoritesDTO) -> Unit,
    isCreating: Boolean = false,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Collection") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )

                if (isCreating) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onCreateCollection(
                            favoritesDTO(
                                title = title,
                                description = description
                            )
                        )
                        onDismiss()
                    }
                },
                enabled = title.isNotBlank() && !isCreating
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        modifier = modifier
    )
}
@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
private fun EmptyCollectionsState(
    onCreateCollection: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No collections yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Create your first collection to save artwork",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onCreateCollection) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Collection")
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionCard(
    collection: favoritesDTO,
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = collection.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = collection.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}