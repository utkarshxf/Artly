package com.orion.templete.presentation.chat.share

import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.orion.templete.data.model.chat.ArtworkRef
import com.orion.templete.data.model.chat.ChatUser
import com.orion.templete.presentation.chat.components.ChatAvatar
import com.orion.templete.presentation.chat.components.ChatBlue
import com.orion.templete.presentation.chat.components.ChatPeerBubbleDark
import com.orion.templete.presentation.chat.components.ChatPeerBubbleLight
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Instagram's share sheet for an artwork: search, a grid of people (selected ones and recent chats first), tap to
 * select, then one "Send" for everyone picked with an optional message. With nobody selected the bottom shows
 * "Copy link" and "Share to…" (other apps).
 */
@Composable
fun ShareToChatSheet(
    artwork: ArtworkRef,
    onDismiss: () -> Unit,
    onShareExternally: (() -> Unit)? = null,
) {
    ShareToChatSheet(payload = SharePayload.Artwork(artwork), onDismiss = onDismiss, onShareExternally = onShareExternally)
}

// Same sheet for a profile ("Share profile")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareToChatSheet(
    payload: SharePayload,
    onDismiss: () -> Unit,
    onShareExternally: (() -> Unit)? = null,
    viewModel: ShareToChatViewModel = hiltViewModel(),
) {
    // New id each time the sheet is opened, kept across rotation / process death
    val sessionId = rememberSaveable { UUID.randomUUID().toString() }
    LaunchedEffect(sessionId, payload) { viewModel.open(sessionId, payload) }

    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val colors = remember(dark) {
        ShareSheetColors(
            sheet = if (dark) Color(0xFF121212) else Color.White,
            field = if (dark) ChatPeerBubbleDark else ChatPeerBubbleLight,
        )
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val dismiss: () -> Unit = {
        viewModel.close()
        onDismiss()
    }
    val closeAnimated: () -> Unit = {
        focusManager.clearFocus()
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) dismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = dismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = colors.sheet,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        ShareSheetContent(
            state = state,
            colors = colors,
            onQueryChange = viewModel::onQueryChange,
            onNoteChange = viewModel::onNoteChange,
            onToggle = viewModel::toggle,
            onRetry = viewModel::retry,
            onSend = {
                val count = viewModel.sendToSelected()
                if (count > 0) {
                    Toast.makeText(context.applicationContext, "Sent", Toast.LENGTH_SHORT).show()
                    closeAnimated()
                }
            },
            onCopyLink = {
                clipboard.setText(AnnotatedString(payload.shareLinkText()))
                // Android 13+ shows its own "copied" confirmation
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                }
                closeAnimated()
            },
            onShareExternally = onShareExternally?.let { share ->
                {
                    share()
                    closeAnimated()
                }
            },
        )
    }
}

private data class ShareSheetColors(val sheet: Color, val field: Color)

