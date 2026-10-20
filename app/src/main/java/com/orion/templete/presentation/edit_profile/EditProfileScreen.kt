import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.orion.templete.MainActivity
import com.orion.templete.R
import com.orion.templete.data.model.user_model.UserDetails
import com.orion.templete.presentation.common.CustomTextField
import com.orion.templete.presentation.components.AnimatedPreloader
import com.orion.templete.presentation.edit_profile.EditProfileViewModel
import com.orion.templete.presentation.ui.theme.LighterGray
import com.orion.templete.util.SecureStorage
import com.orion.templete.util.uploadImage
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserEditScreen(
    userDetails: UserDetails,
    navController: NavController,
    viewModel: EditProfileViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(userDetails.name) }
    var dob by remember { mutableStateOf(userDetails.dob) }
    val language by remember { mutableStateOf(userDetails.language) }
    var countryIso2 by remember { mutableStateOf(userDetails.countryIso2) }
    var artist by remember { mutableStateOf(userDetails.artist) }
    var profilePicture by remember { mutableStateOf(userDetails.profilePicture) }
    val uiState = viewModel.updateUserState
    var showDatePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context as MainActivity
    val imageCropper = remember { activity.getImageCropper() }
    val galleryLauncher = remember { activity.getGalleryLauncher() }

    var selectedGender by remember {
        mutableStateOf(userDetails.gender.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(
                Locale.ROOT
            ) else it.toString()
        })
    }
    var pickedImageUri by remember { mutableStateOf<Uri?>(null) }
    val croppedImageUri by imageCropper.croppedImageUri.collectAsState(null)
    val datePickerState = rememberDatePickerState()

    // Effects
    LaunchedEffect(key1 = croppedImageUri) {
        pickedImageUri = croppedImageUri
    }
    LaunchedEffect(uiState) {
        uiState.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }

        uiState.data?.let {
            navController.popBackStack()
        }
    }

    LaunchedEffect(key1 = Unit) {
        imageCropper.clearCroppedImageUri()
    }

    LaunchedEffect(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let {
            val localDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
            dob = "${localDate.year}-${
                localDate.monthValue.toString().padStart(2, '0')
            }-${localDate.dayOfMonth.toString().padStart(2, '0')}"
        }
    }


    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = {
            TextButton(onClick = { showDatePicker = false }) {
                Text("OK")
            }
        }, dismissButton = {
            TextButton(onClick = { showDatePicker = false }) {
                Text("Cancel")
            }
        }) {
            DatePicker(state = datePickerState)
        }
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Edit Profile") }, actions = {
            TextButton(onClick = {
                viewModel.updateUser(
                    userDetails.copy(
                        name = name,
                        dob = dob,
                        gender = selectedGender,
                        language = language,
                        countryIso2 = countryIso2,
                        artist = artist,
                        profilePicture = profilePicture
                    )
                )
            }) {
                Text("Save")
            }
        })
    }) { paddingValues ->
        Column(
            modifier = modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Box(modifier = Modifier.padding(start = 8.dp) , contentAlignment = Alignment.BottomEnd) {
                AsyncImage(
                    model = pickedImageUri ?: profilePicture,
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
                        .clickable {
                                galleryLauncher.launchGallery()
                        }
                )
            }


            Spacer(modifier = Modifier.height(24.dp))

            // Form Fields
            CustomTextField(
                value = name,
                onValueChange = { name = it },
                hint = R.string.name_hint,
                keyboardType = KeyboardType.Text,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Date of Birth Field
            TextField(value = dob,
                onValueChange = { },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Select Date"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = if (isSystemInDarkTheme()){
                        MaterialTheme.colorScheme.surface
                    }else{
                        LighterGray
                    },
                    unfocusedContainerColor = if (isSystemInDarkTheme()){
                        MaterialTheme.colorScheme.surface
                    }else{
                        LighterGray
                    },
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent
                ),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Gender Selection
            Text(
                text = "Gender",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val genderOptions = listOf("Male", "Female", "Others")

                genderOptions.forEach { gender ->
                    FilterChip(selected = gender == selectedGender,
                        onClick = { selectedGender = gender },
                        label = { Text(gender) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Submit Button
            Button(
                onClick = {
                    SecureStorage(context).getUserId()?.let { userId ->
                        if (pickedImageUri != null) {
                            uploadImage(pickedImageUri, context) { imageUrl ->
                                viewModel.updateUser(
                                    UserDetails(
                                        id = userId,
                                        name = name,
                                        dob = dob,
                                        gender = selectedGender,
                                        language = language,
                                        countryIso2 = countryIso2,
                                        artist = false,
                                        profilePicture = imageUrl
                                    )
                                )
                            }
                        } else {
                            viewModel.updateUser(
                                UserDetails(
                                    id = userId,
                                    name = name,
                                    dob = dob,
                                    gender = selectedGender,
                                    language = language,
                                    countryIso2 = countryIso2,
                                    artist = false,
                                    profilePicture = profilePicture
                                )
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = MaterialTheme.shapes.medium,
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    AnimatedPreloader()
                } else Text(
                    text = stringResource(R.string.update),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
