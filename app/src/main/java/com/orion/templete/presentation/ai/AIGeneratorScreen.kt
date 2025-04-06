package com.orion.templete.presentation.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIGeneratorScreen(
    navController: NavController,
    viewModel: AIViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.generatedImage.collectAsState()
    var showAdvancedOptions by remember { mutableStateOf(false) }
    var showMoreInputOptions by remember { mutableStateOf(false) }

    // Input states
    var prompt by remember { mutableStateOf("") }
    var strength by remember { mutableStateOf(0.75f) }
    var guidanceScale by remember { mutableStateOf(7.5f) }
    var steps by remember { mutableStateOf(50) }
    var seedValue by remember { mutableStateOf("") }
    var useRandomSeed by remember { mutableStateOf(true) }

    val focusManager = LocalFocusManager.current

    // Handle seed logic
    val seed = if (useRandomSeed) null else seedValue
    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        when (uiState) {
            is AIScreenUiState.Initial -> {
                InitialState(
                    prompt = prompt,
                    onPromptChange = { prompt = it },
                    onGenerate = {
                        focusManager.clearFocus()
                        viewModel.generateImage(
                            prompt = prompt,
                            strength = strength,
                            guidanceScale = guidanceScale,
                            steps = steps,
                            seed = seed
                        )
                    },
                    showAdvancedOptions = showAdvancedOptions,
                    onToggleAdvancedOptions = { showAdvancedOptions = !showAdvancedOptions },
                    showMoreInputOptions = showMoreInputOptions,
                    onToggleMoreInputOptions = { showMoreInputOptions = !showMoreInputOptions },
                    strength = strength,
                    onStrengthChange = { strength = it },
                    guidanceScale = guidanceScale,
                    onGuidanceScaleChange = { guidanceScale = it },
                    steps = steps,
                    onStepsChange = { steps = it },
                    seedValue = seedValue,
                    onSeedValueChange = { seedValue = it },
                    useRandomSeed = useRandomSeed,
                    onUseRandomSeedChange = { useRandomSeed = it },
                    modifier = Modifier.fillMaxSize()
                )
            }
            is AIScreenUiState.Loading -> {
                AnimatedLoading(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is AIScreenUiState.Success -> {
                val response = (uiState as AIScreenUiState.Success).value
                SuccessState(
                    imageUrl = response.image,
                    prompt = prompt,
                    onPromptChange = { prompt = it },
                    onGenerateNew = {
                        focusManager.clearFocus()
                        viewModel.generateImage(
                            image = response.image,
                            prompt = prompt,
                            strength = strength,
                            guidanceScale = guidanceScale,
                            steps = steps,
                            seed = seed
                        )
                    },
                    onReset = { viewModel.reset() },
                    showMoreInputOptions = showMoreInputOptions,
                    onToggleMoreInputOptions = { showMoreInputOptions = !showMoreInputOptions },
                    modifier = Modifier.fillMaxSize()
                )
            }
            is AIScreenUiState.Error -> {
                ErrorState(
                    message = (uiState as AIScreenUiState.Error).message,
                    onRetry = {
                        viewModel.generateImage(
                            prompt = prompt,
                            strength = strength,
                            guidanceScale = guidanceScale,
                            steps = steps,
                            seed = seed
                        )
                    },
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun InitialState(
    prompt: String,
    onPromptChange: (String) -> Unit,
    onGenerate: () -> Unit,
    showAdvancedOptions: Boolean,
    onToggleAdvancedOptions: () -> Unit,
    showMoreInputOptions: Boolean,
    onToggleMoreInputOptions: () -> Unit,
    strength: Float,
    onStrengthChange: (Float) -> Unit,
    guidanceScale: Float,
    onGuidanceScaleChange: (Float) -> Unit,
    steps: Int,
    onStepsChange: (Int) -> Unit,
    seedValue: String,
    onSeedValueChange: (String) -> Unit,
    useRandomSeed: Boolean,
    onUseRandomSeedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Click here to add an image",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        MaterialTheme.shapes.large
                    )
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .height(110.dp)
                ) {
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = onPromptChange,
                        placeholder = { Text("Enter a prompt...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(end = 8.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onGenerate() }),
                        minLines = 1,
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Row for input actions
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable { onToggleMoreInputOptions() }
                                .background(
                                    if (showMoreInputOptions)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else
                                        MaterialTheme.colorScheme.surface
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = "More options",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Submit button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable(enabled = prompt.isNotBlank()) { onGenerate() }
                                .background(
                                    if (prompt.isNotBlank())
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Generate",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                AnimatedVisibility(
                    visible = showMoreInputOptions,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Strength slider
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Strength: ${String.format("%.2f", strength)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Slider(
                                value = strength,
                                onValueChange = onStrengthChange,
                                valueRange = 0f..1f,
                                steps = 20
                            )
                            Text(
                                text = "Controls how much to transform the image",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        // Guidance scale slider
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Guidance Scale: ${String.format("%.1f", guidanceScale)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Slider(
                                value = guidanceScale,
                                onValueChange = onGuidanceScaleChange,
                                valueRange = 1f..20f,
                                steps = 38
                            )
                            Text(
                                text = "Higher values make image more closely follow the prompt",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        // Steps slider
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Steps: $steps",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Slider(
                                value = steps.toFloat(),
                                onValueChange = { onStepsChange(it.toInt()) },
                                valueRange = 10f..100f,
                                steps = 18
                            )
                            Text(
                                text = "More steps generally mean higher quality",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        // Seed options
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = useRandomSeed,
                                    onCheckedChange = onUseRandomSeedChange
                                )
                                Text(
                                    text = "Use random seed",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            AnimatedVisibility(visible = !useRandomSeed) {
                                OutlinedTextField(
                                    value = seedValue,
                                    onValueChange = onSeedValueChange,
                                    label = { Text("Seed value") },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(onDone = { onGenerate() }),
                                    singleLine = true
                                )
                            }

                            Text(
                                text = "Same seed produces similar results with the same prompt",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuccessState(
    imageUrl: String,
    prompt: String,
    onPromptChange: (String) -> Unit,
    onGenerateNew: () -> Unit,
    onReset: () -> Unit,
    showMoreInputOptions: Boolean,
    onToggleMoreInputOptions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Generated image
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Generated image",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        Spacer(modifier = Modifier.weight(1f))

        // ChatGPT-style input area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column {
                // Expanded options area
                AnimatedVisibility(
                    visible = showMoreInputOptions,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                MaterialTheme.shapes.large
                            )
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Reset button in expanded area
                        Button(
                            onClick = onReset,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start New Image")
                        }
                    }
                }

                // Input field with action buttons
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        OutlinedTextField(
                            value = prompt,
                            onValueChange = onPromptChange,
                            placeholder = { Text("Modify prompt for variation...") },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { onGenerateNew() }),
                            minLines = 1,
                            maxLines = 5,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                            )
                        )

                        // Row for input actions
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Options button
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .clickable { onToggleMoreInputOptions() }
                                    .background(
                                        if (showMoreInputOptions)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else
                                            MaterialTheme.colorScheme.surface
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = "More options",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Submit button
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .clickable(enabled = prompt.isNotBlank()) { onGenerateNew() }
                                    .background(
                                        if (prompt.isNotBlank())
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Generate variation",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedLoading(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Generating your image...",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "This may take a moment",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
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
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}