@Composable
private fun ShareSheetContent(
    state: ShareToChatUiState,
    colors: ShareSheetColors,
    onQueryChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onToggle: (ChatUser) -> Unit,
    onRetry: () -> Unit,
    onSend: () -> Unit,
    onCopyLink: () -> Unit,
    onShareExternally: (() -> Unit)?,
) {
    val focusManager = LocalFocusManager.current
    val query = state.query.trim()
    // Browsing: people already picked stay first (like Instagram), then recent chats
    val suggested = remember(state.recent, state.selected) {
        state.selected + state.recent.filterNot { recent -> state.selected.any { it.username.equals(recent.username, ignoreCase = true) } }
    }
    val matches = remember(state.recent, state.selected, query) {
        if (query.isEmpty()) emptyList()
        else (state.selected + state.recent).distinctBy { it.username.lowercase() }.filter { it.matches(query) }
    }
    val morePeople = remember(state.results, matches) {
        val shown = matches.map { it.username.lowercase() }.toSet()
        state.results.filter { it.username.lowercase() !in shown }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.88f)
            .imePadding()
    ) {
        PillTextField(
            value = state.query,
            onValueChange = onQueryChange,
            placeholder = "Search",
            background = colors.field,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            leading = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = secondaryTextColor(),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            },
            trailing = {
                if (state.query.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear search",
                        tint = secondaryTextColor(),
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .clickable(role = Role.Button) { onQueryChange("") }
                    )
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            if (query.isEmpty()) {
                suggestedSection(state, suggested, sheetColor = colors.sheet, onToggle = onToggle, onRetry = onRetry)
            } else {
                items(matches, key = { "match_${it.username}" }) { user ->
                    ShareTargetCell(user, state.isSelected(user), colors.sheet, onToggle)
                }
                if (morePeople.isNotEmpty()) {
                    fullWidth("more_header") { SectionHeader("More people") }
                    items(morePeople, key = { "result_${it.username}" }) { user ->
                        ShareTargetCell(user, state.isSelected(user), colors.sheet, onToggle)
                    }
                }
                when {
                    state.searching -> fullWidth("searching") { SmallProgress() }
                    matches.isEmpty() && morePeople.isEmpty() -> fullWidth("no_results") {
                        CenteredMessage(
                            title = state.searchError ?: "No people found",
                            body = if (state.searchError == null) "Try a different name or username." else null,
                            onRetry = if (state.searchError != null) onRetry else null
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        AnimatedContent(
            targetState = state.selected.isNotEmpty(),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "shareBottomBar"
        ) { anySelected ->
            if (anySelected) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    PillTextField(
                        value = state.note,
                        onValueChange = onNoteChange,
                        placeholder = "Write a message…",
                        background = colors.field,
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SendButton(
                        text = if (state.selected.size > 1) "Send separately" else "Send",
                        onClick = onSend
                    )
                }
            } else {
                ExternalActions(
                    fieldColor = colors.field,
                    onCopyLink = onCopyLink,
                    onShareExternally = onShareExternally
                )
            }
        }
    }
}

private fun LazyGridScope.fullWidth(key: String, content: @Composable () -> Unit) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }
}

private fun LazyGridScope.suggestedSection(
    state: ShareToChatUiState,
    suggested: List<ChatUser>,
    sheetColor: Color,
    onToggle: (ChatUser) -> Unit,
    onRetry: () -> Unit,
) {
    val error = state.recentError
    when {
        suggested.isEmpty() && state.loadingRecent -> fullWidth("loading") { SmallProgress() }
        suggested.isEmpty() && error != null -> fullWidth("error") {
            CenteredMessage(title = error, body = null, onRetry = onRetry)
        }
        suggested.isEmpty() -> fullWidth("empty") {
            CenteredMessage(
                title = "No chats yet",
                body = "Search for an artist or a friend to send this to.",
                onRetry = null
            )
        }
        else -> items(suggested, key = { "suggested_${it.username}" }) { user ->
            ShareTargetCell(user, state.isSelected(user), sheetColor, onToggle)
        }
    }
}

private fun ChatUser.matches(query: String): Boolean =
    username.contains(query, ignoreCase = true) || name?.contains(query, ignoreCase = true) == true

@Composable
private fun secondaryTextColor(): Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

// One person in the grid: avatar (active dot, or the blue check once selected) with the name underneath
@Composable
private fun ShareTargetCell(user: ChatUser, selected: Boolean, sheetColor: Color, onToggle: (ChatUser) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Checkbox, onClickLabel = if (selected) "Unselect" else "Select") { onToggle(user) }
            .padding(vertical = 10.dp, horizontal = 4.dp)
    ) {
        Box(modifier = Modifier.size(64.dp)) {
            ChatAvatar(
                url = user.avatar,
                name = user.displayName,
                size = 64.dp,
                active = user.isActiveNow() && !selected
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp)
                        .border(2.dp, sheetColor, CircleShape)
                        .clip(CircleShape)
                        .background(ChatBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = user.displayName,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SendButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ChatBlue)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

// Nobody selected: other ways to share, as round buttons like the bottom row of Instagram's sheet
@Composable
private fun ExternalActions(fieldColor: Color, onCopyLink: () -> Unit, onShareExternally: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        ExternalAction(icon = Icons.Default.Link, label = "Copy link", fieldColor = fieldColor, onClick = onCopyLink)
        if (onShareExternally != null) {
            ExternalAction(icon = Icons.Default.Share, label = "Share to…", fieldColor = fieldColor, onClick = onShareExternally)
        }
    }
}

@Composable
private fun ExternalAction(icon: ImageVector, label: String, fieldColor: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(fieldColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun SmallProgress() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
    }
}

@Composable
private fun CenteredMessage(title: String, body: String?, onRetry: (() -> Unit)?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Send,
            contentDescription = null,
            tint = secondaryTextColor(),
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        if (body != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryTextColor(),
                textAlign = TextAlign.Center
            )
        }
        if (onRetry != null) {
            TextButton(onClick = onRetry) {
                Text(text = "Try again", color = ChatBlue, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// Compact Instagram-style rounded field (Material text fields are 56dp tall)
@Composable
private fun PillTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    background: Color,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    // Both are laid out inside the field's Row; they bring their own spacing
    leading: @Composable () -> Unit = {},
    trailing: @Composable () -> Unit = {},
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else 4,
        textStyle = textStyle,
        cursorBrush = SolidColor(ChatBlue),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp)
                    .background(background, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                leading()
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = textStyle,
                            color = secondaryTextColor(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    innerTextField()
                }
                trailing()
            }
        }
    )
}
