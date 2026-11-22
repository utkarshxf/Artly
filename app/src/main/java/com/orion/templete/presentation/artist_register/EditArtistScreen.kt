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
import com.orion.templete.util.uploadImage
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditArtistScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditArtistViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as MainActivity
    val imageCropper = remember { activity.getImageCropper() }
    val galleryLauncher = remember { activity.getGalleryLauncher() }

    // State variables for artist data
    var name by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var education by remember { mutableStateOf("") }
    var awards by remember { mutableStateOf("") }
    var wikipediaUrl by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var nationality by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }

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

    val initialLoadState = viewModel.initialLoadState
    val loadArtistDataState by viewModel.loadArtistDataState.collectAsState()
    val updateArtistState by viewModel.updateArtistState.collectAsState()
    val currentArtistData by viewModel.currentArtistData.collectAsState()
    val birthDatePickerState = rememberDatePickerState(
        initialDisplayedMonthMillis = january2004Millis,
        yearRange = 1900..currentYear
    )

    // Effects
    LaunchedEffect(Unit) {
        imageCropper.clearCroppedImageUri()
        // Business Logic: Load artist data
        viewModel.loadArtistData()
    }

    LaunchedEffect(loadArtistDataState) {
        when (loadArtistDataState) {
            is LoadArtistDataState.Success -> {
                val artistData = (loadArtistDataState as LoadArtistDataState.Success).data
                name = artistData.name ?: ""
                birthDate = artistData.birth_date ?: ""
                education = artistData.education ?: ""
                awards = artistData.awards ?: ""
                wikipediaUrl = artistData.wikipedia_url ?: ""
                description = artistData.description ?: ""
                nationality = artistData.nationality ?: ""
                imageUrl = artistData.image_url ?: ""

                // Show optional fields if they have data
                showEducation = education.isNotEmpty()
                showAwards = awards.isNotEmpty()
                showWikipediaUrl = wikipediaUrl.isNotEmpty()
                showDescription = description.isNotEmpty()
            }
            is LoadArtistDataState.Error -> {
                Toast.makeText(context, (loadArtistDataState as LoadArtistDataState.Error).message, Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }


    LaunchedEffect(croppedImageUri) {
        pickedImageUri = croppedImageUri
    }

    LaunchedEffect(birthDatePickerState.selectedDateMillis) {
        birthDatePickerState.selectedDateMillis?.let {
            val localDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
            birthDate = "${localDate.year}-${localDate.monthValue.toString().padStart(2, '0')}-${localDate.dayOfMonth.toString().padStart(2, '0')}"
        }
    }

    // MVVM: Handle update responses
    LaunchedEffect(updateArtistState) {
        when (updateArtistState) {
            is UpdateArtistState.Success -> {
                Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                onNavigateBack()
            }
            is UpdateArtistState.Error -> {
                Toast.makeText(context, (updateArtistState as UpdateArtistState.Error).message, Toast.LENGTH_SHORT).show()
            }
            else -> {}
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

    // MVVM: Show loading state
    if (initialLoadState is InitialLoadState.Loading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
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
            text = "Edit Artist Profile",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Update your artist profile information",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Profile Picture
        Box(modifier = Modifier.padding(start = 8.dp), contentAlignment = Alignment.BottomEnd) {
            AsyncImage(
                model = pickedImageUri ?: imageUrl.ifEmpty { R.drawable.person_outline_24px },
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

        // Name Field
        CustomTextField(
            value = name,
            onValueChange = { name = it },
            hint = R.string.name_hint,
            keyboardType = KeyboardType.Text,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Birth Date Field
        TextField(
            value = birthDate,
            onValueChange = { },
            readOnly = true,
            label = { Text("Birth Date") },
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
            fieldLabel = "Profession",
            value = education,
            onValueChange = { education = it },
            hint = R.string.education_hint
        )

        // Optional: Highlights (stored as "awards")
        OptionalField(
            isVisible = showAwards,
            onVisibilityChange = {
                showAwards = it
                if (!it) awards = ""
            },
            fieldLabel = "Highlights",
            value = awards,
            onValueChange = { awards = it },
            hint = R.string.awards_hint
        )

        // Optional: personal website / portfolio (stored as "wikipedia_url")
        OptionalField(
            isVisible = showWikipediaUrl,
            onVisibilityChange = {
                showWikipediaUrl = it
                if (!it) wikipediaUrl = ""
            },
            fieldLabel = "Website",
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

        // Update Button
        Button(
            onClick = {
                val finalImageUrl = if (pickedImageUri != null) {
                    uploadImage(pickedImageUri, context) { uploadedUrl ->
                        // Business Logic: Call ViewModel to update artist
                        viewModel.updateArtist(
                            RegisterArtistRequest(
                                id = currentArtistData?.id ?: "",
                                name = name,
                                birth_date = birthDate,
                                nationality = nationality,
                                education = education.ifBlank { null },
                                awards = awards.ifBlank { null },
                                image_url = uploadedUrl,
                                wikipedia_url = normalizeWebsite(wikipediaUrl),
                                description = description.ifBlank { null }
                            )
                        )
                    }
                    return@Button
                } else {
                    imageUrl
                }

                // Business Logic: Call ViewModel to update artist
                viewModel.updateArtist(
                    RegisterArtistRequest(
                        id = currentArtistData?.id ?: "",
                        name = name,
                        birth_date = birthDate,
                        nationality = nationality,
                        education = education.ifBlank { null },
                        awards = awards.ifBlank { null },
                        image_url = finalImageUrl,
                        wikipedia_url = normalizeWebsite(wikipediaUrl),
                        description = description.ifBlank { null }
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = updateArtistState !is UpdateArtistState.Loading
        ) {
            if (updateArtistState is UpdateArtistState.Loading) {
                AnimatedPreloader(
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text("Update Profile")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cancel Button
        Button(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(),
            enabled = updateArtistState !is UpdateArtistState.Loading
        ) {
            Text("Cancel")
        }
    }
}
