package com.orion.templete.presentation.reels

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.orion.templete.data.model.artwork_model.comments.CommentRequest
import com.orion.templete.data.model.artwork_model.comments.GetCommentsDTO
import com.orion.templete.presentation.artist_profile.ArtistProfileViewModel
import com.orion.templete.presentation.artist_profile.GetCommentsOnArtworkUiState
import com.orion.templete.presentation.artwork_detail.CommentSection
import com.orion.templete.presentation.artwork_detail.SaveSection
import com.orion.templete.util.SecureStorage
import com.orion.templete.presentation.swipe.components.BottomSheet
import com.orion.templete.util.GuestGate
import com.orion.templete.R
import com.orion.templete.data.model.artwork_model.ArtworkDTO
import com.orion.templete.presentation.common.ErrorScreen
import com.orion.templete.presentation.common.LoadingScreen
import com.orion.templete.util.ShareUtils
import com.orion.templete.util.extractYear

/**
 * Full-screen vertical artwork feed with automatic story narration, styled like
 * the Home feed card: the whole artwork fitted on black, the serif title block
 * with artist / museum / medium / year at the bottom, and a right-side action
 * rail (like, comment, share, save, narration play-pause).
 */
@Composable
fun ReelsScreen(
    viewModel: ReelsViewModel,
    onOpenArtwork: (ArtworkDTO) -> Unit,
    reelsMode: String? = null,
    reelsRefId: String? = null
) {
    LaunchedEffect(Unit) { viewModel.setSource(reelsMode, reelsRefId) }
    val state = viewModel.state

    // Audio stops the moment the feed leaves the screen: tab change/back (dispose)
    // or the app going to the background (ON_STOP).
    DisposableEffect(Unit) {
        onDispose { viewModel.stopAudio() }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.stopAudio()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    state.toastMessage?.let { message ->
        val context = LocalContext.current
        LaunchedEffect(message) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    when {
        state.items.isEmpty() && state.isLoading -> LoadingScreen()
        state.items.isEmpty() && state.error != null -> ErrorScreen(
            message = state.error ?: "Something went wrong",
            onRetry = viewModel::loadNextItems
        )
        state.items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No artworks yet", color = Color.White)
        }
        else -> ReelsPager(state, viewModel, onOpenArtwork)
    }
}

@Composable
private fun ReelsPager(
    state: ReelsUiState,
    viewModel: ReelsViewModel,
    onOpenArtwork: (ArtworkDTO) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { state.items.size })
    // Save opens the same collections bottom sheet as the artwork screen
    var saveSheetArtworkId by remember { mutableStateOf<String?>(null) }
    // Comments open the same bottom sheet as the artwork screen, in place on the player
    var commentsArtworkId by remember { mutableStateOf<String?>(null) }
    val commentsVm: ArtistProfileViewModel = hiltViewModel()

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            viewModel.onPageSettled(page)
        }
    }

    // Warm images + story/audio for the page the fling is heading to, before it lands
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.targetPage }.collect { page ->
            viewModel.onPageTargeted(page)
        }
    }

    VerticalPager(
        state = pagerState,
        beyondViewportPageCount = 1,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) { page ->
        val artwork = state.items[page]
        val context = LocalContext.current
        ReelPage(
            artwork = artwork,
            state = state,
            isActivePage = page == pagerState.settledPage,
            onToggleAudio = viewModel::toggleAudio,
            onLanguageSelected = viewModel::onLanguageSelected,
            onVoiceSelected = viewModel::onVoiceSelected,
            onToggleLike = { if (!GuestGate.check(context)) artwork.id?.let(viewModel::toggleLike) },
            onSave = {
                if (!GuestGate.check(context)) saveSheetArtworkId = artwork.id
            },
            onOpenArtwork = { onOpenArtwork(artwork) },
            onOpenComments = { commentsArtworkId = artwork.id }
        )
    }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(24.dp)
                    .size(28.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        }
    }

    saveSheetArtworkId?.let { targetId ->
        BottomSheet(onDismiss = { saveSheetArtworkId = null }) {
            SaveSection(
                dismiss = { saveSheetArtworkId = null },
                artworkId = targetId
            )
        }
    }

    commentsArtworkId?.let { targetId ->
        LaunchedEffect(targetId) { commentsVm.getAllComments(targetId) }
        BottomSheet(onDismiss = { commentsArtworkId = null }) {
            when (val uiState = commentsVm.getCommentsOnArtworkUiState) {
                is GetCommentsOnArtworkUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentSize(Alignment.Center)
                    )
                }

                is GetCommentsOnArtworkUiState.Success -> {
                    var comments by remember { mutableStateOf(uiState.data) }
                    val context = LocalContext.current
                    val currentUser = SecureStorage(context).getUserDetails()
                    CommentSection(
                        comments = comments,
                        onSendComment = { newComment ->
                            comments = listOf(
                                GetCommentsDTO(
                                    userId = currentUser?.id,
                                    userName = currentUser?.name,
                                    userProfilePicture = currentUser?.profilePicture,
                                    text = newComment
                                )
                            ) + comments
                            commentsVm.commentOnArtwork(targetId, CommentRequest(text = newComment))
                        }
                    )
                }

                is GetCommentsOnArtworkUiState.Error -> {
                    ErrorScreen(
                        message = uiState.message,
                        onRetry = { commentsVm.refreshComments(artworkId = targetId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReelPage(
    artwork: ArtworkDTO,
    state: ReelsUiState,
    isActivePage: Boolean,
    onToggleAudio: () -> Unit,
    onLanguageSelected: (ReelsLanguage) -> Unit,
    onVoiceSelected: (String?) -> Unit,
    onToggleLike: () -> Unit,
    onSave: () -> Unit,
    onOpenArtwork: () -> Unit,
    onOpenComments: () -> Unit
) {
    val context = LocalContext.current
    Box(Modifier.fillMaxSize()) {
        // Whole artwork visible, never cropped; letterboxed on black.
        AsyncImage(
            model = artwork.image_url_compressed ?: artwork.imageUrl,
            contentDescription = artwork.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        // Compact top bar: language and voice pickers, small and out of the way.
        Row(
            Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent)
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LanguageSelector(
                selected = state.language,
                onSelected = onLanguageSelected
            )
            VoiceSelector(
                state = state,
                onSelected = onVoiceSelected
            )
        }

        // Bottom info block, styled like the Home feed card.
        Column(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .clickable { onOpenArtwork() }
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                    )
                )
                .padding(start = 16.dp, end = 76.dp, top = 48.dp, bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                artwork.title?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            artwork.artist?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }

            artwork.currentLocation?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Light,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                artwork.medium?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                extractYear(artwork.releasedDate)?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Right-side action rail, Instagram-reels style.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 10.dp, bottom = 24.dp)
        ) {
            val liked = artwork.liked == true
            RailButton(
                iconRes = if (liked) R.drawable.ic_favorite_filled else R.drawable.ic_favorite,
                contentDescription = if (liked) "Unlike" else "Like",
                tint = if (liked) Color(0xFFE0245E) else Color.White,
                onClick = onToggleLike
            )
            RailButton(
                iconRes = R.drawable.ic_comment,
                contentDescription = "Comments",
                onClick = onOpenComments
            )
            RailButton(
                iconRes = R.drawable.ic_share,
                contentDescription = "Share",
                onClick = { ShareUtils.shareArtwork(context, artwork) }
            )
            RailButton(
                iconRes = R.drawable.ic_save,
                contentDescription = "Save to Favorites",
                onClick = onSave
            )
            // Play / pause for the story narration.
            if (isActivePage && !artwork.description.isNullOrBlank()) {
                val speaking = state.speakingArtworkId == artwork.id
                RailButton(
                    iconRes = if (speaking) R.drawable.ic_pause else R.drawable.ic_play,
                    contentDescription = if (speaking) "Pause story" else "Play story",
                    onClick = onToggleAudio
                )
            }
        }
    }
}

@Composable
private fun RailButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = Color.White
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun LanguageSelector(
    selected: ReelsLanguage,
    onSelected: (ReelsLanguage) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) {
            Text(
                selected.label,
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            ReelsLanguage.entries.forEach { language ->
                DropdownMenuItem(
                    text = { Text(language.label) },
                    enabled = language.enabled,
                    onClick = {
                        onSelected(language)
                        open = false
                    }
                )
            }
        }
    }
}

@Composable
private fun VoiceSelector(
    state: ReelsUiState,
    onSelected: (String?) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    // Engine voice ids are cryptic ("en-in-x-end-local"); give each one a stable human
    // name, matched to its gender where the engine reports it.
    val femaleNames = listOf("Asha", "Meera", "Kavya", "Anaya", "Priya", "Sara", "Nisha", "Riya", "Ira", "Tara",
        "Anika", "Diya", "Myra", "Kiara", "Naina", "Sanya", "Vani", "Zoya", "Aarti", "Pooja")
    val maleNames = listOf("Arjun", "Rohan", "Vikram", "Aditya", "Karan", "Dev", "Nikhil", "Rahul", "Aman", "Varun",
        "Ishaan", "Kabir", "Yash", "Om", "Rey", "Samar", "Tejas", "Vivaan", "Zaid", "Mohan")
    var femaleCount = 0
    var maleCount = 0
    val labels = state.availableVoices.map { voice ->
        val gender = voice.features?.firstOrNull {
            it.equals("female", ignoreCase = true) || it.equals("male", ignoreCase = true)
        }?.lowercase()
            // Google voices carry the gender in the id ("...#female_2-local"), not in features
            ?: Regex("female|male").find(voice.name.lowercase())?.value
        val label = when (gender) {
            "male" -> maleNames.getOrElse(maleCount++) { "Voice ${maleCount}" }
            "female" -> femaleNames.getOrElse(femaleCount++) { "Voice ${femaleCount}" }
            else -> if ((femaleCount + maleCount) % 2 == 0) femaleNames.getOrElse(femaleCount++) { "Voice ${femaleCount}" }
                    else maleNames.getOrElse(maleCount++) { "Voice ${maleCount}" }
        }
        voice.name to label
    }
    val currentLabel = labels.firstOrNull { it.first == state.selectedVoiceName }?.second
        ?: "Default voice"
    Box {
        TextButton(onClick = { open = true }) {
            Text(
                currentLabel,
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier.heightIn(max = 360.dp)
        ) {
            DropdownMenuItem(
                text = { Text("Default voice") },
                onClick = {
                    onSelected(null)
                    open = false
                }
            )
            labels.forEach { (name, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSelected(name)
                        open = false
                    }
                )
            }
        }
    }
}
