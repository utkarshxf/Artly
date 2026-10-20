package com.orion.templete.presentation.chat.thread

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orion.templete.data.model.call.CallKind
import com.orion.templete.data.model.call.CallPeer
import com.orion.templete.domain.call.CallIntents
import com.orion.templete.presentation.chat.components.ChatBlue
import com.orion.templete.presentation.chat.components.ChatTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private const val TAG = "ChatThreadScreen"
private const val MAX_JUMP_PAGES = 10
private const val CALL_TAP_GAP_MS = 1_000L

// Instagram-style conversation. Entry point wired by Home.kt (route Screens.ChatThread, nav arg "peerUsername").
@Composable
fun ChatThreadScreen(
    onBack: () -> Unit,
    onOpenProfile: (username: String) -> Unit,
    onOpenArtwork: (artworkId: String) -> Unit,
    viewModel: ChatThreadViewModel = hiltViewModel(),
) {
    val content by viewModel.content.collectAsStateWithLifecycle()
    val list by viewModel.list.collectAsStateWithLifecycle()
    val header by viewModel.header.collectAsStateWithLifecycle()
    val canCall by viewModel.canCall.collectAsStateWithLifecycle()

    val colors = rememberThreadColors()
    val context = LocalContext.current
    val density = LocalDensity.current
    val clipboard = LocalClipboardManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val snackbar = remember { SnackbarHostState() }
    val focusRequester = remember { FocusRequester() }
    val maxBubbleWidth = (LocalConfiguration.current.screenWidthDp * 0.72f).dp

    val currentHeader by rememberUpdatedState(header)
    val currentOnOpenProfile by rememberUpdatedState(onOpenProfile)
    val currentOnOpenArtwork by rememberUpdatedState(onOpenArtwork)

    var selection by remember { mutableStateOf<ThreadSelection?>(null) }
    var viewer by remember { mutableStateOf<ThreadViewerImage?>(null) }
    var unsendTarget by remember { mutableStateOf<ThreadMessageUi?>(null) }
    var reactionsFor by remember { mutableStateOf<String?>(null) }
    var highlightedId by remember { mutableStateOf<String?>(null) }
    var pendingJump by remember { mutableStateOf<String?>(null) }
    var jumpPages by remember { mutableIntStateOf(0) }
    val root = remember { RootCoordinates() }

    // ---- Lifecycle: visible conversation, read receipts, typing, notification ----
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.onResume()
                    cancelChatNotification(context, viewModel.conversationId)
                }
                Lifecycle.Event.ON_PAUSE -> viewModel.onPause()
                Lifecycle.Event.ON_STOP -> viewModel.onStop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onLeave()
        }
    }

    // ---- Photo picker (images only) ----
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.sendImage(uri)
    }
    val openPicker: () -> Unit = remember(pickPhoto) {
        {
            try {
                pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            } catch (e: ActivityNotFoundException) {
                Log.w(TAG, "No photo picker", e)
                viewModel.showMessage("No app available to pick photos.")
            }
        }
    }

    fun focusComposer() {
        try {
            focusRequester.requestFocus()
            keyboard?.show()
        } catch (e: IllegalStateException) {
            // Composer not on screen (error state)
        }
    }

    // ---- One-off events ----
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ThreadEvent.Message -> launch { snackbar.showSnackbar(event.text) }
                ThreadEvent.ScrollToBottom -> launch { scrollToBottom(listState) }
            }
        }
    }

    // ---- Stick to the bottom when new rows arrive while the user is at the bottom ----
    val stickPx = with(density) { 48.dp.roundToPx() }
    val bottomTracker = remember { BottomTracker() }
    val bottomKey = list.items.firstOrNull()?.key
    SideEffect {
        if (bottomTracker.key != bottomKey) {
            val previous = bottomTracker.key
            bottomTracker.key = bottomKey
            // listState still describes the previous data here (before the next measure)
            if (previous != null &&
                listState.firstVisibleItemIndex == 0 &&
                listState.firstVisibleItemScrollOffset <= stickPx
            ) {
                listState.requestScrollToItem(0)
            }
        }
    }

    // ---- "N new messages" pill / scroll-to-bottom button ----
    val atBottom by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset <= stickPx }
    }
    var seenUpTo by rememberSaveable { mutableLongStateOf(0L) }
    LaunchedEffect(atBottom, list.newestMessageAt) {
        if (atBottom || seenUpTo == 0L) seenUpTo = list.newestMessageAt
    }
    val newCount = remember(list, seenUpTo) { if (seenUpTo == 0L) 0 else list.peerMessagesAfter(seenUpTo) }
    val scrolledUp by remember { derivedStateOf { listState.firstVisibleItemIndex > 2 } }

    // ---- Load older messages when the top is reached ----
    LaunchedEffect(listState, list.hasOlder) {
        if (!list.hasOlder) return@LaunchedEffect
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            (info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - 3) to info.totalItemsCount
        }
            .distinctUntilChanged()
            .collect { (nearTop, _) -> if (nearTop) viewModel.loadOlder() }
    }

    // ---- Jump to a replied-to message (loads older pages until it is found) ----
    LaunchedEffect(pendingJump, list) {
        val target = pendingJump ?: return@LaunchedEffect
        val index = list.indexOfMessage(target)
        when {
            index >= 0 -> {
                // Clearing pendingJump restarts this effect, so the scroll runs in the screen's scope
                pendingJump = null
                highlightedId = target
                scope.launch { listState.animateScrollToItem(index) }
            }
            list.loadingOlder -> Unit // wait for the page
            list.hasOlder && jumpPages < MAX_JUMP_PAGES -> {
                jumpPages++
                if (!viewModel.loadOlder()) pendingJump = null
            }
            else -> {
                pendingJump = null
                viewModel.showMessage("The original message isn't available.")
            }
        }
    }
    LaunchedEffect(highlightedId) {
        if (highlightedId != null) {
            delay(1_400L)
            highlightedId = null
        }
    }

    // ---- Calls: the top-bar buttons and "Call back" / "Call again" on a call row all just open the call screen.
    // It asks for the microphone / camera and places the call, so that logic lives in one place. ----
    val callTaps = remember { CallTapGate() }
    val placeCall: (Boolean) -> Unit = remember(viewModel, context) {
        { video: Boolean ->
            // A double tap must not open the call screen twice
            if (callTaps.pass()) {
                keyboard?.hide()
                val peer = CallPeer(
                    username = viewModel.peer,
                    name = currentHeader.name,
                    avatar = currentHeader.avatar,
                )
                try {
                    context.startActivity(
                        CallIntents.call(context, peer, if (video) CallKind.VIDEO else CallKind.AUDIO)
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Couldn't open the call screen", e)
                    viewModel.showMessage("Couldn't start the call. Try again.")
                }
            }
        }
    }

    val callbacks = remember(viewModel) {
        ThreadRowCallbacks(
            onLongPress = { message, bounds ->
                keyboard?.hide()
                selection = ThreadSelection(message, bounds.translate(-root.positionInRoot()))
            },
            onDoubleTap = { message -> viewModel.toggleHeart(message) },
            onSwipeReply = { message ->
                viewModel.startReply(message)
                focusComposer()
            },
            onOpenImage = { message ->
                message.image?.let { image ->
                    viewer = ThreadViewerImage(
                        image = image,
                        title = if (message.mine) "You" else currentHeader.name,
                        subtitle = ChatTime.separator(message.createdAt),
                    )
                }
            },
            onOpenArtwork = { artworkId -> currentOnOpenArtwork(artworkId) },
            onOpenSharedProfile = { profileId -> currentOnOpenProfile(profileId) },
            onOpenLink = { url ->
                if (!openLink(context, url)) viewModel.showMessage("Couldn't open this link.")
            },
            onReplyQuoteClick = { message ->
                message.replyTo?.takeIf { !it.unavailable }?.let { reply ->
                    jumpPages = 0
                    pendingJump = reply.targetId
                }
            },
            onRetry = { message -> viewModel.retrySend(message) },
            onReactionsClick = { message -> reactionsFor = message.id },
            onPeerClick = { currentOnOpenProfile(viewModel.peer) },
            onCall = { video -> placeCall(video) },
        )
    }

    val blurRadius by animateDpAsState(
        targetValue = if (selection != null) 14.dp else 0.dp,
        animationSpec = tween(durationMillis = 180),
        label = "overlayBlur",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .onGloballyPositioned { root.coordinates = it },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier),
        ) {
            ThreadTopBar(
                header = header,
                colors = colors,
                onBack = onBack,
                onOpenProfile = { onOpenProfile(viewModel.peer) },
                showCallButtons = canCall,
                onAudioCall = { placeCall(false) },
                onVideoCall = { placeCall(true) },
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                when (val state = content) {
                    is ThreadContent.Error -> ThreadErrorState(
                        error = state,
                        colors = colors,
                        onRetry = viewModel::retry,
                    )
                    else -> {
                        ThreadMessageList(
                            list = list,
                            listState = listState,
                            header = header,
                            highlightedId = highlightedId,
                            colors = colors,
                            maxBubbleWidth = maxBubbleWidth,
                            callbacks = callbacks,
                            canCall = canCall,
                            onViewProfile = { onOpenProfile(viewModel.peer) },
                        )
                        if (!list.loaded && list.items.isEmpty()) {
                            ThreadCenteredLoading()
                        }
                        JumpControls(
                            newCount = newCount,
                            showPill = newCount > 0 && !atBottom,
                            showButton = scrolledUp,
                            colors = colors,
                            onClick = { scope.launch { scrollToBottom(listState) } },
                        )
                    }
                }
            }
            if (content !is ThreadContent.Error) {
                val replyTarget = viewModel.replyTarget
                val replyDraft = remember(replyTarget, header.name) {
                    replyTarget?.let { target ->
                        ThreadReplyDraftUi(
                            title = if (target.sender == viewModel.me) "Replying to yourself" else "Replying to ${header.name}",
                            preview = target.preview.ifBlank { ThreadListBuilder.previewText(target.type, "") },
                        )
                    }
                }
                ThreadComposer(
                    text = viewModel.draft,
                    onTextChange = viewModel::onDraftChange,
                    onSend = viewModel::sendText,
                    onLike = viewModel::sendLike,
                    onPickPhoto = openPicker,
                    reply = replyDraft,
                    onCancelReply = viewModel::cancelReply,
                    colors = colors,
                    focusRequester = focusRequester,
                )
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp),
        )

        selection?.let { selected ->
            ThreadMessageOverlay(
                selection = selected,
                colors = colors,
                maxBubbleWidth = maxBubbleWidth,
                onReact = { emoji ->
                    viewModel.react(selected.message, emoji)
                    selection = null
                },
                onReply = {
                    viewModel.startReply(selected.message)
                    selection = null
                    focusComposer()
                },
                onCopy = {
                    selected.message.copyText?.let { text ->
                        clipboard.setText(AnnotatedString(text))
                        // Android 13+ shows its own clipboard confirmation
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) viewModel.showMessage("Copied")
                    }
                    selection = null
                },
                onUnsend = {
                    unsendTarget = selected.message
                    selection = null
                },
                onRetry = {
                    viewModel.retrySend(selected.message)
                    selection = null
                },
                onDeleteLocal = {
                    viewModel.deleteFailed(selected.message)
                    selection = null
                },
                onDismiss = { selection = null },
            )
        }
    }

    unsendTarget?.let { target ->
        ThreadUnsendDialog(
            onConfirm = {
                viewModel.unsend(target)
                unsendTarget = null
            },
            onDismiss = { unsendTarget = null },
        )
    }

    // Live: follows the message as reactions change; closes when there is nothing left to show
    val reactionsMessage = remember(list, reactionsFor) {
        reactionsFor?.let { id ->
            list.items.firstNotNullOfOrNull { item -> (item as? ThreadItem.Message)?.ui?.takeIf { it.id == id } }
        }
    }
    val reactions = reactionsMessage?.reactions
    if (reactionsFor != null && reactionsMessage != null && reactions != null) {
        ThreadReactionsSheet(
            reactions = reactions,
            me = viewModel.me,
            header = header,
            colors = colors,
            onRemoveMine = {
                viewModel.react(reactionsMessage, null)
                reactionsFor = null
            },
            onDismiss = { reactionsFor = null },
        )
    } else if (reactionsFor != null && list.loaded) {
        LaunchedEffect(Unit) { reactionsFor = null }
    }

    viewer?.let { image ->
        ThreadImageViewer(image = image, onDismiss = { viewer = null })
    }
}

