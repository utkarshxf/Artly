package com.orion.templete.presentation.upload

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import android.net.Uri
import androidx.compose.animation.core.*
import com.orion.templete.presentation.components.AnimatedPreloader
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    onUploadComplete: (Uri) -> Unit = {},
    onNavigateToCheckout: () -> Unit= {}
) {
    // State
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }
    var selectedSize by remember { mutableStateOf<PrintSize?>(null) }
    var selectedFrame by remember { mutableStateOf<FrameStyle?>(null) }
    var showSizeGuide by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Image picker launcher
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isLoading = true
            selectedImageUri = it
            scope.launch {
                snackbarHostState.showSnackbar("Image uploaded successfully!")
                isLoading = false
                showPreview = true
            }
            onUploadComplete(it)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Custom Print",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    IconButton(onClick = { /* Show help */ }) {
                        Icon(Icons.Default.Phone, "Help")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Upload Section with Animation
            AnimatedVisibility(
                visible = !showPreview,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                UploadSection(
                    isLoading = isLoading,
                    onUploadClick = { imagePicker.launch("image/*") }
                )
            }

            // Image Preview with Edit Options
            AnimatedVisibility(
                visible = showPreview,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                ImagePreviewSection(
                    imageUri = selectedImageUri,
                    onEditClick = { showPreview = false },
                    selectedSize = selectedSize,
                    selectedFrame = selectedFrame
                )
            }

            // Print Sizes
            PrintSizeSelector(
                selectedSize = selectedSize,
                onSizeSelected = { selectedSize = it },
                onInfoClick = { showSizeGuide = true }
            )

            // Frame Styles
            FrameStyleSelector(
                selectedFrame = selectedFrame,
                onFrameSelected = { selectedFrame = it }
            )

            // Price and Checkout Section
            PriceAndCheckoutSection(
                selectedSize = selectedSize,
                selectedFrame = selectedFrame,
                enabled = selectedImageUri != null && selectedSize != null && selectedFrame != null,
                onCheckoutClick = onNavigateToCheckout
            )
        }
    }

    // Size Guide Dialog
    if (showSizeGuide) {
        SizeGuideDialog(onDismiss = { showSizeGuide = false })
    }
}

@Composable
private fun UploadSection(
    isLoading: Boolean,
    onUploadClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .shadow(8.dp)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = !isLoading) { onUploadClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            AnimatedPreloader()
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Drop your artwork here or click to upload",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    "Supported formats: JPG, PNG, HEIC",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ImagePreviewSection(
    imageUri: Uri?,
    onEditClick: () -> Unit,
    selectedSize: PrintSize?,
    selectedFrame: FrameStyle?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = imageUri,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentScale = ContentScale.Fit
            )
            IconButton(
                onClick = onEditClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(Icons.Default.Edit, "Edit")
            }
        }
    }
}

@Composable
private fun PrintSizeSelector(
    selectedSize: PrintSize?,
    onSizeSelected: (PrintSize) -> Unit,
    onInfoClick: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Select Size",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onInfoClick) {
                Icon(Icons.Default.Info, "Size Guide")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(PrintSize.values()) { size ->
                SizeCard(
                    size = size,
                    isSelected = size == selectedSize,
                    onClick = { onSizeSelected(size) }
                )
            }
        }
    }
}

@Composable
private fun FrameStyleSelector(
    selectedFrame: FrameStyle?,
    onFrameSelected: (FrameStyle) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "Choose Frame",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(FrameStyle.values()) { frame ->
                FrameCard(
                    frame = frame,
                    isSelected = frame == selectedFrame,
                    onClick = { onFrameSelected(frame) }
                )
            }
        }
    }
}

@Composable
private fun PriceAndCheckoutSection(
    selectedSize: PrintSize?,
    selectedFrame: FrameStyle?,
    enabled: Boolean,
    onCheckoutClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Subtotal")
                Text("calculatePrice(selectedSize, selectedFrame)")
            }
            Button(
                onClick = onCheckoutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = enabled,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Continue to Checkout",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SizeCard(
    size: PrintSize,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick),
        border = BorderStroke(
            width = 2.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                size.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                size.dimensions,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                size.price,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun FrameCard(
    frame: FrameStyle,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick),
        border = BorderStroke(
            width = 2.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(frame.color, RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                frame.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                frame.price,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SizeGuideDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Print Size Guide") },
        text = {
            Column {
                Text("Our print sizes are optimized for standard frame sizes and wall spaces:")
                Spacer(modifier = Modifier.height(8.dp))
                PrintSize.values().forEach { size ->
                    Text("• ${size.displayName}: ${size.dimensions}")
                    Text(size.description, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it")
            }
        }
    )
}

enum class PrintSize(
    val displayName: String,
    val dimensions: String,
    val price: String,
    val description: String
) {
    SMALL("Small", "12\" × 16\"", "$49.99", "Perfect for desks and small walls"),
    MEDIUM("Medium", "18\" × 24\"", "$79.99", "Most popular size for living spaces"),
    LARGE("Large", "24\" × 36\"", "$129.99", "Makes a bold statement"),
    XLARGE("Extra Large", "36\" × 48\"", "$199.99", "Gallery-sized impact")
}

enum class FrameStyle(
    val displayName: String,
    val price: String,
    val color: Color
) {
    NATURAL("Natural Wood", "$89.99", Color(0xFFD4B59E)),
    BLACK("Matte Black", "$79.99", Color(0xFF2C2C2C)),
    WHITE("Clean White", "$79.99", Color(0xFFF5F5F5)),
    GOLD("Gold", "$99.99", Color(0xFFD4AF37))
}