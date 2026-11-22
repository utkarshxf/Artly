package com.orion.templete.data.repository.chat

import android.net.Uri
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.storage.StorageException
import com.orion.templete.data.chat.ChatCoroutineScope
import com.orion.templete.data.chat.ChatErrors
import com.orion.templete.data.chat.ChatIds
import com.orion.templete.data.chat.ChatMediaUploader
import com.orion.templete.data.chat.ChatNotifications
import com.orion.templete.data.chat.ChatUnavailableException
import com.orion.templete.data.model.chat.ArtworkRef
import com.orion.templete.data.model.chat.CallLog
import com.orion.templete.data.model.chat.ProfileRef
import com.orion.templete.data.model.chat.ChatAuthState
import com.orion.templete.data.model.chat.ChatMessage
import com.orion.templete.data.model.chat.ChatNotifyRequest
import com.orion.templete.data.model.chat.ChatUser
import com.orion.templete.data.model.chat.Conversation
import com.orion.templete.data.model.chat.MessagePage
import com.orion.templete.data.model.chat.MessagePreview
import com.orion.templete.data.model.chat.MessageType
import com.orion.templete.data.model.chat.OutgoingMessage
import com.orion.templete.data.model.user_model.SearchUserItem
import com.orion.templete.data.model.user_model.UserDTO
import com.orion.templete.data.network.ApiService
import com.orion.templete.domain.repository.chat.ChatRepository
import com.orion.templete.domain.repository.chat.ChatSession
import com.orion.templete.util.SecureStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/*
 * Firestore implementation of the Instagram-style DMs (wire format: chat_spec.md "Firestore schema").
 *
 *   users/{username}                      {username, name, avatar, lastActive}
 *   conversations/{a__b}                  {usernames, createdAt, updatedAt, lastMessage, unread, lastRead, typing,
 *                                          muted, markedUnread, clearedAt}
 *   conversations/{a__b}/messages/{mid}   {sender, type, text, imageUrl, imageWidth, imageHeight, artwork, replyTo,
 *                                          reactions, createdAt, unsent}
 *                                         type "call" (an audio / video call, written by the backend only) adds
 *                                         call: {id, kind, outcome, durationSec}; it is never replied to, reacted
 *                                         to or unsent
 *
 * - Identity: the Firebase uid is the Artistry username (ChatSession signs in with a backend-minted custom token);
 *   the current username always comes from SecureStorage.getUserId().
 * - Every flow waits for ChatSession.ensureSignedIn() and fails (instead of emitting empty lists) on errors.
 * - Server timestamps are read with ServerTimestampBehavior.ESTIMATE so optimistic writes sort and render at once.
 * - Per-user map entries are always addressed with FieldPath.of(map, username), never with dotted strings, so no
 *   username can be misread as a nested path.
 */