@Composable
private fun ThreadMessageList(
    list: ThreadListUi,
    listState: LazyListState,
    header: ThreadHeaderUi,
    highlightedId: String?,
    colors: ThreadColors,
    maxBubbleWidth: Dp,
    callbacks: ThreadRowCallbacks,
    canCall: Boolean,
    onViewProfile: () -> Unit,
) {
    LazyColumn(
        state = listState,
        reverseLayout = true,
        contentPadding = PaddingValues(
            start = ThreadDimens.HorizontalPadding,
            end = ThreadDimens.HorizontalPadding,
            top = 8.dp,
            bottom = 10.dp,
        ),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(
            items = list.items,
            key = { it.key },
            contentType = { it.contentType },
        ) { item ->
            when (item) {
                is ThreadItem.Message -> ThreadMessageRow(
                    message = item.ui,
                    peerAvatar = header.avatar,
                    peerName = header.name,
                    highlighted = highlightedId != null && item.ui.id == highlightedId,
                    colors = colors,
                    maxBubbleWidth = maxBubbleWidth,
                    callbacks = callbacks,
                    modifier = Modifier.animateItem(),
                    canCall = canCall,
                )
                is ThreadItem.Separator -> ThreadTimeSeparator(label = item.label, colors = colors)
                ThreadItem.Typing -> ThreadTypingIndicator(
                    avatarUrl = header.avatar,
                    name = header.name,
                    colors = colors,
                    modifier = Modifier.animateItem(),
                )
                ThreadItem.Header -> ThreadProfileHeader(
                    header = header,
                    colors = colors,
                    onViewProfile = onViewProfile,
                )
                ThreadItem.LoadingOlder -> ThreadLoadingOlder()
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.JumpControls(
    newCount: Int,
    showPill: Boolean,
    showButton: Boolean,
    colors: ThreadColors,
    onClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = showPill,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 12.dp),
    ) {
        val pill = RoundedCornerShape(50)
        Row(
            modifier = Modifier
                .shadow(elevation = 6.dp, shape = pill)
                .clip(pill)
                .background(ChatBlue)
                .clickable(onClickLabel = "Show new messages", onClick = onClick)
                .padding(start = 10.dp, end = 14.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = if (newCount == 1) "1 new message" else "$newCount new messages",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
    AnimatedVisibility(
        visible = showButton && !showPill,
        enter = fadeIn() + scaleIn(initialScale = 0.7f),
        exit = fadeOut() + scaleOut(targetScale = 0.7f),
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 16.dp, bottom = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .shadow(elevation = 6.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(colors.menuBackground)
                .clickable(onClickLabel = "Scroll to the latest message", onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = "Latest messages",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

// Plain holders (never trigger recomposition)
private class RootCoordinates {
    var coordinates: LayoutCoordinates? = null

    fun positionInRoot(): Offset = coordinates?.takeIf { it.isAttached }?.positionInRoot() ?: Offset.Zero
}

private class BottomTracker {
    var key: String? = null
}

// One call tap per moment: the call screen takes a beat to cover the buttons
private class CallTapGate {
    private var lastTap = 0L

    fun pass(): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (now - lastTap < CALL_TAP_GAP_MS) return false
        lastTap = now
        return true
    }
}

private suspend fun scrollToBottom(listState: LazyListState) {
    if (listState.firstVisibleItemIndex > 30) listState.scrollToItem(0) else listState.animateScrollToItem(0)
}

private fun cancelChatNotification(context: Context, conversationId: String) {
    if (conversationId.isEmpty()) return
    try {
        NotificationManagerCompat.from(context).cancel(conversationId.hashCode())
    } catch (e: Exception) {
        Log.w(TAG, "Couldn't cancel the chat notification", e)
    }
}

private fun openLink(context: Context, url: String): Boolean = try {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE)
    if (context !is android.app.Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
    true
} catch (e: ActivityNotFoundException) {
    Log.w(TAG, "No app for $url", e)
    false
} catch (e: Exception) {
    Log.w(TAG, "Couldn't open $url", e)
    false
}
