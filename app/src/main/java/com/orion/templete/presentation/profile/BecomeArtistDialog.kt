package com.orion.templete.presentation.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.orion.templete.data.model.artist_model.ArtistDTO
import com.orion.templete.domain.repository.UserRepository
import com.orion.templete.util.ResponseStates
import kotlinx.coroutines.launch

@Composable
fun BecomeArtistDialog(
    userId: String,
    userName: String,
    userRepository: UserRepository,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var name by remember { mutableStateOf(userName) }
    var birthDate by remember { mutableStateOf("") }
    var deathDate by remember { mutableStateOf("") }
    var nationality by remember { mutableStateOf("") }
    var notableWorks by remember { mutableStateOf("") }
    var artMovement by remember { mutableStateOf("") }
    var education by remember { mutableStateOf("") }
    var awards by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var wikipediaUrl by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val coroutineScope = rememberCoroutineScope()
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Become an Artist",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Form fields
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = birthDate,
                    onValueChange = { birthDate = it },
                    label = { Text("Birth Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = deathDate,
                    onValueChange = { deathDate = it },
                    label = { Text("Death Date (leave empty if alive)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = nationality,
                    onValueChange = { nationality = it },
                    label = { Text("Nationality") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = notableWorks,
                    onValueChange = { notableWorks = it },
                    label = { Text("Notable Works") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = artMovement,
                    onValueChange = { artMovement = it },
                    label = { Text("Art Movement") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = education,
                    onValueChange = { education = it },
                    label = { Text("Education") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = awards,
                    onValueChange = { awards = it },
                    label = { Text("Awards") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("Image URL") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = wikipediaUrl,
                    onValueChange = { wikipediaUrl = it },
                    label = { Text("Wikipedia URL") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Error message
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            if (name.isBlank() || birthDate.isBlank() || nationality.isBlank()) {
                                errorMessage = "Name, birth date, and nationality are required"
                                return@Button
                            }
                            
                            isLoading = true
                            errorMessage = null
                            
                            val artist = ArtistDTO(
                                id = userId,
                                name = name,
                                birth_date = birthDate,
                                death_date = deathDate,
                                nationality = nationality,
                                notable_works = notableWorks,
                                art_movement = artMovement,
                                education = education,
                                awards = awards,
                                image_url = imageUrl,
                                wikipedia_url = wikipediaUrl,
                                description = description,
                                follow = false
                            )
                            
                            coroutineScope.launch {
                                userRepository.registerAsArtist(artist).collect { response ->
                                    when (response) {
                                        is ResponseStates.Loading -> {
                                            isLoading = true
                                        }
                                        is ResponseStates.Success -> {
                                            isLoading = false
                                            onSuccess()
                                        }
                                        is ResponseStates.Error -> {
                                            isLoading = false
                                            errorMessage = response.error
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Submit")
                        }
                    }
                }
            }
        }
    }
}