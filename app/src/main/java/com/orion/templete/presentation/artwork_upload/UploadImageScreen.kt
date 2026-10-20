package com.orion.templete.presentation.artwork_upload

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.orion.templete.presentation.ui.theme.ButtonHeight
import com.orion.templete.util.ImageCropper
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadImageScreen(
    navController: NavController,
    viewModel: UploadViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    // State from ViewModel
    val uploadUiState = viewModel.uploadUiState
    val originalImageUri by viewModel.originalImageUri.collectAsState()
    val croppedImageUri by viewModel.croppedImageUri.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()
    val genreSearchResults by viewModel.genreSearchResults.collectAsState()

    val activity = LocalContext.current as androidx.activity.ComponentActivity
    val imageCropper = remember { ImageCropper(activity) }

    // UCrop launcher
    val uCropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            imageCropper.handleActivityResult(result.data)
        }
    }

    // Image picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val cropIntent = imageCropper.createCropIntent(it)
            uCropLauncher.launch(cropIntent)
        }
    }

    // Launch gallery only when there's no image selected
    LaunchedEffect(originalImageUri) {
        if (originalImageUri == null) {
            galleryLauncher.launch("image/*")
        }
    }

    // Collect cropped image URI
    LaunchedEffect(Unit) {
        imageCropper.croppedImageUri.collectLatest { uri ->
            uri?.let { viewModel.setCroppedImageUri(it) }
        }
    }

    // Handle UI state changes
    LaunchedEffect(uploadUiState) {
        when (uploadUiState) {
            is UploadUiState.Error -> {
                snackbarHostState.showSnackbar(uploadUiState.message)
            }
            is UploadUiState.Success -> {
                snackbarHostState.showSnackbar("Artwork uploaded successfully!")
                navController.popBackStack()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("New Artwork") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Image Preview
            ImagePreviewSection(
                originalImageUri = originalImageUri,
                croppedImageUri = croppedImageUri,
                uploadProgress = uploadProgress,
                onSelectImageClick = { galleryLauncher.launch("image/*") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Artwork Details Form
            ArtworkDetailsForm(
                viewModel = viewModel,
                genreSearchResults = genreSearchResults,
                onSubmit = {
                    focusManager.clearFocus()
                    viewModel.submitArtwork()
                }
            )
        }
    }
}

@Composable
fun ImagePreviewSection(
    originalImageUri: Uri?,
    croppedImageUri: Uri?,
    uploadProgress: Float,
    onSelectImageClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val imageUri = croppedImageUri ?: originalImageUri

            if (imageUri != null) {
                // Image preview
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Artwork preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                // Upload progress indicator
                if (uploadProgress < 1f && uploadProgress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                progress = { uploadProgress },
                                modifier = Modifier.size(60.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Uploading... ${(uploadProgress * 100).toInt()}%",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                // Change image button
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    IconButton(
                        onClick = onSelectImageClick,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Change image"
                        )
                    }
                }
            } else {
                // Empty state
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(onClick = onSelectImageClick)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add image",
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Select an image",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun ArtworkDetailsForm(
    viewModel: UploadViewModel,
    genreSearchResults: List<GenreItem>,
    onSubmit: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var showGenreDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        OutlinedTextField(
            value = viewModel.title,
            onValueChange = { viewModel.updateTitle(it) },
            label = { Text("Title*") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            isError = viewModel.uploadUiState is UploadUiState.Error && viewModel.title.isBlank()
        )

        // Genre Section
        GenreSection(
            viewModel = viewModel,
            genreSearchResults = genreSearchResults,
            showGenreDropdown = showGenreDropdown,
            onShowGenreDropdownChange = { showGenreDropdown = it },
            focusRequester = focusRequester
        )

        // Description
        OutlinedTextField(
            value = viewModel.description,
            onValueChange = { viewModel.updateDescription(it) },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5
        )

        // Optional Fields
        OptionalFieldsSection(viewModel = viewModel)

        Spacer(modifier = Modifier.height(16.dp))

        // Submit Button
        SubmitButton(
            uploadUiState = viewModel.uploadUiState,
            onSubmit = onSubmit
        )
    }
}

@Composable
private fun GenreSection(
    viewModel: UploadViewModel,
    genreSearchResults: List<GenreItem>,
    showGenreDropdown: Boolean,
    onShowGenreDropdownChange: (Boolean) -> Unit,
    focusRequester: FocusRequester
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = viewModel.genreName,
            onValueChange = { viewModel.updateGenreName(it) },
            label = { Text("Genre*") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { onShowGenreDropdownChange(true) }
            ),
            trailingIcon = {
                IconButton(onClick = { onShowGenreDropdownChange(!showGenreDropdown) }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Toggle genre dropdown"
                    )
                }
            },
            isError = viewModel.uploadUiState is UploadUiState.Error && viewModel.selectedGenre == null
        )

        // Genre dropdown
        AnimatedVisibility(
            visible = showGenreDropdown && genreSearchResults.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                LazyColumn {
                    items(genreSearchResults) { genre ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectGenre(genre)
                                    viewModel.updateGenreName(genre.name)
                                    onShowGenreDropdownChange(false)
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = genre.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }

        // Selected genre chip
        viewModel.selectedGenre?.let { genre ->
            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = genre.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = {
                        viewModel.selectGenre(null)
                        viewModel.updateGenreName("")
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove genre",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionalFieldsSection(viewModel: UploadViewModel) {
    var showDimensions by remember { mutableStateOf(false) }
    var showCurrentLocation by remember { mutableStateOf(false) }
    var showPeriodStyle by remember { mutableStateOf(false) }
    var showArtMovement by remember { mutableStateOf(false) }
    var showLicenseInfo by remember { mutableStateOf(false) }
    var showSourceUrl by remember { mutableStateOf(false) }
    var medium by remember { mutableStateOf(false) }


    // Medium
    OptionalField(
        isVisible = medium,
        onVisibilityChange = { medium = it },
        value = viewModel.medium,
        onValueChange = { viewModel.updateMedium(it) },
        fieldLabel = "Medium",
    )

    // Dimensions
    OptionalField(
        isVisible = showDimensions,
        onVisibilityChange = { showDimensions = it },
        fieldLabel = "Dimensions",
        value = viewModel.dimensions,
        onValueChange = { viewModel.updateDimensions(it) }
    )

    // Current Location
    OptionalField(
        isVisible = showCurrentLocation,
        onVisibilityChange = { showCurrentLocation = it },
        fieldLabel = "Current Location",
        value = viewModel.currentLocation,
        onValueChange = { viewModel.updateCurrentLocation(it) }
    )

    // Period/Style
    OptionalField(
        isVisible = showPeriodStyle,
        onVisibilityChange = { showPeriodStyle = it },
        fieldLabel = "Period/Style",
        value = viewModel.periodStyle,
        onValueChange = { viewModel.updatePeriodStyle(it) }
    )

    // Art Movement
    OptionalField(
        isVisible = showArtMovement,
        onVisibilityChange = { showArtMovement = it },
        fieldLabel = "Art Movement",
        value = viewModel.artMovement,
        onValueChange = { viewModel.updateArtMovement(it) }
    )

    // License Info
    OptionalField(
        isVisible = showLicenseInfo,
        onVisibilityChange = { showLicenseInfo = it },
        fieldLabel = "License Information",
        value = viewModel.licenseInfo,
        onValueChange = { viewModel.updateLicenseInfo(it) },
        multiLine = true
    )

    // Source URL
    OptionalField(
        isVisible = showSourceUrl,
        onVisibilityChange = { showSourceUrl = it },
        fieldLabel = "Source URL",
        value = viewModel.sourceUrl,
        onValueChange = { viewModel.updateSourceUrl(it) },
        keyboardType = KeyboardType.Uri
    )
}

@Composable
private fun OptionalField(
    isVisible: Boolean,
    onVisibilityChange: (Boolean) -> Unit,
    fieldLabel: String,
    value: String,
    onValueChange: (String) -> Unit,
    multiLine: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(fieldLabel) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = !multiLine,
            minLines = if (multiLine) 2 else 1,
            maxLines = if (multiLine) 4 else 1,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            trailingIcon = {
                IconButton(
                    onClick = {
                        onVisibilityChange(false)
                        onValueChange("")
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove $fieldLabel"
                    )
                }
            }
        )
    }

    if (!isVisible) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onVisibilityChange(true) }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = fieldLabel,
                style = MaterialTheme.typography.bodyLarge
            )
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add $fieldLabel"
            )
        }
    }
}

@Composable
private fun SubmitButton(
    uploadUiState: UploadUiState,
    onSubmit: () -> Unit
) {
    Button(
        onClick = onSubmit,
        modifier = Modifier
            .fillMaxWidth()
            .height(ButtonHeight),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium,
        enabled = uploadUiState !is UploadUiState.Loading
    ) {
        if (uploadUiState is UploadUiState.Loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Submitting...")
        } else {
            Text("Submit Artwork")
        }
    }
}
