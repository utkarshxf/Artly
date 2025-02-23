import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import java.time.LocalDate
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import com.orion.templete.R
import com.orion.templete.data.model.user_model.UserDetails
import com.orion.templete.presentation.ui.theme.TempleteTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserEditScreen(
    userDetails: UserDetails,
    onSave: (UserDetails) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(userDetails.name) }
    var dob by remember { mutableStateOf(userDetails.dob) }
    var gender by remember { mutableStateOf(userDetails.gender) }
    val language by remember { mutableStateOf(userDetails.language) }
    var countryIso2 by remember { mutableStateOf(userDetails.countryIso2) }
    var artist by remember { mutableStateOf(userDetails.artist) }
    var profilePicture by remember { mutableStateOf(userDetails.profilePicture) }

    var showDatePicker by remember { mutableStateOf(false) }

    val languages = listOf("English", "Spanish", "French", "German", "Chinese", "Japanese")
    val genders = listOf("Male", "Female", "Other", "Prefer not to say")
    val countries = listOf(
        "US" to "United States",
        "GB" to "United Kingdom",
        "CA" to "Canada",
        "AU" to "Australia"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                actions = {
                    TextButton(
                        onClick = {
                            onSave(
                                userDetails.copy(
                                    name = name,
                                    dob = dob,
                                    gender = gender,
                                    language = language,
                                    countryIso2 = countryIso2,
                                    artist = artist,
                                    profilePicture = profilePicture
                                )
                            )
                        }
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Picture Section
            val pickedImageUri = R.drawable.peter
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(modifier = Modifier.padding(start = 8.dp) , contentAlignment = Alignment.BottomEnd)
                {
                    Box(contentAlignment = Alignment.Center) {
                        if (pickedImageUri != null) {
                            AsyncImage(
                                model = pickedImageUri,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .border(
                                        1.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    ),
                                contentScale = ContentScale.Crop
                            )
                        } else {
//                            ImageFromUrl(imageUrl = userData?.photo ?: "")
                        }
                    }
                    Image(
                        painter = painterResource(id = R.drawable.ic_camara),
                        contentDescription = null,
                        modifier = Modifier
                            .size(60.dp)
                            .align(Alignment.BottomEnd)
                            .clickable {
//                                galleryLauncher.launchGallery()
                            }
                    )
                }
            }

            // Personal Information Section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Personal Information",
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dob,
                        onValueChange = { dob = it },
                        label = { Text("Date of Birth") },
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true },
                    )
                    ExposedDropdownMenuBox(
                        expanded = false,
                        onExpandedChange = { }
                    ) {
                        OutlinedTextField(
                            value = gender,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Gender") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = false,
                            onDismissRequest = { }
                        ) {
                            genders.forEach { gender ->
                                DropdownMenuItem(
                                    text = { Text(gender) },
                                    onClick = { /* Handle selection */ }
                                )
                            }
                        }
                    }
                }
            }

            // Preferences Section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Preferences",
                        style = MaterialTheme.typography.titleMedium
                    )

                    ExposedDropdownMenuBox(
                        expanded = false,
                        onExpandedChange = { }
                    ) {
                        OutlinedTextField(
                            value = language,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Language") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = false,
                            onDismissRequest = { }
                        ) {
                            languages.forEach { language ->
                                DropdownMenuItem(
                                    text = { Text(language) },
                                    onClick = { /* Handle selection */ }
                                )
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = false,
                        onExpandedChange = { }
                    ) {
                        OutlinedTextField(
                            value = countries.find { it.first == countryIso2 }?.second ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Country") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = false,
                            onDismissRequest = { }
                        ) {
                            countries.forEach { (code, name) ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = { countryIso2 = code }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Artist Account",
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = artist,
                            onCheckedChange = { artist = it }
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = rememberDatePickerState()
            )
        }
    }
}

@Preview
@Composable
private fun This() {
    TempleteTheme {
        UserEditScreen(
            userDetails = UserDetails(
                name = "John Doe",
                dob = "1990-01-01",
                gender = "Male",
                language = "English",
                countryIso2 = "US",
                artist = true,
                profilePicture = null,
                id = "1",
            ),
            onSave = { },
        )
    }
}