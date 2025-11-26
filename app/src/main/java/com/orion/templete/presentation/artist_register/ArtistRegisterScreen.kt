package com.orion.templete.presentation.artist_register

import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.orion.templete.MainActivity
import com.orion.templete.R
import com.orion.templete.data.model.user_model.RegisterArtistRequest
import com.orion.templete.presentation.common.CustomTextField
import com.orion.templete.presentation.components.AnimatedPreloader
import com.orion.templete.presentation.ui.theme.LighterGray
import com.orion.templete.util.SecureStorage
import com.orion.templete.util.uploadImage
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistRegisterScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArtistRegisterScreenViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as MainActivity
    val imageCropper = remember { activity.getImageCropper() }
    val galleryLauncher = remember { activity.getGalleryLauncher() }

    // Required State variables
    var name by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    val nationality = SecureStorage(context).getUserDetails()?.countryIso2 ?: "in"

    // Optional State variables
    var education by remember { mutableStateOf("") }
    var awards by remember { mutableStateOf("") }
    var wikipediaUrl by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // Optional fields visibility
    var showEducation by remember { mutableStateOf(false) }
    var showAwards by remember { mutableStateOf(false) }
    var showWikipediaUrl by remember { mutableStateOf(false) }
    var showDescription by remember { mutableStateOf(false) }

    var pickedImageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val croppedImageUri by imageCropper.croppedImageUri.collectAsState(null)

    // Date picker states
    var showBirthDatePicker by remember { mutableStateOf(false) }
    val currentYear = LocalDate.now().year
    val january2004Millis = LocalDate.of(2004, 1, 1)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    val uiState = viewModel.registerArtistState
    val birthDatePickerState = rememberDatePickerState(
        initialDisplayedMonthMillis = january2004Millis,
        yearRange = 1900..currentYear
    )

    // Effects
    LaunchedEffect(croppedImageUri) {
        pickedImageUri = croppedImageUri
    }

    LaunchedEffect(Unit) {
        imageCropper.clearCroppedImageUri()
    }

    LaunchedEffect(birthDatePickerState.selectedDateMillis) {
        birthDatePickerState.selectedDateMillis?.let {
            val localDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
            birthDate = "${localDate.year}-${localDate.monthValue.toString().padStart(2, '0')}-${localDate.dayOfMonth.toString().padStart(2, '0')}"
        }
    }

    LaunchedEffect(uiState) {
        uiState.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }

        uiState.data?.let {
            Toast.makeText(context, "You Are A Creator Now. Happy Posting Your Art!!", Toast.LENGTH_SHORT).show()
            onNavigateBack()
        }
    }

    // Birth Date Picker Dialog
    if (showBirthDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showBirthDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showBirthDatePicker = false }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBirthDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = birthDatePickerState)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "Become an Artist",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Share your artworks and show your art to art enthusiasts worldwide.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Profile Picture
        Box(modifier = Modifier.padding(start = 8.dp), contentAlignment = Alignment.BottomEnd) {
            AsyncImage(
                model = pickedImageUri ?: R.drawable.person_outline_24px,
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .clickable { galleryLauncher.launchGallery() },
                contentScale = ContentScale.Crop
            )
            Image(
                painter = painterResource(id = R.drawable.ic_camara),
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .align(Alignment.BottomEnd)
                    .clickable { galleryLauncher.launchGallery() }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Required: Name Field
        CustomTextField(
            value = name,
            onValueChange = { name = it },
            hint = R.string.name_hint,
            keyboardType = KeyboardType.Text,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Required: Birth Date Field
        TextField(
            value = birthDate,
            onValueChange = { },
            readOnly = true,
            label = { Text("Birth Date*") },
            trailingIcon = {
                IconButton(onClick = { showBirthDatePicker = true }) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Select Birth Date"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = if (isSystemInDarkTheme()) {
                    MaterialTheme.colorScheme.surface
                } else {
                    LighterGray
                },
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Optional: Education
        OptionalField(
            isVisible = showEducation,
            onVisibilityChange = {
                showEducation = it
                if (!it) education = ""
            },
            fieldLabel = "Education",
            value = education,
            onValueChange = { education = it },
            hint = R.string.education_hint
        )

        // Optional: Awards
        OptionalField(
            isVisible = showAwards,
            onVisibilityChange = {
                showAwards = it
                if (!it) awards = ""
            },
            fieldLabel = "Awards",
            value = awards,
            onValueChange = { awards = it },
            hint = R.string.awards_hint
        )

        // Optional: Wikipedia URL
        OptionalField(
            isVisible = showWikipediaUrl,
            onVisibilityChange = {
                showWikipediaUrl = it
                if (!it) wikipediaUrl = ""
            },
            fieldLabel = "Wikipedia URL",
            value = wikipediaUrl,
            onValueChange = { wikipediaUrl = it },
            hint = R.string.wikipedia_url_hint,
            keyboardType = KeyboardType.Uri
        )

        // Optional: Description
        OptionalField(
            isVisible = showDescription,
            onVisibilityChange = {
                showDescription = it
                if (!it) description = ""
            },
            fieldLabel = "Description",
            value = description,
            onValueChange = { description = it },
            hint = R.string.description_hint,
            multiLine = true
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Submit Button
        Button(
            onClick = {
                if (pickedImageUri != null) {
                    uploadImage(pickedImageUri, context) { imageUrl ->
                        viewModel.registerAsArtist(
                            RegisterArtistRequest(
                                name = name,
                                birth_date = birthDate,
                                nationality = nationality,
                                education = education.ifBlank { null },
                                awards = awards.ifBlank { null },
                                image_url = imageUrl,
                                wikipedia_url = wikipediaUrl.ifBlank { null },
                                description = description.ifBlank { null }
                            )
                        )
                    }
                } else {
                    viewModel.registerAsArtist(
                        RegisterArtistRequest(
                            name = name,
                            birth_date = birthDate,
                            nationality = nationality,
                            education = education.ifBlank { null },
                            awards = awards.ifBlank { null },
                            image_url = null,
                            wikipedia_url = wikipediaUrl.ifBlank { null },
                            description = description.ifBlank { null }
                        )
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = MaterialTheme.shapes.medium,
            enabled = !uiState.isLoading && name.isNotBlank() && birthDate.isNotBlank()
        ) {
            if (uiState.isLoading) {
                AnimatedPreloader()
            } else {
                Text(
                    text = stringResource(id = R.string.save_button_label),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun OptionalField(
    isVisible: Boolean,
    onVisibilityChange: (Boolean) -> Unit,
    fieldLabel: String,
    value: String,
    onValueChange: (String) -> Unit,
    hint: Int,
    multiLine: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Column {
            CustomTextField(
                value = value,
                onValueChange = onValueChange,
                hint = hint,
                keyboardType = keyboardType,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(
                        onClick = { onVisibilityChange(false) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove $fieldLabel"
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (!isVisible) {
        Column {
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
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
