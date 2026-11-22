package com.orion.templete.presentation.reels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.orion.templete.R
import com.orion.templete.data.model.artwork_model.ArtworkDTO
import com.orion.templete.presentation.common.ErrorScreen
import com.orion.templete.presentation.common.LoadingScreen

/**
 * Full-screen vertical artwork feed with automatic story narration.
 * Swipe up/down to move between artworks; the story of the artwork on screen
 * is read aloud with the device's text-to-speech voice.
 */
@Composable
fun ReelsScreen(viewModel: ReelsViewModel) {
    val state = viewModel.state

    DisposableEffect(Unit) {
        onDispose { viewModel.stopAudio() }
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
        else -> ReelsPager(state, viewModel)
    }
}

@Composable
private fun ReelsPager(state: ReelsUiState, viewModel: ReelsViewModel) {
    val pagerState = rememberPagerState(pageCount = { state.items.size })

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            viewModel.onPageSettled(page)
        }
    }

    VerticalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) { page ->
        val artwork = state.items[page]
        ReelPage(
            artwork = artwork,
            state = state,
            isActivePage = page == pagerState.settledPage,
            onToggleAudio = viewModel::toggleAudio,
            onLanguageSelected = viewModel::onLanguageSelected,
            onVoiceSelected = viewModel::onVoiceSelected
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
}

@Composable
private fun ReelPage(
    artwork: ArtworkDTO,
    state: ReelsUiState,
    isActivePage: Boolean,
    onToggleAudio: () -> Unit,
    onLanguageSelected: (ReelsLanguage) -> Unit,
    onVoiceSelected: (String?) -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        AsyncImage(
            model = artwork.imageUrl ?: artwork.image_url_compressed,
            contentDescription = artwork.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Readability scrims.
        Box(
            Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                    )
                )
                .padding(bottom = 32.dp)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
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
                if (!artwork.title.isNullOrBlank()) {
                    Text(
                        text = artwork.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                    )
                )
                .padding(24.dp)
        ) {
            Text(
                text = artwork.artist ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }

        // Play / pause for the story narration.
        if (isActivePage && !artwork.description.isNullOrBlank()) {
            val speaking = state.speakingArtworkId == artwork.id
            IconButton(
                onClick = onToggleAudio,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .size(52.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    painter = painterResource(
                        if (speaking) R.drawable.ic_pause else R.drawable.ic_play
                    ),
                    contentDescription = if (speaking) "Pause story" else "Play story",
                    tint = Color.White
                )
            }
        }
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
            Text(selected.label, color = Color.White)
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
    val currentLabel = state.availableVoices
        .firstOrNull { it.name == state.selectedVoiceName }
        ?.let { voiceLabel(it.name) }
        ?: "Default voice"
    Box {
        TextButton(onClick = { open = true }) {
            Text(currentLabel, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("Default voice") },
                onClick = {
                    onSelected(null)
                    open = false
                }
            )
            state.availableVoices.forEach { voice ->
                DropdownMenuItem(
                    text = { Text(voiceLabel(voice.name)) },
                    onClick = {
                        onSelected(voice.name)
                        open = false
                    }
                )
            }
        }
    }
}

/** Turns an engine voice id like "en-in-x-end-network" into a short label. */
private fun voiceLabel(name: String): String =
    name.substringAfterLast('-').replaceFirstChar { it.uppercase() }