@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val api: ApiService,
    private val secureStorage: SecureStorage,
    private val chatSession: ChatSession,
    private val uploader: ChatMediaUploader,
    private val notifications: ChatNotifications,
    private val appScope: ChatCoroutineScope,
) : ChatRepository {

    // What the composer's content turns into on the wire
    private class Content(val type: MessageType, val fields: Map<String, Any?>, val preview: String)

    private class TypingState {
        var active = false // typing.<me> is currently set on the server (as far as we know)
        var lastWriteAt = 0L // elapsedRealtime of the last typing.<me> write
        var idleJob: Job? = null
    }

    // conversationId -> whether its document exists (learned from listeners, reads and our own creates)
    private val conversationExists = ConcurrentHashMap<String, Boolean>()
    private val typingStates = ConcurrentHashMap<String, TypingState>()

    // Latest known public profile per username (Firestore or backend), for instant first frames
    private val userCache = ConcurrentHashMap<String, ChatUser>()
    // Backend profiles (users/{u} missing or without name/avatar); one lookup per username at a time
    private val backendProfiles = ConcurrentHashMap<String, ChatUser>()
    private val backendLookups = ConcurrentHashMap<String, Deferred<ChatUser?>>()

    override val me: String
        get() = currentUsername().orEmpty()

    override fun conversationIdWith(peer: String): String = ChatIds.conversationId(me, peer)

    // ------------------------------------------------------------------------------------------------ inbox

    override fun observeConversations(): Flow<List<Conversation>> = flow {
        chatSession.ensureSignedIn()
        val me = requireMe()
        val query = conversations()
            .whereArrayContains(F_USERNAMES, me)
            .orderBy(F_UPDATED_AT, Query.Direction.DESCENDING)
            .limit(INBOX_LIMIT)
        emitAll(query.snapshotFlow(MetadataChanges.EXCLUDE).map { snapshot -> inboxFrom(snapshot, me) })
    }.distinctUntilChanged().flowOn(Dispatchers.Default)

    override fun observeConversation(conversationId: String): Flow<Conversation?> = flow {
        chatSession.ensureSignedIn()
        val me = requireMe()
        requireParty(conversationId, me)
        emitAll(
            conversationRef(conversationId).snapshotFlow().map { doc ->
                conversationExists[conversationId] = doc.exists()
                doc.toConversation(me)
            }
        )
    }.distinctUntilChanged().flowOn(Dispatchers.Default)

    override fun observeUnreadConversationCount(): Flow<Int> = observeConversations()
        .map { conversations -> conversations.count { it.hasUnread } }
        .distinctUntilChanged()

    private fun inboxFrom(snapshot: QuerySnapshot, me: String): List<Conversation> =
        snapshot.documents.mapNotNull { doc ->
            conversationExists[doc.id] = true
            doc.toConversation(me)
        }.filter { conversation ->
            // No empty chats (opened but never written to) and nothing the user deleted until a newer message arrives
            conversation.lastMessage != null &&
                !(conversation.clearedAt > 0 && conversation.clearedAt >= conversation.updatedAt)
        }

    // ------------------------------------------------------------------------------------------------ thread

    override fun observeMessages(conversationId: String, limit: Int): Flow<MessagePage> = flow {
        chatSession.ensureSignedIn()
        val me = requireMe()
        requireParty(conversationId, me)
        val window = limit.coerceAtLeast(1)
        val clearedAt: Flow<Long> = conversationRef(conversationId).snapshotFlow()
            .map { doc ->
                conversationExists[conversationId] = doc.exists()
                millisOf(doc.stringMap(F_CLEARED_AT)[me]) ?: 0L
            }
            .distinctUntilChanged()
        // One extra message tells whether older history exists. clearedAt is applied here rather than in the query:
        // Firestore's local view can't match a range filter against a pending serverTimestamp, which would hide
        // optimistic sends in a chat that was deleted-for-me before.
        val messages: Flow<QuerySnapshot> = messagesRef(conversationId)
            .orderBy(F_CREATED_AT, Query.Direction.ASCENDING)
            .limitToLast(window + 1L)
            .snapshotFlow(MetadataChanges.INCLUDE) // pending -> sent transitions of single messages
        emitAll(combine(clearedAt, messages) { cleared, snapshot -> pageFrom(conversationId, snapshot, cleared, window) })
    }.distinctUntilChanged().flowOn(Dispatchers.Default)

    private fun pageFrom(conversationId: String, snapshot: QuerySnapshot, clearedAt: Long, window: Int): MessagePage {
        val documents = snapshot.documents // oldest first; pending server timestamps sort last (newest)
        val parsed = documents.mapNotNull { it.toMessage(conversationId) }
        val visible = parsed.filter { isVisibleAfterClear(it, clearedAt) }
        val overflow = documents.size > window
        // Everything older than a hidden (cleared) message is hidden too
        val hasOlder = overflow && parsed.firstOrNull()?.let { isVisibleAfterClear(it, clearedAt) } == true
        return MessagePage(messages = if (visible.size > window) visible.takeLast(window) else visible, hasOlder = hasOlder)
    }

    // A pending message was necessarily written after the clear (its estimated time may be skewed by the local clock)
    private fun isVisibleAfterClear(message: ChatMessage, clearedAt: Long): Boolean =
        clearedAt <= 0L || message.pending || message.createdAt > clearedAt

    // ------------------------------------------------------------------------------------------------ people

    override fun observeUser(username: String): Flow<ChatUser?> = flow {
        val name = username.trim()
        if (name.isEmpty()) {
            emit(null)
            return@flow
        }
        userCache[name]?.let { emit(it) }
        chatSession.ensureSignedIn()
        emitAll(
            userRef(name).snapshotFlow().transform { doc ->
                if (doc.exists()) {
                    val live = doc.toChatUser(name)
                    if (live.hasProfile()) {
                        emit(remember(live))
                    } else {
                        // The doc only carries presence (profile not published yet): name/avatar from the backend
                        val known = backendProfiles[name]
                        emit(remember(live.withProfileOf(known)))
                        if (known == null) backendProfile(name)?.let { emit(remember(live.withProfileOf(it))) }
                    }
                } else {
                    // Never opened chat v2: fall back to the backend profile (once), keep the username otherwise
                    val fallback = backendProfile(name) ?: userCache[name] ?: ChatUser(username = name)
                    emit(remember(fallback))
                }
            }
        )
    }.distinctUntilChanged().flowOn(Dispatchers.Default)

    override suspend fun getUser(username: String): ChatUser? {
        val name = username.trim()
        if (name.isEmpty()) return null
        return try {
            chatSession.ensureSignedIn()
            val doc = userRef(name).get().await()
            if (doc.exists()) {
                val live = doc.toChatUser(name)
                remember(if (live.hasProfile()) live else live.withProfileOf(backendProfile(name)))
            } else {
                remember(backendProfile(name) ?: userCache[name] ?: ChatUser(username = name))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't load the chat profile of $name", e)
            userCache[name] ?: backendProfile(name)?.let { remember(it) }
        }
    }

    override suspend fun searchUsers(query: String, limit: Int): Result<List<ChatUser>> {
        val term = query.trim().removePrefix("@").trim()
        if (term.isEmpty()) return Result.success(emptyList())
        return try {
            val response = api.searchUsers(term, limit.coerceIn(1, MAX_SEARCH_LIMIT))
            if (!response.isSuccessful) {
                closeQuietly(response.errorBody())
                return Result.failure(IOException("People search failed (HTTP ${response.code()})"))
            }
            val me = currentUsername()
            val items: List<SearchUserItem>? = response.body()?.users
            val users = items.orEmpty().mapNotNull { item ->
                val rawUsername: String? = item.username
                val rawName: String? = item.name
                val rawAvatar: String? = item.profile_pic
                val username = rawUsername?.trim()
                if (username.isNullOrEmpty() || username == me) {
                    null
                } else {
                    ChatUser(
                        username = username,
                        name = rawName?.trim()?.takeIf { it.isNotEmpty() },
                        avatar = rawAvatar?.trim()?.takeIf { it.isNotEmpty() },
                    )
                }
            }.distinctBy { it.username }
            Result.success(users)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun remember(user: ChatUser): ChatUser {
        userCache[user.username] = user
        return user
    }

    private fun ChatUser.hasProfile(): Boolean = !name.isNullOrBlank() || !avatar.isNullOrBlank()

    private fun ChatUser.withProfileOf(profile: ChatUser?): ChatUser =
        if (profile == null) this else copy(name = name?.takeIf { it.isNotBlank() } ?: profile.name,
            avatar = avatar?.takeIf { it.isNotBlank() } ?: profile.avatar)

    private suspend fun backendProfile(username: String): ChatUser? {
        backendProfiles[username]?.let { return it }
        val lookup = backendLookups.computeIfAbsent(username) { appScope.async { fetchBackendProfile(username) } }
        val result = try {
            lookup.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Backend profile of $username unavailable", e)
            null
        }
        backendLookups.remove(username, lookup) // a failed lookup may be retried by the next caller
        if (result != null) backendProfiles[username] = result
        return result
    }

    private suspend fun fetchBackendProfile(username: String): ChatUser? {
        val response = api.getUserByUserId(username)
        if (!response.isSuccessful) {
            closeQuietly(response.errorBody())
            // Unknown user: remember just the username; anything else (server asleep, offline...) is retried later
            return if (response.code() == 404) ChatUser(username = username) else null
        }
        val dto: UserDTO = response.body() ?: return ChatUser(username = username)
        val name: String? = dto.name
        val avatar: String? = dto.profilePicture
        return ChatUser(
            username = username,
            name = name?.trim()?.takeIf { it.isNotEmpty() },
            avatar = avatar?.trim()?.takeIf { it.isNotEmpty() },
        )
    }

    // ------------------------------------------------------------------------------------------------ send

    override suspend fun ensureConversation(peer: String): String {
        chatSession.ensureSignedIn()
        val me = requireMe()
        val other = peer.trim()
        if (other.isEmpty() || other == me) throw IllegalArgumentException("You can't message yourself.")
        val conversationId = ChatIds.conversationId(me, other)
        ensureConversationExists(conversationId, requireParty(conversationId, me))
        return conversationId
    }

    // Creates conversations/{cid} with the six (empty) per-user maps when it doesn't exist yet. No updatedAt: a chat
    // enters the inbox (ordered by updatedAt) with its first message.
    private suspend fun ensureConversationExists(conversationId: String, usernames: List<String>) {
        if (conversationExists[conversationId] == true) return
        val ref = conversationRef(conversationId)
        val existing: DocumentSnapshot? = try {
            ref.get().await()
        } catch (e: FirebaseFirestoreException) {
            // Offline and not cached: we can't know, so queue the create below without waiting for it
            if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) null else throw mapError(e)
        }
        if (existing != null && existing.exists()) {
            conversationExists[conversationId] = true
            return
        }
        val data = hashMapOf<String, Any?>(
            F_USERNAMES to usernames,
            F_CREATED_AT to FieldValue.serverTimestamp(),
            F_LAST_MESSAGE to null,
            F_UNREAD to emptyMap<String, Any>(),
            F_LAST_READ to emptyMap<String, Any>(),
            F_TYPING to emptyMap<String, Any>(),
            F_MUTED to emptyMap<String, Any>(),
            F_MARKED_UNREAD to emptyMap<String, Any>(),
            F_CLEARED_AT to emptyMap<String, Any>(),
        )
        val create = ref.set(data)
        conversationExists[conversationId] = true
        if (existing == null || existing.metadata.isFromCache) {
            // Offline: only the cache said "missing", and waiting for the server would hold the first message back.
            // Writes reach the server in order, so the send batch lands after this create. If the conversation does
            // exist there, the rules reject this create (a set on an existing doc is an update that changes
            // createdAt), nothing is overwritten, and the send batch still applies.
            create.addOnFailureListener { Log.i(TAG, "Queued conversation create not applied: ${it.message}") }
            return
        }
        try {
            create.await()
        } catch (e: FirebaseFirestoreException) {
            conversationExists.remove(conversationId)
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                // The peer created it at the same moment
                val again = try {
                    ref.get(Source.SERVER).await()
                } catch (retry: Exception) {
                    null
                }
                if (again?.exists() == true) {
                    conversationExists[conversationId] = true
                    return
                }
            }
            throw mapError(e)
        }
    }

    override suspend fun send(conversationId: String, message: OutgoingMessage): String {
        chatSession.ensureSignedIn()
        val me = requireMe()
        val usernames = requireParty(conversationId, me)
        val peer = usernames.first { it != me }
        val content = contentOf(message)
        ensureConversationExists(conversationId, usernames)

        val conversation = conversationRef(conversationId)
        val messageRef = conversation.collection(C_MESSAGES).document() // id generated here: optimistic rows keep it
        val messageData = HashMap<String, Any?>(content.fields)
        messageData[M_SENDER] = me
        messageData[M_TYPE] = content.type.wire
        messageData[M_REPLY_TO] = message.replyTo?.let { replyToMap(it) }
        messageData[M_REACTIONS] = emptyMap<String, Any>()
        messageData[F_CREATED_AT] = FieldValue.serverTimestamp()
        messageData[M_UNSENT] = false
        val lastMessage = hashMapOf<String, Any?>(
            P_ID to messageRef.id,
            P_SENDER to me,
            P_TYPE to content.type.wire,
            P_PREVIEW to content.preview,
            P_CREATED_AT to FieldValue.serverTimestamp(),
        )

        val batch = db.batch()
        batch.set(messageRef, messageData)
        batch.updateFields(
            conversation,
            listOf(
                FieldPath.of(F_LAST_MESSAGE) to lastMessage,
                FieldPath.of(F_UPDATED_AT) to FieldValue.serverTimestamp(),
                FieldPath.of(F_UNREAD, peer) to FieldValue.increment(1L),
                FieldPath.of(F_UNREAD, me) to 0L,
                FieldPath.of(F_LAST_READ, me) to FieldValue.serverTimestamp(),
                FieldPath.of(F_TYPING, me) to FieldValue.delete(),
                FieldPath.of(F_MARKED_UNREAD, me) to FieldValue.delete(),
            )
        )
        val commit = batch.commit()
        resetTyping(conversationId) // the batch removed typing.<me>
        // Push to the recipient once the server has the message, even if the caller (screen) is gone by then
        commit.addOnSuccessListener { appScope.launch { notifyRecipient(conversationId, messageRef.id) } }
        try {
            commit.await() // offline: stays pending ("Sending…") and completes once delivered
        } catch (e: FirebaseFirestoreException) {
            throw mapError(e)
        }
        return messageRef.id
    }

    private fun contentOf(message: OutgoingMessage): Content = when (message) {
        is OutgoingMessage.Text -> {
            val text = message.text.trim()
            if (text.isEmpty()) throw IllegalArgumentException("Write a message first.")
            if (text.length > MAX_TEXT) throw IllegalArgumentException("This message is too long.")
            Content(MessageType.TEXT, messageFields(text = text), previewOf(text))
        }
        is OutgoingMessage.Like -> Content(MessageType.LIKE, messageFields(), PREVIEW_LIKE)
        is OutgoingMessage.Image -> {
            val url = message.url.trim()
            if (url.isEmpty() || url.length > MAX_URL) throw IllegalArgumentException("This photo couldn't be sent.")
            val caption = message.caption.trim()
            if (caption.length > MAX_TEXT) throw IllegalArgumentException("This message is too long.")
            Content(
                MessageType.IMAGE,
                messageFields(
                    text = caption,
                    imageUrl = url,
                    imageWidth = message.width?.takeIf { it > 0 },
                    imageHeight = message.height?.takeIf { it > 0 },
                ),
                PREVIEW_PHOTO,
            )
        }
        is OutgoingMessage.Artwork -> {
            val artwork = message.artwork
            val id = artwork.id.trim()
            if (id.isEmpty() || id.length > MAX_ID || '/' in id) throw IllegalArgumentException("This post can't be shared.")
            val artworkMap = hashMapOf<String, Any?>(
                A_ID to id,
                A_TITLE to artwork.title?.trim()?.take(MAX_TITLE),
                A_IMAGE_URL to artwork.imageUrl?.trim()?.takeIf { it.isNotEmpty() && it.length <= MAX_URL },
                A_ARTIST_NAME to artwork.artistName?.trim()?.take(MAX_NAME),
            )
            Content(MessageType.ARTWORK, messageFields(artwork = artworkMap), PREVIEW_POST)
        }
        is OutgoingMessage.Profile -> {
            val profile = message.profile
            val id = profile.id.trim()
            if (id.isEmpty() || id.length > MAX_ID || '/' in id) throw IllegalArgumentException("This profile can't be shared.")
            val profileMap = hashMapOf<String, Any?>(
                R_ID to id,
                R_NAME to profile.name?.trim()?.take(MAX_NAME),
                R_AVATAR to profile.avatar?.trim()?.takeIf { it.isNotEmpty() && it.length <= MAX_URL },
                R_SUBTITLE to profile.subtitle?.trim()?.take(MAX_NAME),
            )
            // "profile" is only written on profile messages, so other messages keep the exact shape older rules expect
            Content(MessageType.PROFILE, messageFields() + (M_PROFILE to profileMap), PREVIEW_PROFILE)
        }
    }

    private fun messageFields(
        text: String = "",
        imageUrl: String? = null,
        imageWidth: Int? = null,
        imageHeight: Int? = null,
        artwork: Map<String, Any?>? = null,
    ): Map<String, Any?> = hashMapOf(
        M_TEXT to text,
        M_IMAGE_URL to imageUrl,
        M_IMAGE_WIDTH to imageWidth,
        M_IMAGE_HEIGHT to imageHeight,
        M_ARTWORK to artwork,
    )

    private fun replyToMap(reply: MessagePreview): Map<String, Any?>? {
        // A call row can't be replied to (the UI doesn't offer it): the message goes out without the quote
        if (reply.type == MessageType.CALL) return null
        val id = reply.id.trim()
        if (id.isEmpty() || id.length > MAX_ID) return null
        return hashMapOf(
            P_ID to id,
            P_SENDER to reply.sender.take(MAX_USERNAME),
            P_TYPE to reply.type.wire,
            P_PREVIEW to previewOf(reply.preview),
            P_CREATED_AT to reply.createdAt.takeIf { it > 0L }?.let { Timestamp(Date(it)) },
        )
    }

    private suspend fun notifyRecipient(conversationId: String, messageId: String) {
        repeat(NOTIFY_ATTEMPTS) { attempt ->
            try {
                val response = api.chatNotify(ChatNotifyRequest(conversationId = conversationId, messageId = messageId))
                closeQuietly(response.body())
                closeQuietly(response.errorBody())
                // 5xx other than "not configured" may be the backend waking up; anything else won't get better
                if (response.isSuccessful || response.code() < 500 || response.code() == 503) {
                    if (!response.isSuccessful) Log.w(TAG, "Chat push not sent (HTTP ${response.code()})")
                    return
                }
                Log.w(TAG, "Chat push failed (HTTP ${response.code()}), attempt ${attempt + 1}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Chat push request failed, attempt ${attempt + 1}", e)
            }
            if (attempt < NOTIFY_ATTEMPTS - 1) delay(NOTIFY_RETRY_DELAY_MS)
        }
    }

    override suspend fun uploadImage(
        conversationId: String,
        uri: Uri,
        onProgress: (Float) -> Unit,
    ): Triple<String, Int, Int> {
        chatSession.ensureSignedIn()
        val me = requireMe()
        requireParty(conversationId, me)
        return try {
            uploader.upload(conversationId, me, uri, onProgress)
        } catch (e: CancellationException) {
            throw e
        } catch (e: StorageException) {
            throw when (e.errorCode) {
                StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> ChatUnavailableException(ChatErrors.OFFLINE, cause = e)
                StorageException.ERROR_QUOTA_EXCEEDED -> ChatUnavailableException(ChatErrors.SERVER, cause = e)
                else -> ChatUnavailableException(ERROR_UPLOAD, cause = e)
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ state writes

    override suspend fun markRead(conversationId: String) {
        chatSession.ensureSignedIn()
        val me = requireMe()
        requireParty(conversationId, me)
        notifications.clearConversation(conversationId)
        if (conversationExists[conversationId] == false) return // nothing to read yet
        val task = conversationRef(conversationId).updateFields(
            listOf(
                FieldPath.of(F_UNREAD, me) to 0L,
                FieldPath.of(F_LAST_READ, me) to FieldValue.serverTimestamp(),
                FieldPath.of(F_MARKED_UNREAD, me) to FieldValue.delete(),
            )
        )
        awaitWrite(task, "Mark as read", ignoreMissing = true)
    }

    // typing.<me>: throttled while typing, removed after 5 s without keystrokes, on send and on setTyping(false).
    // Never throws: typing is best effort.
    override suspend fun setTyping(conversationId: String, typing: Boolean) {
        val me = currentUsername() ?: return
        if (ChatIds.peerOf(conversationId, me) == null) return
        if (chatSession.authState.value != ChatAuthState.Ready) return
        val state = typingStates.getOrPut(conversationId) { TypingState() }
        val value: Any = synchronized(state) {
            state.idleJob?.cancel()
            state.idleJob = null
            if (typing) {
                // Only chats that exist can carry typing.<me>; a brand-new thread starts with its first message
                if (conversationExists[conversationId] != true) return
                state.idleJob = appScope.launch {
                    delay(TYPING_IDLE_MS)
                    stopTypingIfIdle(conversationId, me, coroutineContext[Job])
                }
                val now = SystemClock.elapsedRealtime()
                if (state.active && now - state.lastWriteAt < TYPING_THROTTLE_MS) return
                state.active = true
                state.lastWriteAt = now
                FieldValue.serverTimestamp()
            } else {
                if (!state.active) return
                state.active = false
                FieldValue.delete()
            }
        }
        writeTyping(conversationId, me, value)
    }

    // 5 s without keystrokes; ignored when a newer keystroke already replaced this timer
    private fun stopTypingIfIdle(conversationId: String, me: String, timer: Job?) {
        val state = typingStates[conversationId] ?: return
        synchronized(state) {
            if (timer == null || state.idleJob !== timer) return
            state.idleJob = null
            if (!state.active) return
            state.active = false
        }
        writeTyping(conversationId, me, FieldValue.delete())
    }

    private fun writeTyping(conversationId: String, me: String, value: Any) {
        try {
            conversationRef(conversationId).update(FieldPath.of(F_TYPING, me), value)
                .addOnFailureListener { Log.w(TAG, "Typing update failed", it) }
        } catch (e: Exception) {
            Log.w(TAG, "Typing update failed", e)
        }
    }

    private fun resetTyping(conversationId: String) {
        val state = typingStates[conversationId] ?: return
        synchronized(state) {
            state.idleJob?.cancel()
            state.idleJob = null
            state.active = false
        }
    }

    override suspend fun setReaction(conversationId: String, messageId: String, emoji: String?) {
        chatSession.ensureSignedIn()
        val me = requireMe()
        requireParty(conversationId, me)
        val reaction = emoji?.trim()?.takeIf { it.isNotEmpty() }
        if (reaction != null && reaction.length > MAX_REACTION) throw IllegalArgumentException("Unsupported reaction.")
        val task = messageRef(conversationId, messageId)
            .update(FieldPath.of(M_REACTIONS, me), reaction ?: FieldValue.delete())
        awaitWrite(task, "Reaction")
    }

    // Needs the server (transaction): the conversation's preview may only change if this is still its last message
    override suspend fun unsend(conversationId: String, message: ChatMessage) {
        chatSession.ensureSignedIn()
        val me = requireMe()
        requireParty(conversationId, me)
        // Call rows belong to the backend (their sender is whoever started the call)
        if (message.type == MessageType.CALL) throw IllegalArgumentException("Calls can't be unsent.")
        if (message.sender != me) throw IllegalArgumentException("You can only unsend your own messages.")
        if (message.unsent) return
        val conversation = conversationRef(conversationId)
        val target = messageRef(conversationId, message.id)
        val wipe = hashMapOf<String, Any?>(
            M_UNSENT to true,
            M_TEXT to "",
            M_IMAGE_URL to null,
            M_ARTWORK to null,
            M_REPLY_TO to null,
            M_REACTIONS to emptyMap<String, Any>(),
        )
        if (message.type == MessageType.PROFILE) wipe[M_PROFILE] = null
        try {
            db.runTransaction { transaction ->
                val current = transaction.get(conversation)
                val lastId = asStringMap(current.get(F_LAST_MESSAGE))?.get(P_ID) as? String
                transaction.update(target, wipe)
                if (current.exists() && lastId == message.id) {
                    transaction.update(conversation, FieldPath.of(F_LAST_MESSAGE, P_PREVIEW), PREVIEW_UNSENT)
                }
                null
            }.await()
        } catch (e: FirebaseFirestoreException) {
            throw mapError(e)
        }
    }

    override suspend fun setMuted(conversationId: String, muted: Boolean) {
        chatSession.ensureSignedIn()
        val me = requireMe()
        requireParty(conversationId, me)
        val task = conversationRef(conversationId).update(FieldPath.of(F_MUTED, me), muted)
        awaitWrite(task, "Mute")
    }

    override suspend fun setMarkedUnread(conversationId: String, unread: Boolean) {
        chatSession.ensureSignedIn()
        val me = requireMe()
        requireParty(conversationId, me)
        val value: Any = if (unread) true else FieldValue.delete()
        val task = conversationRef(conversationId).update(FieldPath.of(F_MARKED_UNREAD, me), value)
        awaitWrite(task, "Mark as unread")
    }

    // "Delete chat" for me: hides the history up to now and the conversation until a newer message arrives
    override suspend fun deleteForMe(conversationId: String) {
        chatSession.ensureSignedIn()
        val me = requireMe()
        requireParty(conversationId, me)
        notifications.clearConversation(conversationId)
        val task = conversationRef(conversationId).updateFields(
            listOf(
                FieldPath.of(F_CLEARED_AT, me) to FieldValue.serverTimestamp(),
                FieldPath.of(F_UNREAD, me) to 0L,
                FieldPath.of(F_MARKED_UNREAD, me) to FieldValue.delete(),
            )
        )
        awaitWrite(task, "Delete chat")
    }

    // Waits for the server to accept a write, but not forever: offline, the write stays queued (and is shown
    // optimistically by the listeners) while the caller moves on.
    private suspend fun awaitWrite(task: Task<Void>, what: String, ignoreMissing: Boolean = false) {
        try {
            val acknowledged = withTimeoutOrNull(WRITE_ACK_TIMEOUT_MS) {
                task.await()
                true
            }
            if (acknowledged == null) task.addOnFailureListener { Log.w(TAG, "$what failed", it) }
        } catch (e: FirebaseFirestoreException) {
            if (ignoreMissing && e.code == FirebaseFirestoreException.Code.NOT_FOUND) return
            throw mapError(e)
        }
    }

    // ------------------------------------------------------------------------------------------------ parsing

    private fun DocumentSnapshot.toConversation(me: String): Conversation? {
        if (!exists()) return null
        val usernames = (get(F_USERNAMES) as? List<*>)?.filterIsInstance<String>().orEmpty()
        if (usernames.size != 2 || me !in usernames) return null
        val peer = usernames.firstOrNull { it != me } ?: return null
        val unread = stringMap(F_UNREAD)
        val lastRead = stringMap(F_LAST_READ)
        val typing = stringMap(F_TYPING)
        val muted = stringMap(F_MUTED)
        val markedUnread = stringMap(F_MARKED_UNREAD)
        val clearedAt = stringMap(F_CLEARED_AT)
        val lastMessage = previewFrom(get(F_LAST_MESSAGE, ESTIMATE))
        val updatedAt = millisOf(get(F_UPDATED_AT, ESTIMATE))
            ?: lastMessage?.createdAt
            ?: millisOf(get(F_CREATED_AT, ESTIMATE))
            ?: 0L
        return Conversation(
            id = id,
            usernames = usernames,
            peer = peer,
            lastMessage = lastMessage,
            updatedAt = updatedAt,
            unreadCount = (unread[me] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0,
            markedUnread = markedUnread[me] == true,
            muted = muted[me] == true,
            myLastRead = millisOf(lastRead[me]) ?: 0L,
            peerLastRead = millisOf(lastRead[peer]) ?: 0L,
            peerTypingAt = millisOf(typing[peer]) ?: 0L,
            clearedAt = millisOf(clearedAt[me]) ?: 0L,
        )
    }

    private fun DocumentSnapshot.toMessage(conversationId: String): ChatMessage? {
        if (!exists()) return null
        val sender = string(M_SENDER)?.takeIf { it.isNotEmpty() } ?: return null
        val artwork = asStringMap(get(M_ARTWORK))?.let { map ->
            val artworkId = (map[A_ID] as? String)?.takeIf { it.isNotBlank() }
            artworkId?.let {
                ArtworkRef(
                    id = it,
                    title = map[A_TITLE] as? String,
                    imageUrl = map[A_IMAGE_URL] as? String,
                    artistName = map[A_ARTIST_NAME] as? String,
                )
            }
        }
        val profile = asStringMap(get(M_PROFILE))?.let { map ->
            (map[R_ID] as? String)?.takeIf { it.isNotBlank() }?.let {
                ProfileRef(
                    id = it,
                    name = map[R_NAME] as? String,
                    avatar = map[R_AVATAR] as? String,
                    subtitle = map[R_SUBTITLE] as? String,
                )
            }
        }
        // A call row is drawn from this map; if it is missing or incomplete the row falls back to the text
        val call = asStringMap(get(M_CALL))?.let { map ->
            val callId = (map[K_ID] as? String)?.takeIf { it.isNotBlank() }
            val kind = map[K_KIND] as? String
            val outcome = (map[K_OUTCOME] as? String)?.takeIf { it.isNotBlank() }
            if (callId == null || kind == null || outcome == null) {
                null
            } else {
                CallLog(
                    id = callId,
                    video = kind == CALL_KIND_VIDEO,
                    outcome = outcome,
                    durationSec = (map[K_DURATION_SEC] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0,
                )
            }
        }
        val reactions = HashMap<String, String>()
        asStringMap(get(M_REACTIONS))?.forEach { (user, emoji) ->
            if (emoji is String && emoji.isNotEmpty()) reactions[user] = emoji
        }
        // Pending = the create itself isn't acknowledged yet (createdAt still an unresolved serverTimestamp);
        // a pending reaction on an old message doesn't make it "Sending…"
        val pending = metadata.hasPendingWrites() && get(F_CREATED_AT) !is Timestamp
        val createdAt = millisOf(get(F_CREATED_AT, ESTIMATE))
            ?: if (pending) System.currentTimeMillis() else 0L
        return ChatMessage(
            id = id,
            conversationId = conversationId,
            sender = sender,
            type = MessageType.fromWire(string(M_TYPE)),
            text = string(M_TEXT).orEmpty(),
            imageUrl = string(M_IMAGE_URL)?.takeIf { it.isNotBlank() },
            imageWidth = (get(M_IMAGE_WIDTH) as? Number)?.toInt()?.takeIf { it > 0 },
            imageHeight = (get(M_IMAGE_HEIGHT) as? Number)?.toInt()?.takeIf { it > 0 },
            artwork = artwork,
            profile = profile,
            call = call,
            replyTo = previewFrom(get(M_REPLY_TO, ESTIMATE)),
            reactions = reactions,
            createdAt = createdAt,
            pending = pending,
            unsent = get(M_UNSENT) == true,
        )
    }

    private fun DocumentSnapshot.toChatUser(username: String): ChatUser = ChatUser(
        username = username,
        name = string(U_NAME)?.trim()?.takeIf { it.isNotEmpty() },
        avatar = string(U_AVATAR)?.trim()?.takeIf { it.isNotEmpty() },
        lastActive = millisOf(get(U_LAST_ACTIVE, ESTIMATE)),
    )

    // Typed getters (getString/getBoolean...) throw on an unexpected type; one odd document must not break a screen
    private fun DocumentSnapshot.string(field: String): String? = get(field) as? String

    private fun previewFrom(value: Any?): MessagePreview? {
        val map = asStringMap(value) ?: return null
        val id = (map[P_ID] as? String)?.takeIf { it.isNotEmpty() } ?: return null
        return MessagePreview(
            id = id,
            sender = map[P_SENDER] as? String ?: "",
            type = MessageType.fromWire(map[P_TYPE] as? String),
            preview = map[P_PREVIEW] as? String ?: "",
            createdAt = millisOf(map[P_CREATED_AT]) ?: 0L,
        )
    }

    // A per-user map field (username -> value); server timestamps inside are estimated
    private fun DocumentSnapshot.stringMap(field: String): Map<String, Any?> =
        asStringMap(get(field, ESTIMATE)).orEmpty()

    private fun asStringMap(value: Any?): Map<String, Any?>? {
        val raw = value as? Map<*, *> ?: return null
        val result = HashMap<String, Any?>(raw.size)
        for ((key, entry) in raw) if (key is String) result[key] = entry
        return result
    }

    private fun millisOf(value: Any?): Long? = when (value) {
        is Timestamp -> value.seconds * 1000L + value.nanoseconds / 1_000_000
        is Date -> value.time
        is Number -> value.toLong()
        else -> null
    }

    // Lists/inbox previews: whitespace collapsed, at most 120 characters (never splitting an emoji)
    private fun previewOf(text: String): String {
        val flat = WHITESPACE.replace(text, " ").trim()
        if (flat.length <= MAX_PREVIEW) return flat
        var end = MAX_PREVIEW
        if (Character.isHighSurrogate(flat[end - 1])) end--
        return flat.substring(0, end).trimEnd()
    }

    // ------------------------------------------------------------------------------------------------ plumbing

    private fun currentUsername(): String? = secureStorage.getUserId()?.trim()?.takeIf { it.isNotEmpty() }

    private fun requireMe(): String = currentUsername() ?: throw ChatUnavailableException(ChatErrors.SIGNED_OUT)

    // The two parties (sorted) of a conversation of mine; anything else is a programming error or a stale id
    private fun requireParty(conversationId: String, me: String): List<String> =
        ChatIds.partiesOf(conversationId, me)
            ?: throw IllegalArgumentException("This conversation isn't available.")

    private fun conversations(): CollectionReference = db.collection(C_CONVERSATIONS)

    private fun conversationRef(conversationId: String): DocumentReference = conversations().document(conversationId)

    private fun messagesRef(conversationId: String): CollectionReference =
        conversationRef(conversationId).collection(C_MESSAGES)

    private fun messageRef(conversationId: String, messageId: String): DocumentReference {
        val id = messageId.trim()
        if (id.isEmpty() || '/' in id) throw IllegalArgumentException("This message isn't available.")
        return messagesRef(conversationId).document(id)
    }

    private fun userRef(username: String): DocumentReference = db.collection(C_USERS).document(username)

    private fun Query.snapshotFlow(metadataChanges: MetadataChanges): Flow<QuerySnapshot> = callbackFlow {
        val registration = addSnapshotListener(metadataChanges) { snapshot, error ->
            if (error != null) {
                close(mapError(error))
                return@addSnapshotListener
            }
            if (snapshot != null) trySend(snapshot)
        }
        awaitClose { registration.remove() }
    }.conflate() // every snapshot is a complete state: a slow collector only needs the latest

    private fun DocumentReference.snapshotFlow(): Flow<DocumentSnapshot> = callbackFlow {
        val registration = addSnapshotListener(MetadataChanges.EXCLUDE) { snapshot, error ->
            if (error != null) {
                close(mapError(error))
                return@addSnapshotListener
            }
            if (snapshot != null) trySend(snapshot)
        }
        awaitClose { registration.remove() }
    }.conflate()

    // Firestore failures as user-presentable errors (the original stays attached as the cause)
    private fun mapError(error: Throwable): Throwable {
        if (error !is FirebaseFirestoreException) return error
        Log.w(TAG, "Firestore error ${error.code}", error)
        val message = when (error.code) {
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> ChatErrors.OFFLINE
            FirebaseFirestoreException.Code.FAILED_PRECONDITION, // e.g. an index that isn't deployed yet
            FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED,
            FirebaseFirestoreException.Code.INTERNAL,
            FirebaseFirestoreException.Code.ABORTED,
            FirebaseFirestoreException.Code.UNKNOWN -> ChatErrors.SERVER
            else -> ChatErrors.GENERIC
        }
        return ChatUnavailableException(message, cause = error)
    }

    private fun closeQuietly(body: okhttp3.ResponseBody?) {
        try {
            body?.close()
        } catch (e: Exception) {
            // nothing to do
        }
    }

    private fun varargsOf(fields: List<Pair<FieldPath, Any?>>): Array<Any?> {
        val rest = arrayOfNulls<Any>(2 * (fields.size - 1))
        for (i in 1 until fields.size) {
            rest[2 * (i - 1)] = fields[i].first
            rest[2 * (i - 1) + 1] = fields[i].second
        }
        return rest
    }

    private fun WriteBatch.updateFields(ref: DocumentReference, fields: List<Pair<FieldPath, Any?>>): WriteBatch =
        update(ref, fields[0].first, fields[0].second, *varargsOf(fields))

    private fun DocumentReference.updateFields(fields: List<Pair<FieldPath, Any?>>): Task<Void> =
        update(fields[0].first, fields[0].second, *varargsOf(fields))

    private companion object {
        const val TAG = "ChatRepository"
        val ESTIMATE = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        val WHITESPACE = Regex("\\s+")

        const val C_USERS = "users"
        const val C_CONVERSATIONS = "conversations"
        const val C_MESSAGES = "messages"

        // users/{username}
        const val U_NAME = "name"
        const val U_AVATAR = "avatar"
        const val U_LAST_ACTIVE = "lastActive"

        // conversations/{cid}
        const val F_USERNAMES = "usernames"
        const val F_CREATED_AT = "createdAt"
        const val F_UPDATED_AT = "updatedAt"
        const val F_LAST_MESSAGE = "lastMessage"
        const val F_UNREAD = "unread"
        const val F_LAST_READ = "lastRead"
        const val F_TYPING = "typing"
        const val F_MUTED = "muted"
        const val F_MARKED_UNREAD = "markedUnread"
        const val F_CLEARED_AT = "clearedAt"

        // conversations/{cid}/messages/{mid} (createdAt shares F_CREATED_AT)
        const val M_SENDER = "sender"
        const val M_TYPE = "type"
        const val M_TEXT = "text"
        const val M_IMAGE_URL = "imageUrl"
        const val M_IMAGE_WIDTH = "imageWidth"
        const val M_IMAGE_HEIGHT = "imageHeight"
        const val M_ARTWORK = "artwork"
        const val M_PROFILE = "profile"
        const val M_CALL = "call"
        const val M_REPLY_TO = "replyTo"
        const val M_REACTIONS = "reactions"
        const val M_UNSENT = "unsent"

        // lastMessage / replyTo maps
        const val P_ID = "id"
        const val P_SENDER = "sender"
        const val P_TYPE = "type"
        const val P_PREVIEW = "preview"
        const val P_CREATED_AT = "createdAt"

        // artwork map
        const val A_ID = "id"
        const val A_TITLE = "title"
        const val A_IMAGE_URL = "imageUrl"
        const val A_ARTIST_NAME = "artistName"

        // profile map
        const val R_ID = "id"
        const val R_NAME = "name"
        const val R_AVATAR = "avatar"
        const val R_SUBTITLE = "subtitle"

        // call map (messages of type "call")
        const val K_ID = "id"
        const val K_KIND = "kind"
        const val K_OUTCOME = "outcome"
        const val K_DURATION_SEC = "durationSec"
        const val CALL_KIND_VIDEO = "video"

        const val PREVIEW_PHOTO = "Sent a photo"
        const val PREVIEW_POST = "Shared a post"
        const val PREVIEW_PROFILE = "Shared a profile"
        const val PREVIEW_LIKE = "❤️"
        const val PREVIEW_UNSENT = "Unsent a message"
        const val ERROR_UPLOAD = "Couldn't upload the photo. Please try again."

        const val INBOX_LIMIT = 100L
        const val MAX_SEARCH_LIMIT = 50
        const val MAX_TEXT = 4000
        const val MAX_PREVIEW = 120
        const val MAX_URL = 2048
        const val MAX_ID = 128
        const val MAX_TITLE = 500
        const val MAX_NAME = 200
        const val MAX_USERNAME = 100
        const val MAX_REACTION = 32

        // Safety net only: callers already send at most one "typing" every 3 s; a slightly shorter gate here keeps
        // that 3 s rhythm from being halved by millisecond differences between the two clocks
        const val TYPING_THROTTLE_MS = 2_500L
        const val TYPING_IDLE_MS = 5_000L
        const val WRITE_ACK_TIMEOUT_MS = 15_000L
        const val NOTIFY_ATTEMPTS = 2
        const val NOTIFY_RETRY_DELAY_MS = 5_000L
    }
}
