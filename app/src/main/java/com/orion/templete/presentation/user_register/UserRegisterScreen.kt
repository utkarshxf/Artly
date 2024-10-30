package com.orion.templete.presentation.user_register

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.nameisjayant.composeprojects.components.SpacerHeight
import com.orion.templete.R
import com.orion.templete.data.model.user_model.UserDetails
import com.orion.templete.presentation.common.CustomTextField
import com.orion.templete.presentation.ui.theme.TempleteTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserRegisterScreen(
    viewModel: UserRegisterScreenViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
    onNavigateToHome: () -> Unit
) {
    val context = LocalContext.current
    val uiState = viewModel.createUserState
    LaunchedEffect(uiState) {
        uiState.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState.data) {
        uiState.data?.let {
            onNavigateToHome()
        }
    }

    var name by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("") }
    var countryIso2 by remember { mutableStateOf("") }
    var isArtist by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(
                color = if (isSystemInDarkTheme()) {
                    MaterialTheme.colorScheme.background
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header Section with Gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Complete Your Profile",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                SpacerHeight(8.dp)
                Text(
                    text = "Let's get to know you better",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            }
        }

        // Profile Picture Section - Overlapping the gradient

        Box(modifier = Modifier
            .size(100.dp)
            .offset(y = (-50).dp)) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 4.dp, color = MaterialTheme.colorScheme.surface, shape = CircleShape
                    )
                    .shadow(
                        elevation = 8.dp, shape = CircleShape
                    )
                    .clickable { /* Handle image picker */ }, contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center),
                    tint = MaterialTheme.colorScheme.primary
                )

            }
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit Profile Picture",
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.BottomEnd)
                    .background(
                        color = MaterialTheme.colorScheme.primary, shape = CircleShape
                    )
                    .padding(4.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }

        // Form Content
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .offset(y = (-30).dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Input Fields with Card Background
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CustomTextField(
                        value = name,
                        onValueChange = { name = it },
                        hint = R.string.name_hint,
                        keyboardType = KeyboardType.Text,
                    )

                    CustomTextField(
                        value = dob,
                        onValueChange = { dob = it },
                        hint = R.string.dob_hint,
                        keyboardType = KeyboardType.Text,
                    )

                    CustomDropdownField(
                        value = gender,
                        onValueChange = { gender = it },
                        hint = R.string.gender_hint,
                        options = listOf("Male", "Female", "Other")
                    )

                    CustomDropdownField(
                        value = language,
                        onValueChange = { language = it },
                        hint = R.string.language_hint,
                        options = listOf("English", "Spanish", "French")
                    )

                    CustomDropdownField(
                        value = countryIso2,
                        onValueChange = { countryIso2 = it },
                        hint = R.string.country_hint,
                        options = listOf("US", "UK", "IN")
                    )

                    // Artist Switch with enhanced styling
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Artist Profile",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Enable if you're a content creator",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Switch(checked = isArtist, onCheckedChange = { isArtist = it })
                        }
                    }
                }
            }

            // Submit Button with gradient background
            Button(
                onClick = {
                    viewModel.createUser(
                        UserDetails(
                            id = "userId",
                            name = name,
                            dob = dob,
                            gender = gender,
                            language = language,
                            countryIso2 = countryIso2,
                            artist = isArtist,
                            profilePicture = ""
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = stringResource(id = R.string.save_button_label),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDropdownField(
    value: String, onValueChange: (String) -> Unit, hint: Int, options: List<String>
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        CustomTextField(
            value = value,
            onValueChange = onValueChange,
            hint = hint,
            keyboardType = KeyboardType.Text,
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = {
                    onValueChange(option)
                    expanded = false
                })
            }
        }
    }
}

@Preview
@Composable
private fun LoginScreenPrev() {
    TempleteTheme {
        UserRegisterScreen(){}
    }
}