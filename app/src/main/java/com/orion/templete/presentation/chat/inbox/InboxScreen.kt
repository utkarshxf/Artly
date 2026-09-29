package com.orion.templete.presentation.chat.inbox

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

// Instagram-style DM inbox: username header, search (chats + "More people"), "Active now" strip and the
// conversation list with long-press actions.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    onBack: () -> Unit,
    onOpenChat: (peer: String) -> Unit,
    onNewMessage: () -> Unit,
    viewModel: InboxViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val searchListState = rememberLazyListState()

    RequestChatNotificationPermissionOnce()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    // Back leaves search first, like Instagram
    BackHandler(enabled = viewModel.query.isNotEmpty()) {
        viewModel.onQueryChange("")
        focusManager.clearFocus()
    }

    val openChat: (String) -> Unit = { peer ->
        focusManager.clearFocus()
        if (viewModel.query.isNotEmpty()) viewModel.onQueryChange("")
        onOpenChat(peer)
    }

    // Long-press sheet and delete confirmation, keyed by conversation id so they survive rotation
    var actionsFor by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmDeleteFor by rememberSaveable { mutableStateOf<String?>(null) }
    val ready = state.content as? InboxContent.Ready
    val actionsRow = actionsFor?.let { id -> ready?.rows?.firstOrNull { it.conversationId == id } }
    val deleteRow = confirmDeleteFor?.let { id -> ready?.rows?.firstOrNull { it.conversationId == id } }
    LaunchedEffect(ready, actionsFor, confirmDeleteFor) {
        // The conversation disappeared (deleted elsewhere): drop the stale sheet/dialog
        if (ready != null) {
            if (actionsFor != null && actionsRow == null) actionsFor = null
            if (confirmDeleteFor != null && deleteRow == null) confirmDeleteFor = null
        }
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val hideSheet: (then: () -> Unit) -> Unit = { then ->
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            actionsFor = null
            then()
        }
    }

    val colors = MaterialTheme.colorScheme
    Scaffold(
        containerColor = colors.surface,
        contentColor = colors.onSurface,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.me.ifBlank { "Messages" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNewMessage) {
                        Icon(imageVector = Icons.Outlined.Edit, contentDescription = "New message")
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
            InboxSearchField(
                value = viewModel.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = "Search",
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
            )
            InboxBody(
                content = state.content,
                listState = listState,
                searchListState = searchListState,
                onOpenChat = openChat,
                onLongPress = { conversationId ->
                    focusManager.clearFocus()
                    actionsFor = conversationId
                },
                onNewMessage = onNewMessage,
                onRetry = viewModel::retry,
                onRetrySearch = viewModel::retrySearch,
                onScrollSearch = { focusManager.clearFocus() },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        }
    }

    if (actionsRow != null) {
        ConversationActionsSheet(
            row = actionsRow,
            sheetState = sheetState,
            onDismissRequest = { actionsFor = null },
            onToggleMute = {
                viewModel.setMuted(actionsRow.conversationId, !actionsRow.muted)
                hideSheet {}
            },
            onToggleUnread = {
                viewModel.setUnread(actionsRow.conversationId, !actionsRow.unread)
                hideSheet {}
            },
            onDelete = {
                val id = actionsRow.conversationId
                hideSheet { confirmDeleteFor = id }
            },
        )
    }

    if (deleteRow != null) {
        DeleteChatDialog(
            name = deleteRow.title,
            onConfirm = {
                confirmDeleteFor = null
                viewModel.deleteChat(deleteRow.conversationId)
            },
            onDismiss = { confirmDeleteFor = null },
        )
    }
}

private enum class InboxBodyKind { Loading, Error, Empty, List, Search }

