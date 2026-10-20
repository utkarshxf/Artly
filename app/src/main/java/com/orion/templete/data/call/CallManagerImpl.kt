package com.orion.templete.data.call

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.SurfaceView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.orion.templete.data.chat.ChatIds
import com.orion.templete.data.chat.ChatSessionImpl
import com.orion.templete.data.model.call.AudioRoute
import com.orion.templete.data.model.call.CallDirection
import com.orion.templete.data.model.call.CallEndReason
import com.orion.templete.data.model.call.CallKind
import com.orion.templete.data.model.call.CallPeer
import com.orion.templete.data.model.call.CallPhase
import com.orion.templete.data.model.call.CallPush
import com.orion.templete.data.model.call.CallStatus
import com.orion.templete.data.model.call.CallUiState
import com.orion.templete.data.model.call.IncomingCallInvite
import com.orion.templete.domain.call.CallIntents
import com.orion.templete.domain.call.CallManager
import com.orion.templete.domain.call.CallPermissions
import com.orion.templete.util.SecureStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/*
 * The current call (at most one). It turns four inputs into one CallUiState:
 *   - the user's taps (CallManager methods),
 *   - the backend's answers (CallBackend) and the two pushes (ChatMessagingService),
 *   - calls/{callId} in Firestore (CallDocumentWatcher) - the fast path, never the only one,
 *   - the Agora engine's events (CallEngine).
 * Everything here runs on the main thread. A call's timers, listener and pending requests are children of its
 * Session job and die with it; only "tell the backend it is over" requests outlive the call.
 *
 * Without the Firestore listener a call still finishes: REST answers, the pushes, Agora's own events and the
 * local timeouts (ring, connect, reconnect) cover every transition, and while a call rings without a confirmed
 * listener its status is asked from the backend every few seconds.
 */
