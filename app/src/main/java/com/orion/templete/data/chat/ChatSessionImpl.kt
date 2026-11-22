package com.orion.templete.data.chat

import android.content.Context
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.orion.templete.data.model.chat.ChatAuthState
import com.orion.templete.data.network.ApiService
import com.orion.templete.domain.repository.chat.ChatSession
import com.orion.templete.util.SecureStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/*
 * App-wide chat session.
 * - Identity: Firebase custom token minted by the backend (POST chat/token), so the Firebase uid == username.
 *   Concurrent callers share one in-flight sign-in (guarded by a Mutex); the Firebase session persists across
 *   restarts, so a cold start normally needs no network at all.
 * - Profile: users/{me} = {username, name, avatar, lastActive} (lastActive is written with every write, the
 *   security rules require lastActive == request.time).
 * - Presence: lastActive heartbeat every 60 s while the app is in the foreground (ProcessLifecycleOwner).
 * - Push: this device's FCM token in users/{me}/private/devices.tokens (arrayUnion / arrayRemove).
 */
@Singleton
class ChatSessionImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val messaging: FirebaseMessaging,
    private val api: ApiService,
    private val secureStorage: SecureStorage,
    private val notifications: ChatNotifications,
    private val appScope: ChatCoroutineScope,
) : ChatSession {

    private val _authState = MutableStateFlow(initialState())
    override val authState: StateFlow<ChatAuthState> = _authState.asStateFlow()

    @Volatile
    private var activeConversation: String? = null

    override var activeConversationId: String?
        get() = activeConversation
        set(value) {
            activeConversation = value
            // The open thread shows everything: drop its notification and the lines it had collected
            if (value != null) notifications.clearConversation(value)
        }

    // This device's registered push token and the account it belongs to. Kept out of Auto Backup
    // (noBackupFilesDir): restored onto a new phone, it would make that phone unregister the old phone's token.
    private val tokenFile by lazy { File(context.noBackupFilesDir, TOKEN_FILE) }
    private val tokenLock = Any()

    private val signInMutex = Mutex()
    private var signInJob: Deferred<Unit>? = null // guarded by signInMutex
    private var signInJobFor: String? = null // guarded by signInMutex
    private val generation = AtomicInteger(0) // bumped by signOut so a sign-in racing it is thrown away
    private val bootstrappedFor = AtomicReference<String?>(null)
    private val startJob = AtomicReference<Job?>(null)

    private val presenceUser = MutableStateFlow<String?>(null)
    private val processStarted = MutableStateFlow(false)
    @Volatile
    private var presenceWrite: Task<Void>? = null

    init {
        notifications.ensureChannel()
        appScope.launch(Dispatchers.Main) {
            ProcessLifecycleOwner.get().lifecycle.addObserver(LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> processStarted.value = true
                    Lifecycle.Event.ON_STOP -> {
                        processStarted.value = false
                        // Leaving the app: stamp the exact time, so "Active N minutes ago" starts from now
                        presenceUser.value?.let { writePresence(it) }
                    }
                    else -> Unit
                }
            })
        }
        appScope.launch {
            combine(processStarted, presenceUser) { started, user -> if (started) user else null }
                .distinctUntilChanged()
                .collectLatest { user ->
                    if (user == null) return@collectLatest
                    while (true) {
                        writePresence(user)
                        delay(PRESENCE_INTERVAL_MS)
                    }
                }
        }
    }

    // True while the given conversation is on screen and the app is in the foreground
    fun isConversationVisible(conversationId: String): Boolean =
        processStarted.value && activeConversation == conversationId

    override fun start() {
        if (currentUsername() == null) return
        val running = startJob.get()
        if (running?.isActive == true) return
        val job = appScope.launch {
            var attempt = 0
            while (true) {
                try {
                    ensureSignedIn()
                    return@launch
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    val retryable = !(e is ChatUnavailableException && e.notConfigured) && currentUsername() != null
                    if (!retryable || attempt >= START_RETRY_DELAYS_MS.size) {
                        Log.w(TAG, "Chat session could not start", e)
                        return@launch
                    }
                    delay(START_RETRY_DELAYS_MS[attempt++])
                }
            }
        }
        if (!startJob.compareAndSet(running, job)) job.cancel()
    }

    override suspend fun ensureSignedIn() {
        val me = currentUsername()
        if (me == null) {
            _authState.value = ChatAuthState.Error(ChatErrors.SIGNED_OUT)
            throw ChatUnavailableException(ChatErrors.SIGNED_OUT)
        }
        if (auth.currentUser?.uid == me) {
            onReady(me)
            return
        }
        val job: Deferred<Unit>? = signInMutex.withLock {
            val inFlight = signInJob
            when {
                auth.currentUser?.uid == me -> null
                inFlight != null && inFlight.isActive && signInJobFor == me -> inFlight
                else -> appScope.async { signIn(me) }.also {
                    signInJob = it
                    signInJobFor = me
                }
            }
        }
        // The sign-in itself runs in the app scope, so a screen that goes away doesn't abort it for everyone else
        if (job == null) {
            onReady(me)
            return
        }
        try {
            job.await()
        } catch (e: CancellationException) {
            // Our caller was cancelled: propagate. Otherwise the shared sign-in was cancelled by a logout, which
            // must reach the caller as an error (a bare CancellationException would end its flow silently).
            currentCoroutineContext().ensureActive()
            throw ChatUnavailableException(ChatErrors.SIGNED_OUT, cause = e)
        }
    }

    private suspend fun signIn(me: String) {
        val startedIn = generation.get()
        _authState.value = ChatAuthState.SigningIn
        try {
            val token = fetchCustomToken(me)
            auth.signInWithCustomToken(token).await()
            if (generation.get() != startedIn || currentUsername() != me) {
                // Logged out (or switched account) while we were signing in
                if (auth.currentUser?.uid == me) auth.signOut()
                throw ChatUnavailableException(ChatErrors.SIGNED_OUT)
            }
            if (auth.currentUser?.uid != me) throw ChatUnavailableException(ChatErrors.GENERIC)
            onReady(me)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            val error = e as? ChatUnavailableException ?: toChatError(e)
            Log.w(TAG, "Chat sign-in failed: ${error.message}", e)
            _authState.value = ChatAuthState.Error(error.message, error.notConfigured)
            throw error
        }
    }

    private suspend fun fetchCustomToken(me: String): String {
        val response = api.chatToken()
        if (response.isSuccessful) {
            val body = response.body()
            val token: String? = body?.token
            val uid: String? = body?.uid
            if (token.isNullOrBlank()) throw ChatUnavailableException(ChatErrors.GENERIC)
            if (!uid.isNullOrBlank() && uid != me) throw ChatUnavailableException(ChatErrors.WRONG_ACCOUNT)
            return token
        }
        val code = response.code()
        val errorBody = try {
            response.errorBody()?.string()
        } catch (e: Exception) {
            null
        }
        throw when {
            // The backend answers 503 {"message":"Chat is not configured yet"} until it has Firebase credentials.
            // An HTML 503 page comes from the hosting platform (app restarting) and is only temporary.
            code == 503 && errorBody?.trimStart()?.startsWith("<") != true ->
                ChatUnavailableException(ChatErrors.NOT_CONFIGURED, notConfigured = true)
            code == 401 || code == 403 -> ChatUnavailableException(ChatErrors.SESSION_EXPIRED)
            code >= 500 -> ChatUnavailableException(ChatErrors.SERVER)
            else -> ChatUnavailableException(ChatErrors.GENERIC)
        }
    }

    private fun toChatError(e: Throwable): ChatUnavailableException = when (e) {
        is IOException, is FirebaseNetworkException -> ChatUnavailableException(ChatErrors.OFFLINE, cause = e)
        else -> ChatUnavailableException(ChatErrors.GENERIC, cause = e)
    }

    // Signed in as `me`: publish the profile and push token once per account and keep presence going
    private fun onReady(me: String) {
        if (_authState.value != ChatAuthState.Ready) _authState.value = ChatAuthState.Ready
        presenceUser.value = me
        if (bootstrappedFor.getAndSet(me) == me) return
        appScope.launch {
            try {
                publishProfile(me)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Couldn't publish the chat profile", e)
            }
        }
        appScope.launch {
            try {
                val token = withTimeoutOrNull(TOKEN_TIMEOUT_MS) { messaging.token.await() }
                if (token != null) registerPushToken(token)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Couldn't register this device for chat notifications", e)
            }
        }
    }

    private suspend fun publishProfile(me: String) {
        // 1) What this device already knows, right away
        val cached = secureStorage.getUserDetails()?.takeIf { it.id == me }
        val cachedName = cached?.name?.takeIf { it.isNotBlank() }
        val cachedAvatar = cached?.profilePicture?.takeIf { it.isNotBlank() }
        if (cachedName != null || cachedAvatar != null) {
            writeProfile(me, cachedName, cachedAvatar)
        }
        // 2) The backend profile is authoritative (also clears a removed avatar)
        val fresh = try {
            val response = api.getUserByUserId(me)
            if (response.isSuccessful) response.body() else null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't load the profile for chat", e)
            null
        }
        if (fresh != null) {
            val name: String? = fresh.name
            val avatar: String? = fresh.profilePicture
            val freshName = name?.trim().orEmpty()
            val freshAvatar = avatar?.trim().orEmpty()
            if (freshName != cachedName.orEmpty() || freshAvatar != cachedAvatar.orEmpty()) {
                writeProfile(me, freshName, freshAvatar)
            }
        } else if (cachedName == null && cachedAvatar == null) {
            writeProfile(me, null, null) // at least create the doc so presence works
        }
    }

    // name/avatar null = leave what is stored; "" = clear it
    private suspend fun writeProfile(me: String, name: String?, avatar: String?) {
        if (auth.currentUser?.uid != me) return
        val data = hashMapOf<String, Any>(
            FIELD_USERNAME to me,
            FIELD_LAST_ACTIVE to FieldValue.serverTimestamp(),
        )
        // Limits enforced by the security rules (name <= 200 chars, avatar URL <= 2048 chars)
        if (name != null) data[FIELD_NAME] = name.trim().take(MAX_NAME)
        if (avatar != null) data[FIELD_AVATAR] = avatar.trim().takeIf { it.length <= MAX_AVATAR }.orEmpty()
        userRef(me).set(data, SetOptions.merge()).await()
    }

    private fun writePresence(user: String) {
        if (auth.currentUser?.uid != user || currentUsername() != user) return
        // Offline: don't pile up a queued write every minute, one pending heartbeat is enough
        if (presenceWrite?.isComplete == false) return
        presenceWrite = userRef(user)
            .set(
                mapOf(FIELD_USERNAME to user, FIELD_LAST_ACTIVE to FieldValue.serverTimestamp()),
                SetOptions.merge()
            )
            .addOnFailureListener { Log.w(TAG, "Presence update failed", it) }
    }

    override suspend fun registerPushToken(token: String) {
        if (token.isBlank()) return
        val me = currentUsername() ?: return
        // Not signed in yet: the token is registered again right after the next chat sign-in
        if (auth.currentUser?.uid != me) return
        val previous = readStoredToken()?.takeIf { it.owner == me && it.token != token }?.token
        val devices = devicesRef(me)
        val batch = db.batch()
        batch.set(devices, mapOf(FIELD_TOKENS to FieldValue.arrayUnion(token)), SetOptions.merge())
        if (previous != null) batch.update(devices, FIELD_TOKENS, FieldValue.arrayRemove(previous))
        val commit = batch.commit()
        writeStoredToken(StoredToken(owner = me, token = token))
        commit.await()
    }

    override suspend fun signOut() {
        // Runs in the app scope: logout navigation must not cancel it half way
        appScope.async {
            generation.incrementAndGet()
            presenceUser.value = null
            bootstrappedFor.set(null)
            activeConversation = null
            notifications.clearAll()

            val uid = auth.currentUser?.uid
            val storedToken = readStoredToken()
            val tokenOwner = storedToken?.owner
            // Only a chat session (uid == username) has a devices doc; a leftover phone/Google Firebase user doesn't
            val chatUid = uid?.takeIf { it == tokenOwner || it == currentUsername() }
            if (chatUid != null) {
                val stored = storedToken?.takeIf { it.owner == chatUid }?.token
                val token = stored ?: bounded(TOKEN_LOOKUP_TIMEOUT_MS, "Reading the push token") {
                    messaging.token.await()
                }
                if (token != null) {
                    bounded(TOKEN_REMOVE_TIMEOUT_MS, "Removing the push token") {
                        devicesRef(chatUid).set(mapOf(FIELD_TOKENS to FieldValue.arrayRemove(token)), SetOptions.merge())
                            .await()
                    }
                }
            }
            signInMutex.withLock {
                signInJob?.cancel()
                signInJob = null
                signInJobFor = null
                if (uid != null && auth.currentUser?.uid == uid) auth.signOut()
            }
            clearStoredToken()
            _authState.value = ChatAuthState.SigningIn

            // Invalidate this device's token too, so nothing addressed to the old account can still arrive here.
            // A fresh token is created (and registered) after the next login.
            appScope.launch {
                bounded(TOKEN_DELETE_TIMEOUT_MS, "Deleting the push token") { messaging.deleteToken().await() }
            }
            Unit
        }.await()
    }

    private class StoredToken(val owner: String, val token: String)

    private fun readStoredToken(): StoredToken? = synchronized(tokenLock) {
        try {
            if (!tokenFile.exists()) return null
            val lines = tokenFile.readLines()
            val owner = lines.getOrNull(0)?.trim().orEmpty()
            val token = lines.getOrNull(1)?.trim().orEmpty()
            if (owner.isEmpty() || token.isEmpty()) null else StoredToken(owner, token)
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't read the stored push token", e)
            null
        }
    }

    private fun writeStoredToken(value: StoredToken) {
        synchronized(tokenLock) {
            try {
                tokenFile.writeText(value.owner + "\n" + value.token)
            } catch (e: Exception) {
                Log.w(TAG, "Couldn't store the push token", e)
            }
        }
    }

    private fun clearStoredToken() {
        synchronized(tokenLock) {
            try {
                tokenFile.delete() // false (no error) when there is nothing stored
            } catch (e: Exception) {
                Log.w(TAG, "Couldn't clear the stored push token", e)
            }
        }
    }

    private suspend fun <T> bounded(timeoutMs: Long, what: String, block: suspend () -> T): T? = try {
        withTimeoutOrNull(timeoutMs) { block() }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "$what failed", e)
        null
    }

    private fun initialState(): ChatAuthState {
        val me = currentUsername()
        return if (me != null && auth.currentUser?.uid == me) ChatAuthState.Ready else ChatAuthState.SigningIn
    }

    private fun currentUsername(): String? = secureStorage.getUserId()?.trim()?.takeIf { it.isNotEmpty() }

    private fun userRef(username: String): DocumentReference = db.collection(COLLECTION_USERS).document(username)

    private fun devicesRef(username: String): DocumentReference =
        userRef(username).collection(COLLECTION_PRIVATE).document(DOC_DEVICES)

    private companion object {
        const val TAG = "ChatSession"
        const val TOKEN_FILE = "chat_push_token"

        const val COLLECTION_USERS = "users"
        const val COLLECTION_PRIVATE = "private"
        const val DOC_DEVICES = "devices"
        const val FIELD_USERNAME = "username"
        const val FIELD_NAME = "name"
        const val FIELD_AVATAR = "avatar"
        const val FIELD_LAST_ACTIVE = "lastActive"
        const val FIELD_TOKENS = "tokens"

        const val PRESENCE_INTERVAL_MS = 60_000L
        const val MAX_NAME = 200
        const val MAX_AVATAR = 2048
        const val TOKEN_TIMEOUT_MS = 20_000L
        const val TOKEN_LOOKUP_TIMEOUT_MS = 3_000L
        const val TOKEN_REMOVE_TIMEOUT_MS = 5_000L
        const val TOKEN_DELETE_TIMEOUT_MS = 15_000L
        val START_RETRY_DELAYS_MS = longArrayOf(15_000L, 60_000L, 180_000L)
    }
}
