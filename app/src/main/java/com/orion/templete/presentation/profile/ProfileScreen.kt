package com.orion.templete.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.orion.templete.R
import com.orion.templete.data.model.artist_model.ArtistDTO
import com.orion.templete.data.model.user_model.UserDTO
import com.orion.templete.di.AppModule.userRepository
import com.orion.templete.domain.repository.UserRepository
import com.orion.templete.presentation.common.ErrorScreen
import com.orion.templete.presentation.common.Screens
import com.orion.templete.presentation.components.AppIcon
import com.orion.templete.presentation.profile.common.DrawerContent
import com.orion.templete.presentation.ui.theme.TempleteTheme
import com.orion.templete.util.ResponseStates
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    navController: NavController ,
    logOut: () -> Unit = {},
    viewModel: ProfileScreenViewModel = hiltViewModel(),
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    ModalDrawerSheet(
                        drawerContainerColor = MaterialTheme.colorScheme.background,
                        windowInsets = WindowInsets(0)
                    ) {
                        DrawerContent(
                            navController = navController,
                            logOut = logOut,
                            onClose = {
                                scope.launch { drawerState.close() }
                            }
                        )
                    }
                }
            }
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                when (val uiState = viewModel.userData) {
                    is ProfileScreenUiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .fillMaxSize()
                                .wrapContentSize(Alignment.Center)
                        )
                    }

                    is ProfileScreenUiState.Success -> {
                        ProfileContent(
                            user = uiState.user,
                            onMenuClick = { scope.launch { drawerState.open() } },
                            onClick = { navController.navigate(Screens.UserEditScreen.route) },
                            navController = navController,
                            viewModel = viewModel
                        )
                    }

                    is ProfileScreenUiState.Error -> {
                        ErrorScreen(
                            message = uiState.message,
                            onRetry = { viewModel.refreshProfile() })
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileContent(
    user: UserDTO, 
    onMenuClick: () -> Unit, 
    onClick: () -> Unit, 
    navController: NavController,
    viewModel: ProfileScreenViewModel
) {
    var showBecomeArtistDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        UserCreditCard(
            user = user,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        ButtonSection(
            onClick = onClick, 
            onMenuClick = onMenuClick,
            user = user,
            onBecomeArtistClick = { showBecomeArtistDialog = true }
        )
        Spacer(modifier = Modifier.height(16.dp))
        ExploreMoreArtistsCard(navController = navController)
    }

    if (showBecomeArtistDialog) {
        Dialog(onDismissRequest = { showBecomeArtistDialog = false }) {
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

                    var name by remember { mutableStateOf(user.name) }
                    var birthDate by remember { mutableStateOf(user.dob) }
                    var deathDate by remember { mutableStateOf("") }
                    var nationality by remember { mutableStateOf(user.countryIso2) }
                    var notableWorks by remember { mutableStateOf("") }
                    var artMovement by remember { mutableStateOf("") }
                    var education by remember { mutableStateOf("") }
                    var awards by remember { mutableStateOf("") }
                    var imageUrl by remember { mutableStateOf(user.profilePicture ?: "") }
                    var wikipediaUrl by remember { mutableStateOf("") }
                    var description by remember { mutableStateOf("") }

                    var isLoading by remember { mutableStateOf(false) }
                    var errorMessage by remember { mutableStateOf<String?>(null) }

                    val coroutineScope = rememberCoroutineScope()

                    // Observe artistRegistrationState changes
                    LaunchedEffect(viewModel.artistRegistrationState) {
                        when (val state = viewModel.artistRegistrationState) {
                            is ArtistRegistrationState.Loading -> {
                                isLoading = true
                            }
                            is ArtistRegistrationState.Success -> {
                                isLoading = false
                                showBecomeArtistDialog = false
                                // Refresh the profile to show the user as an artist
                                navController.navigate(Screens.Profile.route) {
                                    popUpTo(Screens.Profile.route) { inclusive = true }
                                }
                            }
                            is ArtistRegistrationState.Error -> {
                                isLoading = false
                                errorMessage = state.message
                            }
                            is ArtistRegistrationState.Initial -> {
                                // Initial state, do nothing
                            }
                        }
                    }

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
                        label = { Text("Birth Date") },
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
                        TextButton(onClick = { showBecomeArtistDialog = false }) {
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
                                    id = user.id,
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

                                // Call registerAsArtist without collecting the response
                                viewModel.registerAsArtist(artist)

                                // The UI will be updated based on artistRegistrationState changes
                                // which is observed in the LaunchedEffect below
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
}
@Composable
fun ExploreMoreArtistsCard(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF6A11CB),  // Deep Purple
                        Color(0xFF2575FC)   // Vibrant Blue
                    )
                )
            )
            .clickable {
                navController.navigate(Screens.Search.route)
            }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Column(
                modifier = Modifier.weight(0.7f)
            ) {
                Text(
                    text = "Explore More Artists",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Discover new artist and fresh artworks",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Explore More",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}
@Composable
private fun ButtonSection(
    onClick: () -> Unit, 
    onMenuClick: () -> Unit = {}, 
    user: UserDTO? = null, 
    onBecomeArtistClick: () -> Unit = {},
    viewModel: ProfileScreenViewModel = hiltViewModel()
) {
    TempleteTheme {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clickable { onClick() }
                        .background(
                            color = Color.Transparent, shape = RoundedCornerShape(10.dp)
                        )
                        .border(
                            1.dp,
                            color = MaterialTheme.colorScheme.onSurface,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clip(shape = RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Edit",
                        modifier = Modifier.padding(12.dp),
                        color = LocalContentColor.current,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu"
                    )
                }
            }

            // Only show "Become an Artist" button if user is not already an artist
            if (user != null && !user.artist) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onBecomeArtistClick() }
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clip(shape = RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Become an Artist",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
@Composable
fun UserCreditCard(user: UserDTO, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        // Glossy card with gradient background
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row {
                        AsyncImage(
                            model = user.profilePicture,
                            contentDescription = "Profile picture",
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = user.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "ID: ${user.id.take(8)}...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }
                    }

                    AppIcon(icon = R.drawable.ic_logo_no_bacground, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(40.dp))
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.15f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    UserInfoRow(
                        title = "DOB",
                        value = user.dob
                    )
                    UserInfoRow(
                        title = "Gender",
                        value = user.gender
                    )
                    UserInfoRow(
                        title = "Country",
                        value = user.countryIso2
                    )
                    Row {
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "@${user.name.lowercase().replace(" ", "")}",
                            style = MaterialTheme.typography.labelMedium,
                            color =  MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                        )
                    }
                }
            }

        }
    }
}

@Composable
private fun UserInfoRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color =  MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color =  MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
        )
    }
}