private fun InboxContent.kind(): InboxBodyKind = when (this) {
    InboxContent.Loading -> InboxBodyKind.Loading
    is InboxContent.Error -> InboxBodyKind.Error
    is InboxContent.Ready -> when {
        searchQuery.isNotEmpty() -> InboxBodyKind.Search
        rows.isEmpty() -> InboxBodyKind.Empty
        else -> InboxBodyKind.List
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun InboxBody(
    content: InboxContent,
    listState: LazyListState,
    searchListState: LazyListState,
    onOpenChat: (String) -> Unit,
    onLongPress: (conversationId: String) -> Unit,
    onNewMessage: () -> Unit,
    onRetry: () -> Unit,
    onRetrySearch: () -> Unit,
    onScrollSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = content,
        modifier = modifier,
        transitionSpec = { fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(150)) },
        contentKey = { it.kind() },
        label = "inbox_body",
    ) { target ->
        when (target) {
            InboxContent.Loading -> InboxSkeleton()
            is InboxContent.Error -> InboxErrorState(error = target, onRetry = onRetry)
            is InboxContent.Ready -> when (target.kind()) {
                InboxBodyKind.Search -> InboxSearchResults(
                    content = target,
                    listState = searchListState,
                    onOpenChat = onOpenChat,
                    onLongPress = onLongPress,
                    onRetrySearch = onRetrySearch,
                    onScroll = onScrollSearch,
                )
                InboxBodyKind.Empty -> InboxMessageState(
                    icon = Icons.AutoMirrored.Outlined.Send,
                    title = "Your messages",
                    body = "Send private messages to artists and friends.",
                    actionLabel = "Send message",
                    onAction = onNewMessage,
                )
                else -> InboxConversationList(
                    content = target,
                    listState = listState,
                    onOpenChat = onOpenChat,
                    onLongPress = onLongPress,
                )
            }
        }
    }
}

@Composable
private fun InboxErrorState(error: InboxContent.Error, onRetry: () -> Unit) {
    if (error.notConfigured) {
        InboxMessageState(
            icon = Icons.Outlined.CloudOff,
            title = "Messages are almost here",
            body = "We're still setting up messaging on Artistry. Please try again in a little while.",
            actionLabel = "Retry",
            onAction = onRetry,
        )
    } else {
        InboxMessageState(
            icon = Icons.Outlined.ErrorOutline,
            title = "Couldn't load messages",
            body = error.message,
            actionLabel = "Retry",
            onAction = onRetry,
        )
    }
}

@Composable
private fun InboxConversationList(
    content: InboxContent.Ready,
    listState: LazyListState,
    onOpenChat: (String) -> Unit,
    onLongPress: (conversationId: String) -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        // Fixed first item: while the list is at the top it stays the scroll anchor, so a conversation that
        // jumps to the top (new message) or a newly shown "Active now" strip is visible instead of above it
        item(key = "inbox_top", contentType = "spacer") {
            Spacer(Modifier.height(4.dp))
        }
        if (content.activeNow.isNotEmpty()) {
            item(key = "inbox_active_now", contentType = "active_now") {
                InboxActiveNowRow(
                    people = content.activeNow,
                    onClick = onOpenChat,
                    modifier = Modifier.animateItem(),
                )
            }
        }
        item(key = "inbox_messages_header", contentType = "header") {
            InboxSectionHeader(text = "Messages", modifier = Modifier.animateItem())
        }
        items(
            items = content.rows,
            key = { it.conversationId },
            contentType = { "conversation" },
        ) { row ->
            InboxConversationRow(
                row = row,
                onClick = { onOpenChat(row.peer) },
                onLongClick = { onLongPress(row.conversationId) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun InboxSearchResults(
    content: InboxContent.Ready,
    listState: LazyListState,
    onOpenChat: (String) -> Unit,
    onLongPress: (conversationId: String) -> Unit,
    onRetrySearch: () -> Unit,
    onScroll: () -> Unit,
) {
    // Scrolling the results hides the keyboard
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) onScroll()
    }
    val nothingFound = content.searchMatches.isEmpty() && content.people.isEmpty() &&
        !content.peopleLoading && !content.peopleFailed
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
    ) {
        items(
            items = content.searchMatches,
            key = { "chat_${it.conversationId}" },
            contentType = { "conversation" },
        ) { row ->
            InboxConversationRow(
                row = row,
                onClick = { onOpenChat(row.peer) },
                onLongClick = { onLongPress(row.conversationId) },
            )
        }
        if (content.people.isNotEmpty() || content.peopleLoading || content.peopleFailed) {
            item(key = "people_header", contentType = "header") {
                InboxSectionHeader(text = "More people")
            }
        }
        items(
            items = content.people,
            key = { "person_${it.username}" },
            contentType = { "person" },
        ) { person ->
            InboxPersonRow(person = person, onClick = { onOpenChat(person.username) })
        }
        if (content.peopleLoading && content.people.isEmpty()) {
            item(key = "people_loading", contentType = "loading") { InboxLoadingRow() }
        }
        if (content.peopleFailed) {
            item(key = "people_failed", contentType = "notice") {
                InboxNoticeRow(
                    text = "Couldn't search for people.",
                    actionLabel = "Try again",
                    onAction = onRetrySearch,
                )
            }
        }
        if (nothingFound) {
            item(key = "no_results", contentType = "notice") {
                InboxNoticeRow(text = "No results for \"${content.searchQuery}\"")
            }
        }
    }
}
