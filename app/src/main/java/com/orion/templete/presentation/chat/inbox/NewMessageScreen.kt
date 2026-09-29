package com.orion.templete.presentation.chat.inbox

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay

// Instagram "New message": "To:" search field, recent chat peers as "Suggested", people search results.
// Tapping a person opens (or starts) the conversation with them.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewMessageScreen(
    onBack: () -> Unit,
    onOpenChat: (peer: String) -> Unit,
    viewModel: NewMessageViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    // Opens with the keyboard up, like Instagram - but only the first time, not after a rotation or when coming
    // back to this screen
    var autoFocusDone by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (autoFocusDone) return@LaunchedEffect
        autoFocusDone = true
        delay(250) // let the enter transition settle so the keyboard doesn't fight it
        try {
            focusRequester.requestFocus()
        } catch (e: IllegalStateException) {
            // Field not attached (screen already leaving); nothing to focus
        }
    }

    val openChat: (String) -> Unit = { peer ->
        focusManager.clearFocus()
        onOpenChat(peer)
    }

    val colors = MaterialTheme.colorScheme
    Scaffold(
        containerColor = colors.surface,
        contentColor = colors.onSurface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "New message",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        focusManager.clearFocus()
                        onBack()
                    }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.surface,
                    scrolledContainerColor = colors.surface,
                    navigationIconContentColor = colors.onSurface,
                    titleContentColor = colors.onSurface,
                    actionIconContentColor = colors.onSurface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            InboxRecipientField(
                value = viewModel.query,
                onValueChange = viewModel::onQueryChange,
                focusRequester = focusRequester,
            )
            HorizontalDivider(color = colors.onSurface.copy(alpha = 0.08f))
            NewMessageBody(
                state = state,
                listState = listState,
                onPick = openChat,
                onRetrySearch = viewModel::retrySearch,
                onScroll = { focusManager.clearFocus() },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        }
    }
}

private enum class NewMessageBodyKind { SuggestionsLoading, NoSuggestions, List }

private fun NewMessageUiState.kind(): NewMessageBodyKind = when {
    searchQuery.isNotEmpty() -> NewMessageBodyKind.List
    suggestionsLoading -> NewMessageBodyKind.SuggestionsLoading
    suggestions.isEmpty() -> NewMessageBodyKind.NoSuggestions
    else -> NewMessageBodyKind.List
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun NewMessageBody(
    state: NewMessageUiState,
    listState: LazyListState,
    onPick: (username: String) -> Unit,
    onRetrySearch: () -> Unit,
    onScroll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = state,
        modifier = modifier,
        transitionSpec = { fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(150)) },
        contentKey = { it.kind() },
        label = "new_message_body",
    ) { target ->
        when (target.kind()) {
            NewMessageBodyKind.SuggestionsLoading -> Column(modifier = Modifier.fillMaxSize()) {
                InboxSectionHeader(text = "Suggested")
                InboxSkeleton(rows = 8, rowHeight = 64.dp, avatarSize = 48.dp)
            }
            NewMessageBodyKind.NoSuggestions -> InboxMessageState(
                icon = Icons.Outlined.Search,
                title = "Find people",
                body = "Search for artists and friends by name or username to start a conversation.",
                modifier = Modifier.imePadding(),
            )
            NewMessageBodyKind.List -> NewMessageList(
                state = target,
                listState = listState,
                onPick = onPick,
                onRetrySearch = onRetrySearch,
                onScroll = onScroll,
            )
        }
    }
}

@Composable
private fun NewMessageList(
    state: NewMessageUiState,
    listState: LazyListState,
    onPick: (username: String) -> Unit,
    onRetrySearch: () -> Unit,
    onScroll: () -> Unit,
) {
    // Scrolling the list hides the keyboard
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) onScroll()
    }
    val searching = state.searchQuery.isNotEmpty()
    val people = if (searching) state.results else state.suggestions
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
    ) {
        if (!searching) {
            item(key = "suggested_header", contentType = "header") {
                InboxSectionHeader(text = "Suggested")
            }
        }
        items(
            items = people,
            key = { "person_${it.username}" },
            contentType = { "person" },
        ) { person ->
            InboxPersonRow(person = person, onClick = { onPick(person.username) })
        }
        if (searching) {
            when {
                // Local matches show at once; the spinner under them means more people are on the way
                state.searching -> item(key = "search_loading", contentType = "loading") { InboxLoadingRow() }
                state.searchFailed -> item(key = "search_failed", contentType = "notice") {
                    InboxNoticeRow(
                        text = "Couldn't search for people.",
                        actionLabel = "Try again",
                        onAction = onRetrySearch,
                    )
                }
                people.isEmpty() -> item(key = "no_results", contentType = "notice") {
                    InboxNoticeRow(text = "No account found.")
                }
            }
        }
    }
}