@Singleton
class CallManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backend: CallBackend,
    private val watcher: CallDocumentWatcher,
    private val engine: CallEngine,
    private val ringer: CallRinger,
    private val device: CallDeviceControls,
    private val notifications: CallNotifications,
    private val chatSession: ChatSessionImpl,
    private val secureStorage: SecureStorage,
) : CallManager {

    private class Session(
        val direction: CallDirection,
        val peer: CallPeer,
        val kind: CallKind,
        var callId: String?, // outgoing: unknown until the backend has created the call
    ) {
        // Parent of everything that must stop with the call
        val job: Job = SupervisorJob()
        var credentials: CallCredentials? = null

        // finish() ran; only the short "ended" display is left
        var over = false

        // accept() was sent and the backend has not answered yet
        var acceptPending = false
        var joinRequested = false
        var remoteJoined = false

        // Agora uid of the other side: caller = 1, callee = 2 (corrected when they actually join)
        var remoteUid = if (direction == CallDirection.OUTGOING) CALLEE_UID else CALLER_UID

        // What the user wants: the speaker, or the earpiece / headset
        var speakerWanted = kind == CallKind.VIDEO

        // calls/{callId} has delivered a snapshot from the server, and the listener has not failed since
        var listenerConfirmed = false

        var ringTimer: Job? = null
        var connectTimer: Job? = null
        var reconnectTimer: Job? = null
        var tokenRefresh: Job? = null
    }

    // A few recent call ids; the oldest is forgotten first
    private class RecentIds(private val capacity: Int) {
        private val ids = LinkedHashSet<String>()

        // True when the id was not known yet
        fun add(id: String): Boolean {
            if (!ids.add(id)) return false
            if (ids.size > capacity) {
                val oldest = ids.iterator()
                oldest.next()
                oldest.remove()
            }
            return true
        }

        operator fun contains(id: String): Boolean = ids.contains(id)
    }

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate + CoroutineExceptionHandler { _, error ->
            Log.e(TAG, "A call task failed", error)
        }
    )
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow(CallUiState())
    override val state: StateFlow<CallUiState> = _state.asStateFlow()

    private val _callsEnabled = MutableStateFlow<Boolean?>(null)
    override val callsEnabled: StateFlow<Boolean?> = _callsEnabled.asStateFlow()

    private var session: Session? = null
    private var idleJob: Job? = null

    // Calls this process knows are over (finished here, or a "call_ended" push arrived): a late or repeated
    // "incoming_call" push for one of them must not ring. Pushes can arrive out of order.
    private val finishedCalls = RecentIds(RECENT_CALLS)

    // Calls that need no (further) missed-call notification: already shown, or answered / declined on this phone
    private val missedSettled = RecentIds(RECENT_CALLS)

    // Agora App ID, from the config or from the first start / accept answer
    private var appId: String? = null
    private var configJob: Job? = null
    private var configCheckedAt: Long? = null

    private var callScreenVisible = false
    private var localView: SurfaceView? = null
    private var remoteView: SurfaceView? = null

    // The service was started for this call (it stops itself when the state is IDLE)
    private var serviceRunning = false

    // The service could not be started: the manager posts the call's notification itself
    private var standaloneNotification = false

    init {
        engine.listener = EngineEvents()
        notifications.ensureChannels()
    }

    // ------------------------------------------------------------------------------------------ configuration

    override fun refreshConfig() = onMain {
        if (_callsEnabled.value == true || configJob?.isActive == true) return@onMain
        val now = SystemClock.elapsedRealtime()
        val checkedAt = configCheckedAt
        // Not enabled (or unknown): ask again at most once a minute
        if (checkedAt != null && now - checkedAt < CONFIG_RECHECK_MS) return@onMain
        configCheckedAt = now
        configJob = scope.launch {
            try {
                when (val reply = backend.config()) {
                    is CallReply.Ok -> {
                        val id = reply.value.appId?.trim().orEmpty()
                        val enabled = reply.value.enabled == true && id.isNotEmpty()
                        if (enabled) rememberAppId(id)
                        // Once enabled it stays enabled for the life of the process
                        if (_callsEnabled.value != true) _callsEnabled.value = enabled
                    }
                    is CallReply.Rejected ->
                        if (reply.code == CallBackend.HTTP_NOT_CONFIGURED && _callsEnabled.value != true) {
                            _callsEnabled.value = false
                        }
                    is CallReply.Unreachable -> Unit // still unknown
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Couldn't load the call configuration", e)
            }
        }
    }

    private fun rememberAppId(id: String) {
        if (appId == null && id.isNotBlank()) appId = id
    }

    // ------------------------------------------------------------------------------------------ outgoing

    override fun startCall(peer: CallPeer, kind: CallKind): Boolean {
        if (!isMainThread()) {
            Log.e(TAG, "startCall must be called on the main thread")
            return false
        }
        return try {
            placeCall(peer, kind)
        } catch (e: Exception) {
            Log.e(TAG, "Couldn't place the call", e)
            false
        }
    }

    private fun placeCall(peer: CallPeer, kind: CallKind): Boolean {
        val me = currentUsername() ?: return false
        val callee = peer.username.trim()
        if (callee.isEmpty() || callee == me) return false
        val previous = session
        if (previous != null) {
            if (!previous.over) return false
            resetToIdle(previous) // the last call's "ended" line is still showing
        }
        if (!CallPermissions.hasMicrophone(context)) Log.w(TAG, "Placing a call without the microphone permission")

        val s = Session(CallDirection.OUTGOING, peer.copy(username = callee), kind, callId = null)
        session = s
        try {
            notifications.cancelMissedCall(callee) // "Call back" answers it
            val camera = kind == CallKind.VIDEO && CallPermissions.hasCamera(context)
            val routes = device.availableRoutes()
            setState(
                CallUiState(
                    phase = CallPhase.OUTGOING_STARTING,
                    direction = CallDirection.OUTGOING,
                    peer = s.peer,
                    conversationId = ChatIds.conversationId(me, callee),
                    kind = kind,
                    localCameraOn = camera,
                    audioRoute = device.expectedRoute(s.speakerWanted, routes),
                    availableRoutes = routes,
                )
            )
            device.setCpuAwake(true)
            device.requestAudioFocus()
            launchService()
            // Local preview while the backend creates the call (needs the App ID, normally known from the config)
            val knownAppId = appId
            if (camera && knownAppId != null && engine.ensureCreated(knownAppId)) applyCamera()
        } catch (e: Exception) {
            Log.e(TAG, "Couldn't set the call up", e)
            finish(s, CallEndReason.FAILED, MSG_START_FAILED)
            return true
        }
        // Not a child of the call: if the user hangs up meanwhile, the answer is still needed to cancel the call
        scope.launch {
            try {
                requestStart(s, callee, kind)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Placing the call failed", e)
                finish(s, CallEndReason.FAILED, MSG_START_FAILED)
            }
        }
        return true
    }

    private suspend fun requestStart(s: Session, callee: String, kind: CallKind) {
        when (val reply = backend.start(callee, kind)) {
            is CallReply.Ok -> {
                val info = reply.value
                if (!isCurrent(s)) {
                    // Hung up while the backend was still creating the call: take it back
                    finishedCalls.add(info.callId)
                    fire("cancel") { backend.cancel(info.callId, timeout = false) }
                    return
                }
                val credentials = info.credentials
                if (credentials == null || !engine.ensureCreated(credentials.appId)) {
                    Log.e(TAG, "The call was created but can't be joined from this phone")
                    s.callId = info.callId
                    fire("cancel") { backend.cancel(info.callId, timeout = false) }
                    finish(s, CallEndReason.FAILED, MSG_START_FAILED)
                    return
                }
                s.callId = info.callId
                s.credentials = credentials
                rememberAppId(credentials.appId)
                onOutgoingRinging(s, info, credentials)
            }
            is CallReply.Rejected -> if (isCurrent(s)) {
                val message = when (reply.code) {
                    CallBackend.HTTP_NOT_CONFIGURED -> MSG_UNAVAILABLE
                    CallBackend.HTTP_NOT_FOUND -> MSG_CANT_BE_CALLED
                    else -> reply.message ?: MSG_START_FAILED
                }
                finish(s, CallEndReason.FAILED, message)
            }
            is CallReply.Unreachable -> if (isCurrent(s)) finish(s, CallEndReason.FAILED, MSG_NO_CONNECTION)
        }
    }

    private fun onOutgoingRinging(s: Session, info: CallSessionInfo, credentials: CallCredentials) {
        update { it.copy(phase = CallPhase.OUTGOING_RINGING, callId = info.callId) }
        applyCamera()
        applyVideoViews(s, force = true)
        engine.preload(credentials)
        ringer.startRingback(loud = _state.value.audioRoute != AudioRoute.EARPIECE)
        watch(s, info.callId)
        probeWhileRinging(s, info.callId)
        val ringSeconds = info.ringTimeoutSec ?: CallPush.DEFAULT_RING_TIMEOUT_SEC
        s.ringTimer = deadline(s, ringSeconds * 1000L) {
            // Nobody picked up: the call becomes "missed" (and the callee's phone is told to stop ringing)
            fire("cancel") { backend.cancel(info.callId, timeout = true) }
            finish(s, CallEndReason.NO_ANSWER)
        }
        // Normally still "ringing"; anything else is already the answer
        info.status?.let { onRemoteStatus(s, it) }
    }

    private fun onOutgoingAccepted(s: Session) {
        val credentials = s.credentials
        if (credentials == null) {
            endBecauseOfFailure(s, MSG_JOIN_FAILED)
            return
        }
        ringer.stop()
        s.ringTimer?.cancel()
        update { it.copy(phase = CallPhase.CONNECTING) }
        join(s, credentials)
    }

    // ------------------------------------------------------------------------------------------ incoming

    override fun onIncomingCall(invite: IncomingCallInvite) = onMain { ring(invite) }

    private fun ring(invite: IncomingCallInvite) {
        val me = currentUsername()
        val callId = invite.callId.trim()
        val caller = invite.caller.username.trim()
        if (me == null || callId.isEmpty() || caller.isEmpty() || caller == me) {
            Log.w(TAG, "Ignoring an incoming call that is not for this account")
            return
        }
        val current = session
        // The same push twice, or a call already known to be over. sentAt is deliberately not looked at: the
        // phone's clock may be wrong, and FCM drops the push itself once the ring time has passed.
        if (current?.callId == callId || callId in finishedCalls) return
        if (current != null && !current.over) {
            // On another call: tell the caller, keep the current call
            finishedCalls.add(callId)
            fire("decline") { backend.decline(callId, busy = true) }
            showMissedCall(callId, invite.caller.copy(username = caller), invite.kind)
            return
        }
        if (current != null) resetToIdle(current)

        val s = Session(CallDirection.INCOMING, invite.caller.copy(username = caller), invite.kind, callId)
        session = s
        try {
            val routes = device.availableRoutes()
            setState(
                CallUiState(
                    phase = CallPhase.INCOMING_RINGING,
                    callId = callId,
                    direction = CallDirection.INCOMING,
                    peer = s.peer,
                    conversationId = ChatIds.conversationId(me, caller),
                    kind = invite.kind,
                    audioRoute = device.expectedRoute(s.speakerWanted, routes),
                    availableRoutes = routes,
                )
            )
            // First of all: the foreground service has to be started inside the high-priority push's window
            launchService()
            device.setCpuAwake(true)
            device.requestAudioFocus()
            ringer.startRinging()
            val ringSeconds = invite.ringTimeoutSec.takeIf { it > 0 } ?: CallPush.DEFAULT_RING_TIMEOUT_SEC
            s.ringTimer = deadline(s, ringSeconds * 1000L + RING_GRACE_MS) {
                // The caller's phone should have given up by now (its push may have been lost)
                showMissedCall(callId, s.peer, s.kind)
                finish(s, CallEndReason.MISSED, linger = false)
            }
            // Its first snapshot is the truth: a call that is no longer ringing stops ringing here at once
            watch(s, callId)
            probeWhileRinging(s, callId)
            // In the background the notification's full-screen intent brings the screen up instead
            if (isAppVisible()) showCallScreen()
        } catch (e: Exception) {
            Log.e(TAG, "Couldn't ring for the incoming call", e)
            finish(s, CallEndReason.FAILED, linger = false)
        }
    }

    override fun onCallEndedPush(
        callId: String,
        reason: String,
        kind: CallKind,
        caller: CallPeer,
        conversationId: String,
    ) = onMain {
        val id = callId.trim()
        if (id.isEmpty()) return@onMain
        val gaveUp = reason == CallPush.REASON_CANCELLED || reason == CallPush.REASON_MISSED
        val s = session?.takeIf { !it.over && it.callId == id }
        if (s == null) {
            // Never rang here (offline, or this push overtook "incoming_call"), or already over on this phone
            finishedCalls.add(id)
            if (gaveUp && caller.username.isNotBlank() && caller.username.trim() != currentUsername()) {
                showMissedCall(id, caller, kind)
            }
            return@onMain
        }
        val ringing = s.direction == CallDirection.INCOMING && _state.value.phase == CallPhase.INCOMING_RINGING
        when {
            ringing && gaveUp -> {
                showMissedCall(id, s.peer, s.kind)
                finish(s, CallEndReason.MISSED)
            }
            // Answered or declined on another device of this account: stop ringing quietly
            ringing -> finish(s, CallEndReason.ANSWERED_ELSEWHERE)
            // The caller gave up at the very moment Accept was tapped here
            gaveUp && s.acceptPending -> {
                showMissedCall(id, s.peer, s.kind)
                finish(s, CallEndReason.MISSED)
            }
            // This phone is in the call itself: the push also goes to the device that answered. Never end an
            // answered call because of it.
            else -> Unit
        }
    }

    override fun accept() = onMain {
        val s = live() ?: return@onMain
        val callId = s.callId
        if (s.direction != CallDirection.INCOMING || _state.value.phase != CallPhase.INCOMING_RINGING || callId == null) {
            return@onMain
        }
        ringer.stop()
        s.ringTimer?.cancel()
        s.acceptPending = true
        if (!CallPermissions.hasMicrophone(context)) Log.w(TAG, "Answering without the microphone permission")
        val camera = s.kind == CallKind.VIDEO && CallPermissions.hasCamera(context)
        // Before the state changes: Android decides when a service is STARTED whether it may use the microphone
        // in the background, and the ring start came from a push. The call screen is in front now.
        launchService()
        update { it.copy(phase = CallPhase.CONNECTING, localCameraOn = camera, frontCamera = true) }
        // Warm the engine up (and start the preview) while the backend answers
        val knownAppId = appId
        if (knownAppId != null && engine.ensureCreated(knownAppId)) {
            applyCamera()
            applyVideoViews(s, force = false)
        }
        scope.launch(s.job) {
            try {
                requestAccept(s, callId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Answering the call failed", e)
                endBecauseOfFailure(s, MSG_JOIN_FAILED)
            }
        }
    }

    private suspend fun requestAccept(s: Session, callId: String) {
        val reply = backend.accept(callId) // short timeout, repeated twice
        if (!isCurrent(s)) return
        s.acceptPending = false
        when (reply) {
            is CallReply.Ok -> {
                val credentials = reply.value.credentials
                if (credentials == null) {
                    endBecauseOfFailure(s, MSG_JOIN_FAILED)
                    return
                }
                missedSettled.add(callId) // answered here: never "missed"
                s.credentials = credentials
                rememberAppId(credentials.appId)
                join(s, credentials)
            }
            is CallReply.Rejected -> when {
                reply.code != CallBackend.HTTP_CONFLICT -> endBecauseOfFailure(s, reply.message ?: MSG_JOIN_FAILED)
                // The call is already over. Handled on another device of this account...
                reply.callStatus == CallStatus.ACCEPTED || reply.callStatus == CallStatus.DECLINED ||
                    reply.callStatus == CallStatus.BUSY || reply.callStatus == CallStatus.ENDED ->
                    finish(s, CallEndReason.ANSWERED_ELSEWHERE)
                // ...or the caller gave up first
                else -> {
                    showMissedCall(callId, s.peer, s.kind)
                    finish(s, CallEndReason.MISSED)
                }
            }
            is CallReply.Unreachable -> endBecauseOfFailure(s, MSG_NO_CONNECTION)
        }
    }

    override fun decline() = onMain { declineRinging() }

    private fun declineRinging() {
        val s = live() ?: return
        if (s.direction != CallDirection.INCOMING || _state.value.phase != CallPhase.INCOMING_RINGING) return
        val callId = s.callId
        if (callId != null) {
            missedSettled.add(callId) // the user saw it and said no
            // 409 "accepted" = answered on another device meanwhile: nothing more to do here either way
            fire("decline") { backend.decline(callId, busy = false) }
        }
        finish(s, CallEndReason.HUNG_UP, linger = false)
    }

    // ------------------------------------------------------------------------------------------ in a call

    override fun hangUp() = onMain {
        val s = live() ?: return@onMain
        val phase = _state.value.phase
        val callId = s.callId
        when {
            phase == CallPhase.INCOMING_RINGING -> {
                declineRinging()
                return@onMain
            }
            // Still being created: requestStart() cancels it as soon as the backend answers
            callId == null -> Unit
            phase == CallPhase.OUTGOING_RINGING -> fire("cancel") { backend.cancel(callId, timeout = false) }
            // Also right while "accept" is still on its way: ending a ringing call declines it
            else -> fire("end") { backend.end(callId) }
        }
        finish(s, CallEndReason.HUNG_UP)
    }

    override fun setMicMuted(muted: Boolean) = onMain {
        live() ?: return@onMain
        update { it.copy(micMuted = muted) }
        engine.setMicMuted(muted) // applied again when the channel is joined
    }

    override fun setCameraEnabled(enabled: Boolean) = onMain {
        val s = live() ?: return@onMain
        val current = _state.value
        // While an incoming call rings the camera is decided by accept()
        if (current.phase == CallPhase.INCOMING_RINGING || current.localCameraOn == enabled) return@onMain
        if (enabled && !CallPermissions.hasCamera(context)) {
            Log.w(TAG, "The camera permission is missing")
            return@onMain
        }
        update { it.copy(localCameraOn = enabled) }
        applyCamera()
        if (enabled) {
            applyVideoViews(s, force = false)
            // Nobody watches video with the phone at the ear
            if (_state.value.audioRoute == AudioRoute.EARPIECE) chooseSpeaker(s, true)
        }
    }

    override fun switchCamera() = onMain {
        live() ?: return@onMain
        if (_state.value.localCameraOn && engine.switchCamera()) update { it.copy(frontCamera = !it.frontCamera) }
    }

    override fun setSpeakerOn(on: Boolean) = onMain {
        val s = live() ?: return@onMain
        chooseSpeaker(s, on)
    }

    private fun chooseSpeaker(s: Session, on: Boolean) {
        s.speakerWanted = on
        val routes = device.availableRoutes()
        // What it should become; Agora reports the route it really chose (a connected headset wins)
        update { it.copy(audioRoute = device.expectedRoute(on, routes), availableRoutes = routes) }
        if (s.joinRequested) {
            engine.setSpeaker(on)
        } else if (_state.value.phase == CallPhase.OUTGOING_RINGING) {
            // Not in the channel yet: the ringback moves, and the choice is applied when joining
            ringer.startRingback(loud = _state.value.audioRoute != AudioRoute.EARPIECE)
        }
    }

    override fun setLocalVideoView(view: SurfaceView?) = onMain {
        // Without a call nothing may hold on to a view of the screen
        if (view != null && live() == null) return@onMain
        if (view === localView) return@onMain
        localView = view
        engine.bindLocalView(view)
    }

    override fun setRemoteVideoView(view: SurfaceView?) = onMain {
        val s = live()
        if (view != null && s == null) return@onMain
        if (view === remoteView) return@onMain
        remoteView = view
        engine.bindRemoteView(view, s?.remoteUid ?: CALLER_UID)
    }

    override fun setCallScreenVisible(visible: Boolean) = onMain {
        callScreenVisible = visible
        val s = live()
        if (visible && s != null) {
            // A new start command while the app is in front lets the service use the microphone (and camera)
            // after the user leaves the screen. A call that only rings in needs neither yet.
            if (_state.value.phase.isInCall || s.direction == CallDirection.OUTGOING) launchService()
        }
        syncProximity(_state.value)
    }

    private fun join(s: Session, credentials: CallCredentials) {
        if (!engine.ensureCreated(credentials.appId)) {
            endBecauseOfFailure(s, MSG_JOIN_FAILED)
            return
        }
        applyCamera() // before joining, so the camera is published from the first moment
        s.joinRequested = engine.join(credentials, speakerByDefault = s.speakerWanted, micMuted = _state.value.micMuted)
        if (!s.joinRequested) {
            endBecauseOfFailure(s, MSG_JOIN_FAILED)
            return
        }
        applyVideoViews(s, force = true)
        // The other side has to show up; a call that never connects must not hang in "Connecting…"
        s.connectTimer = deadline(s, CONNECT_TIMEOUT_MS) {
            if (!s.remoteJoined) endBecauseOfFailure(s, MSG_NO_CONNECTION)
        }
    }

    // The call can't go on from this phone: make sure the backend ends it too (ending a call that still rings
    // cancels / declines it)
    private fun endBecauseOfFailure(s: Session, message: String) {
        if (!isCurrent(s)) return
        val callId = s.callId
        if (callId != null) fire("end") { backend.end(callId) }
        finish(s, CallEndReason.FAILED, message)
    }

    private inner class EngineEvents : CallEngine.Listener {
        private fun inChannel(): Session? = session?.takeIf { !it.over && it.joinRequested }

        override fun onJoined() {
            val s = inChannel() ?: return
            applyVideoViews(s, force = true)
            engine.setMicMuted(_state.value.micMuted)
        }

        override fun onRemoteJoined(uid: Int) {
            val s = inChannel() ?: return
            s.remoteUid = uid
            s.remoteJoined = true
            s.connectTimer?.cancel()
            s.reconnectTimer?.cancel()
            s.reconnectTimer = null
            update {
                it.copy(
                    phase = CallPhase.CONNECTED,
                    remoteJoined = true,
                    connectedAtElapsed = it.connectedAtElapsed ?: SystemClock.elapsedRealtime(),
                )
            }
            engine.bindRemoteView(remoteView, uid, force = true)
        }

        override fun onRemoteLeft(uid: Int, dropped: Boolean) {
            val s = inChannel() ?: return
            if (!s.remoteJoined || uid != s.remoteUid) return
            // They hung up (the call document says the same a moment later) or their phone vanished. Either way
            // the call is over: make sure the backend knows.
            val callId = s.callId
            if (callId != null) fire("end") { backend.end(callId) }
            finish(s, if (dropped) CallEndReason.CONNECTION_LOST else CallEndReason.REMOTE_HUNG_UP)
        }

        override fun onConnection(event: EngineConnection) {
            val s = inChannel() ?: return
            when (event) {
                EngineConnection.RECONNECTING -> {
                    if (_state.value.phase == CallPhase.CONNECTED) update { it.copy(phase = CallPhase.RECONNECTING) }
                    if (s.reconnectTimer?.isActive != true) {
                        s.reconnectTimer = deadline(s, RECONNECT_TIMEOUT_MS) { connectionGone(s) }
                    }
                }
                EngineConnection.CONNECTED -> {
                    s.reconnectTimer?.cancel()
                    s.reconnectTimer = null
                    if (_state.value.phase == CallPhase.RECONNECTING) update { it.copy(phase = CallPhase.CONNECTED) }
                }
                EngineConnection.FAILED -> connectionGone(s)
                // This account joined the same call from another device; that call goes on, so don't end it
                EngineConnection.REPLACED -> finish(s, CallEndReason.ANSWERED_ELSEWHERE)
            }
        }

        private fun connectionGone(s: Session) {
            if (!isCurrent(s)) return
            if (s.remoteJoined) {
                val callId = s.callId
                if (callId != null) fire("end") { backend.end(callId) }
                finish(s, CallEndReason.CONNECTION_LOST)
            } else {
                endBecauseOfFailure(s, MSG_NO_CONNECTION)
            }
        }

        override fun onTokenExpiring() {
            val s = inChannel() ?: return
            val callId = s.callId ?: return
            if (s.tokenRefresh?.isActive == true) return
            s.tokenRefresh = scope.launch(s.job) {
                try {
                    refreshToken(s, callId)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Couldn't renew the call token", e)
                }
            }
        }

        override fun onRemoteMicMuted(uid: Int, muted: Boolean) {
            val s = inChannel() ?: return
            if (uid == s.remoteUid) update { it.copy(remoteMicMuted = muted) }
        }

        override fun onRemoteCamera(uid: Int, on: Boolean) {
            val s = inChannel() ?: return
            if (uid == s.remoteUid) update { it.copy(remoteCameraOn = on) }
        }

        override fun onWeakNetwork(weak: Boolean) {
            inChannel() ?: return
            update { it.copy(weakNetwork = weak) }
        }

        override fun onAudioRoute(route: AudioRoute) {
            live() ?: return
            // The route Agora really uses; the screen shows this one
            update { it.copy(audioRoute = route, availableRoutes = device.availableRoutes() + route) }
        }

        override fun onCameraFailed() {
            live() ?: return
            if (!_state.value.localCameraOn) return
            Log.w(TAG, "The camera could not be opened; going on without it")
            update { it.copy(localCameraOn = false) }
            applyCamera()
        }
    }

    private suspend fun refreshToken(s: Session, callId: String) {
        repeat(TOKEN_ATTEMPTS) { attempt ->
            when (val reply = backend.token(callId)) {
                is CallReply.Ok -> {
                    val fresh = reply.value.credentials
                    if (fresh != null && isCurrent(s)) {
                        s.credentials = fresh
                        engine.renewToken(fresh.token)
                    }
                    return
                }
                is CallReply.Rejected -> {
                    // No token for a call that is over
                    val status = reply.callStatus
                    if (status != null && isCurrent(s)) onRemoteStatus(s, status)
                    return
                }
                is CallReply.Unreachable -> if (attempt < TOKEN_ATTEMPTS - 1) delay(TOKEN_RETRY_MS)
            }
        }
    }

    // ------------------------------------------------------------------------------------------ call status

    private fun watch(s: Session, callId: String) {
        scope.launch(s.job) {
            try {
                watcher.watch(
                    callId = callId,
                    onFailure = { s.listenerConfirmed = false },
                    onStatus = { status, fromServer ->
                        if (fromServer) s.listenerConfirmed = true
                        if (isCurrent(s)) onRemoteStatus(s, status)
                    },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                s.listenerConfirmed = false
                Log.w(TAG, "The call listener stopped", e)
            }
        }
    }

    // Safety net for a call that rings while the Firestore listener is not (yet) confirmed: without it the caller
    // would never learn that the call was answered. The token endpoint answers with the call's status (or 409 +
    // callStatus) and changes nothing.
    private fun probeWhileRinging(s: Session, callId: String) {
        scope.launch(s.job) {
            try {
                delay(PROBE_FIRST_DELAY_MS) // a working listener has answered long before
                while (isCurrent(s) && _state.value.phase.isRinging) {
                    if (!s.listenerConfirmed) {
                        val status = when (val reply = backend.token(callId)) {
                            is CallReply.Ok -> reply.value.status
                            is CallReply.Rejected -> reply.callStatus
                            is CallReply.Unreachable -> null
                        }
                        if (status != null && isCurrent(s)) onRemoteStatus(s, status)
                    }
                    delay(PROBE_INTERVAL_MS)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "The call status check failed", e)
            }
        }
    }

    // calls/{callId}.status as seen by the listener or the status check. A status only moves forward
    // (ringing -> accepted -> ended, or ringing -> declined / cancelled / missed / busy).
    private fun onRemoteStatus(s: Session, status: CallStatus) {
        if (!isCurrent(s)) return
        val phase = _state.value.phase
        when (s.direction) {
            CallDirection.OUTGOING -> when (status) {
                CallStatus.RINGING -> Unit
                CallStatus.ACCEPTED -> if (phase == CallPhase.OUTGOING_RINGING) onOutgoingAccepted(s)
                CallStatus.DECLINED -> if (phase == CallPhase.OUTGOING_RINGING) finish(s, CallEndReason.DECLINED)
                CallStatus.BUSY -> if (phase == CallPhase.OUTGOING_RINGING) finish(s, CallEndReason.BUSY)
                CallStatus.MISSED, CallStatus.CANCELLED ->
                    if (phase == CallPhase.OUTGOING_RINGING) finish(s, CallEndReason.NO_ANSWER)
                CallStatus.ENDED -> finish(s, CallEndReason.REMOTE_HUNG_UP)
            }
            CallDirection.INCOMING -> when (status) {
                CallStatus.RINGING -> Unit
                // While ringing: another device of this account took the call. After accept() it is this phone's
                // own answer coming back.
                CallStatus.ACCEPTED, CallStatus.DECLINED, CallStatus.BUSY ->
                    if (phase == CallPhase.INCOMING_RINGING) finish(s, CallEndReason.ANSWERED_ELSEWHERE)
                CallStatus.CANCELLED, CallStatus.MISSED ->
                    if (phase == CallPhase.INCOMING_RINGING || s.acceptPending) {
                        s.callId?.let { showMissedCall(it, s.peer, s.kind) }
                        finish(s, CallEndReason.MISSED)
                    }
                CallStatus.ENDED -> finish(
                    s,
                    if (phase == CallPhase.INCOMING_RINGING) CallEndReason.ANSWERED_ELSEWHERE
                    else CallEndReason.REMOTE_HUNG_UP
                )
            }
        }
    }

    // ------------------------------------------------------------------------------------------ ending

    // The one way a call ends on this phone. linger = show why for a moment (ENDED) before going back to IDLE.
    private fun finish(s: Session, reason: CallEndReason, message: String? = null, linger: Boolean = true) {
        if (session !== s || s.over) return
        s.over = true
        s.job.cancel()
        s.callId?.let { finishedCalls.add(it) }
        releaseCallResources()
        if (!linger) {
            resetToIdle(s)
            return
        }
        idleJob?.cancel()
        idleJob = scope.launch {
            try {
                delay(ENDED_LINGER_MS)
                resetToIdle(s)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Couldn't reset the call state", e)
            }
        }
        // Published last: whoever observes the state may call back into the manager right away
        setState(
            _state.value.copy(
                phase = CallPhase.ENDED,
                endReason = reason,
                message = message,
                micMuted = false,
                localCameraOn = false,
                remoteCameraOn = false,
                remoteMicMuted = false,
                weakNetwork = false,
            )
        )
    }

    // Each step on its own: whatever fails, nothing may keep ringing, recording or holding the screen off
    private fun releaseCallResources() {
        safely("ringer") { ringer.stop() }
        safely("video views") {
            localView = null
            remoteView = null
        }
        safely("engine") { engine.leave() }
        safely("audio focus") { device.abandonAudioFocus() }
        safely("proximity lock") { device.setProximityScreenOff(false) }
        safely("wake lock") { device.setCpuAwake(false) }
    }

    private fun resetToIdle(s: Session) {
        if (session !== s) return
        session = null
        val pending = idleJob
        idleJob = null
        serviceRunning = false
        if (standaloneNotification) {
            standaloneNotification = false
            notifications.cancelStandalone()
        }
        // May be the coroutine running this very function; cancelling only matters at its next suspension point
        pending?.cancel()
        // Published last: the service stops itself when it sees IDLE, and the screen may place the next call
        // right away
        setState(CallUiState())
    }

    // Tells the backend that a call is over. Outlives the call, is repeated by CallBackend, and its answer changes
    // nothing here: the backend treats a repeated or late decline / cancel / end as done.
    private fun fire(what: String, request: suspend () -> CallReply<Unit>) {
        scope.launch {
            try {
                when (val reply = request()) {
                    is CallReply.Ok -> Unit
                    is CallReply.Rejected -> Log.w(TAG, "Call $what answered HTTP ${reply.code}")
                    is CallReply.Unreachable -> Log.w(TAG, "Call $what did not reach the server")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Call $what failed", e)
            }
        }
    }

    // Once per call, and not while the chat with the caller is open on screen (the call shows up there as a row)
    private fun showMissedCall(callId: String, caller: CallPeer, kind: CallKind) {
        if (!missedSettled.add(callId)) return
        val me = currentUsername() ?: return
        val username = caller.username.trim()
        if (username.isEmpty() || username == me) return
        try {
            if (chatSession.isConversationVisible(ChatIds.conversationId(me, username))) return
            val peer = caller.copy(username = username)
            // Posted at once (the process may be stopped right after a push); the photo follows
            notifications.showMissedCall(peer, kind)
            scope.launch {
                try {
                    notifications.attachMissedCallAvatar(peer, kind)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Couldn't load the caller's photo", e)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't show the missed call", e)
        }
    }

    // ------------------------------------------------------------------------------------------ platform

    // Never stopService(): the service stops itself on IDLE (a stop that overtakes its startForeground() crashes
    // the app). Every start is a start command; the service decides its foreground types from the state.
    private fun launchService() {
        try {
            ContextCompat.startForegroundService(context, Intent(context, CallService::class.java))
            serviceRunning = true
            standaloneNotification = false // the service's notification has the same id and replaces it
        } catch (e: Exception) {
            // SecurityException, or ForegroundServiceStartNotAllowedException when Android refuses a start from
            // the background. The call goes on; it is just easier for the system to cut off.
            Log.w(TAG, "The call service could not be started", e)
            if (!serviceRunning) {
                standaloneNotification = true
                notifications.postStandalone(_state.value)
            }
        }
    }

    private fun showCallScreen() {
        try {
            context.startActivity(CallIntents.show(context))
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't open the call screen", e)
        }
    }

    private fun isAppVisible(): Boolean = try {
        ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    } catch (e: Exception) {
        false
    }

    // Makes the engine's camera match the state (nothing happens until the engine exists)
    private fun applyCamera() {
        val current = _state.value
        engine.setCamera(current.localCameraOn, current.frontCamera)
    }

    // The screen's SurfaceViews; set again after joining and when the other side appears
    private fun applyVideoViews(s: Session, force: Boolean) {
        engine.bindLocalView(localView, force)
        engine.bindRemoteView(remoteView, s.remoteUid, force)
    }

    // Screen off at the ear: only while the call screen is showing, the sound is on the earpiece and there is
    // no video to look at
    private fun syncProximity(current: CallUiState) {
        val atTheEar = callScreenVisible && live() != null &&
            current.audioRoute == AudioRoute.EARPIECE && !current.showsVideo &&
            (current.phase.isInCall || current.phase == CallPhase.OUTGOING_STARTING ||
                current.phase == CallPhase.OUTGOING_RINGING)
        device.setProximityScreenOff(atTheEar)
    }

    private fun setState(next: CallUiState) {
        _state.value = next
        try {
            syncProximity(next)
            if (standaloneNotification) notifications.postStandalone(next)
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't follow the call state", e)
        }
    }

    private inline fun update(change: (CallUiState) -> CallUiState) {
        val current = _state.value
        val next = change(current)
        if (next != current) setState(next)
    }

    private fun live(): Session? = session?.takeIf { !it.over }

    private fun isCurrent(s: Session): Boolean = session === s && !s.over

    // A timeout that counts real time (SystemClock.elapsedRealtime), also across a doze of the phone
    private fun deadline(s: Session, afterMs: Long, action: () -> Unit): Job = scope.launch(s.job) {
        try {
            val end = SystemClock.elapsedRealtime() + afterMs
            while (true) {
                val left = end - SystemClock.elapsedRealtime()
                if (left <= 0L) break
                delay(minOf(left, TIMER_TICK_MS))
            }
            if (isCurrent(s)) action()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "A call timeout could not be handled", e)
        }
    }

    private fun currentUsername(): String? = secureStorage.getUserId()?.trim()?.takeIf { it.isNotEmpty() }

    private fun isMainThread(): Boolean = Looper.myLooper() == Looper.getMainLooper()

    private inline fun safely(what: String, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            Log.e(TAG, "Call cleanup step failed: $what", e)
        }
    }

    // CallManager is main-thread and never throws; a call from another thread is moved over instead of racing
    private fun onMain(block: () -> Unit) {
        val guarded = Runnable {
            try {
                block()
            } catch (e: Exception) {
                Log.e(TAG, "A call action failed", e)
            }
        }
        if (isMainThread()) guarded.run() else mainHandler.post(guarded)
    }

    private companion object {
        const val TAG = "CallManager"

        // One channel per call; the backend's tokens are bound to these uids
        const val CALLER_UID = 1
        const val CALLEE_UID = 2

        const val MSG_UNAVAILABLE = "Calls aren't available right now."
        const val MSG_CANT_BE_CALLED = "This person can't be called right now."
        const val MSG_NO_CONNECTION = "Couldn't connect. Check your internet and try again."
        const val MSG_START_FAILED = "Couldn't start the call."
        const val MSG_JOIN_FAILED = "Couldn't connect the call."

        const val ENDED_LINGER_MS = 1_500L
        const val RING_GRACE_MS = 5_000L
        const val CONNECT_TIMEOUT_MS = 30_000L
        const val RECONNECT_TIMEOUT_MS = 30_000L
        const val PROBE_FIRST_DELAY_MS = 4_000L
        const val PROBE_INTERVAL_MS = 3_000L
        const val CONFIG_RECHECK_MS = 60_000L
        const val TOKEN_ATTEMPTS = 3
        const val TOKEN_RETRY_MS = 3_000L
        const val TIMER_TICK_MS = 1_000L
        const val RECENT_CALLS = 64
    }
}
