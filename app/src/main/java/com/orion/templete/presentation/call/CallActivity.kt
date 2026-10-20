package com.orion.templete.presentation.call

import android.Manifest
import android.app.KeyguardManager
import android.app.PictureInPictureParams
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.util.Rational
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.orion.templete.data.model.call.AudioRoute
import com.orion.templete.data.model.call.CallKind
import com.orion.templete.data.model.call.CallPeer
import com.orion.templete.data.model.call.CallPhase
import com.orion.templete.data.model.call.CallUiState
import com.orion.templete.domain.call.CallIntents
import com.orion.templete.domain.call.CallManager
import com.orion.templete.domain.call.CallPermissions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.lang.ref.WeakReference
import javax.inject.Inject

/**
 * The call screen: incoming ring, outgoing ring, audio call, video call and the short "call ended" moment.
 *
 * It only renders [CallManager.state] and forwards taps. The call itself lives in the manager and outlives this
 * activity: back, the minimise chevron and picture-in-picture leave the screen, they never hang up. The one thing
 * the screen owns is placing a call ([CallIntents.ACTION_CALL]): the permission prompts and `startCall`.
 */
@AndroidEntryPoint
class CallActivity : ComponentActivity() {

    @Inject
    lateinit var callManager: CallManager

    // What the launching intent asked for. Run in onResume: the keyguard and permission prompts need a resumed
    // activity, and an intent can arrive before the activity is on screen.
    private sealed interface Command {
        data object Answer : Command
        data class Place(val peer: CallPeer, val kind: CallKind) : Command
    }

    // Why the system permission dialog is open
    private sealed interface PermissionPurpose {
        data class Place(val peer: CallPeer, val kind: CallKind) : PermissionPurpose
        data object Answer : PermissionPurpose
        data object Camera : PermissionPurpose
    }

    private data class Placement(val peer: CallPeer, val kind: CallKind)

    private var command: Command? = null

    // The call being placed while the keyguard / permission prompts are up. It does not exist in the manager yet
    // (the state is still IDLE), so this is what keeps the screen open and what it shows.
    private var placing by mutableStateOf<Placement?>(null)

    // startCall() succeeded; `placing` is dropped as soon as the manager's state leaves IDLE
    private var placingStarted = false

    private var permissionPurpose: PermissionPurpose? = null
    private var permissionAskedAt = 0L
    private var rationaleBeforeAsking: Map<String, Boolean> = emptyMap()
    private var unlocking = false

    private var notice by mutableStateOf<CallPermissionNotice?>(null)
    private var lockScreenHint by mutableStateOf(false)
    private var inPictureInPicture by mutableStateOf(false)

    // Last auto-enter value handed to the system (null = nothing sent yet)
    private var autoPictureInPicture: Boolean? = null

    // The next onUserLeaveHint comes from a prompt this screen opened itself, not from the user leaving the call
    private var skipLeaveHint = false

    private val prefs: SharedPreferences by lazy {
        applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
        object : ActivityResultCallback<Map<String, Boolean>> {
            // The map is not used: what counts is what the app holds now (it is empty when the dialog was interrupted)
            override fun onActivityResult(result: Map<String, Boolean>) = onPermissionsAnswered()
        },
    )

    private val pictureInPictureListener = object : Consumer<PictureInPictureModeChangedInfo> {
        override fun accept(value: PictureInPictureModeChangedInfo) {
            inPictureInPicture = value.isInPictureInPictureMode
        }
    }

    private val actions = CallActions(
        onAccept = { answer() },
        onDecline = { callManager.decline() },
        onHangUp = { callManager.hangUp() },
        onToggleMute = { callManager.setMicMuted(!callManager.state.value.micMuted) },
        // The button shows the real route; a tap moves to the speaker, or back to the earpiece / headset
        onToggleSpeaker = { callManager.setSpeakerOn(callManager.state.value.audioRoute != AudioRoute.SPEAKER) },
        onToggleCamera = { toggleCamera() },
        onSwitchCamera = { callManager.switchCamera() },
        onMinimise = { leaveScreen() },
    )

    private val lockScreenHintActions = CallLockScreenHintActions(
        onAllow = { openFullScreenIntentSettings() },
        onDismiss = { dismissLockScreenHint() },
        onSeen = { rememberLockScreenHintOffered() },
    )

    // ------------------------------------------------------------------------------------------------ lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prepareWindow()
        claimScreen()

        inPictureInPicture = isInPictureInPictureMode
        addOnPictureInPictureModeChangedListener(pictureInPictureListener)
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = leaveScreen()
            },
        )

        // Only a fresh launch carries a request. After a rotation or process death the manager's state is all
        // there is: an active call is shown, anything else closes the screen.
        if (savedInstanceState == null) {
            readIntent(intent)
            if (launchedFromHistory(intent) && callManager.state.value.phase == CallPhase.IDLE) {
                // Reopened from Recents long after the call: land in the app instead of on an empty call screen
                openApp()
            }
        }

        // Deliberately not gated on STARTED: the screen also has to close while it is in the background or in
        // picture-in-picture, the moment the call is over.
        lifecycleScope.launch {
            callManager.state.collect { state -> onCallState(state) }
        }
        if (isFinishing) return

        setContent {
            CallTheme {
                val state by callManager.state.collectAsStateWithLifecycle()
                CallScreen(
                    state = state,
                    placingPeer = placing?.peer,
                    inPictureInPicture = inPictureInPicture,
                    callManager = callManager,
                    actions = actions,
                    lockScreenHint = if (lockScreenHint) lockScreenHintActions else null,
                )
                notice?.let { current ->
                    CallPermissionNoticeDialog(
                        notice = current,
                        onRetry = { retryAfterNotice() },
                        onOpenSettings = { openSettingsFromNotice() },
                        onDismiss = { dismissNotice() },
                    )
                }
            }
        }
    }

    // singleTop: "Answer" on the notification, "Call back", or "return to call" while this screen is on top
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        CallScreenPresence.onScreenStarted()
        callManager.setCallScreenVisible(true)
    }

    override fun onResume() {
        super.onResume()
        skipLeaveHint = false
        val next = command ?: return
        command = null
        when (next) {
            Command.Answer -> answer()
            is Command.Place -> place(next.peer, next.kind)
        }
    }

    override fun onStop() {
        super.onStop()
        // A newer copy of the screen may already be showing the call
        if (CallScreenPresence.onScreenStopped()) callManager.setCallScreenVisible(false)
    }

    override fun onDestroy() {
        if (current?.get() === this) current = null
        super.onDestroy()
    }

    // Home during a video call: keep watching in a floating window. Android 12+ does this by itself (auto-enter,
    // see syncPictureInPicture); before that it has to be asked for here.
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (skipLeaveHint) {
            skipLeaveHint = false
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) enterPictureInPicture()
    }

    // ------------------------------------------------------------------------------------------------ window

    private fun prepareWindow() {
        // Call screens are always dark; the app theme's light window background would flash before the first frame
        window.setBackgroundDrawable(ColorDrawable(Color.BLACK))
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        // An incoming call comes up over the lock screen and wakes the display (the manifest says the same)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        // The display stays on for the whole call; at the ear the manager's proximity lock switches it off
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        try {
            // The layouts (and the 9:16 picture-in-picture window) are portrait, like every phone call screen
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Couldn't lock the orientation", e)
        }
    }

    // A second copy of this screen can be created when it is asked for while the first one is buried under another
    // activity or sits in picture-in-picture. Only one may render the call (and own the video views): the newest.
    private fun claimScreen() {
        val previous = current?.get()
        if (previous != null && previous !== this && !previous.isFinishing && !previous.isDestroyed) {
            previous.placing = null
            previous.closeScreen()
        }
        current = WeakReference(this)
    }

    // ------------------------------------------------------------------------------------------------ state

    private fun onCallState(state: CallUiState) {
        if (placingStarted && state.phase != CallPhase.IDLE) {
            // The manager has the call now; from here on its state decides when the screen closes
            placing = null
            placingStarted = false
        }
        if (state.phase == CallPhase.IDLE && placing == null) {
            closeScreen()
            return
        }
        // The offer belongs to the wait while the other phone rings; an answered call keeps its screen clean
        if (state.phase.isInCall || state.phase == CallPhase.ENDED) lockScreenHint = false
        syncPictureInPicture(state)
    }

    private fun closeScreen() {
        if (isFinishing) return
        // Auto-enter has to be off first, or a finishing video call can leave an empty floating window behind
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && autoPictureInPicture == true) {
            autoPictureInPicture = false
            try {
                setPictureInPictureParams(CallPictureInPicture.params(autoEnter = false))
            } catch (e: RuntimeException) {
                Log.w(TAG, "Couldn't update picture-in-picture", e)
            }
        }
        // Opened from a notification with the app closed, this screen is its whole task: once the call is over, an
        // empty Artistry card must not stay in Recents (it would reopen the call screen). Root + resumed means
        // nothing sits above it, so only this screen goes. With the app underneath, or while the call goes on
        // (minimised: the Recents card then leads back to it), a plain finish is right.
        val callOver = callManager.state.value.phase == CallPhase.IDLE
        val alone = isTaskRoot && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        if (callOver && alone) finishAndRemoveTask() else finish()
    }

    // Back and the minimise chevron: leave the screen, keep the call
    private fun leaveScreen() {
        if (placing != null && callManager.state.value.phase == CallPhase.IDLE) {
            // Nothing has been placed yet: leaving gives the call up
            dropPlacement()
            return
        }
        // Video keeps playing in a floating window; an audio call needs no window (the green bar in the app and
        // the ongoing-call notification lead back here)
        if (!enterPictureInPicture()) closeScreen()
    }

    // ------------------------------------------------------------------------------------------------ intents

    private fun readIntent(intent: Intent?) {
        if (intent == null || launchedFromHistory(intent)) return
        when (intent.action) {
            CallIntents.ACTION_ANSWER -> command = Command.Answer
            CallIntents.ACTION_CALL -> {
                val username = intent.getStringExtra(CallIntents.EXTRA_PEER_USERNAME)?.trim()
                if (username.isNullOrEmpty()) return
                val peer = CallPeer(
                    username = username,
                    name = intent.getStringExtra(CallIntents.EXTRA_PEER_NAME)?.takeIf { it.isNotBlank() },
                    avatar = intent.getStringExtra(CallIntents.EXTRA_PEER_AVATAR)?.takeIf { it.isNotBlank() },
                )
                val kind = if (intent.getBooleanExtra(CallIntents.EXTRA_VIDEO, false)) CallKind.VIDEO else CallKind.AUDIO
                // Held from this moment on: with nothing in `placing` the IDLE state would close the screen
                // before onResume gets to place the call
                placing = Placement(peer, kind)
                placingStarted = false
                command = Command.Place(peer, kind)
            }
            // ACTION_SHOW (and anything unknown): the manager's state is rendered as it is
        }
    }

    // Relaunched from Recents with the intent of the first launch: that request was handled back then
    private fun launchedFromHistory(intent: Intent?): Boolean =
        intent != null && (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0

    private fun openApp() {
        val launch = packageManager.getLaunchIntentForPackage(packageName) ?: return
        try {
            startActivity(launch)
        } catch (e: RuntimeException) {
            Log.w(TAG, "Couldn't open the app", e)
        }
    }

    // ------------------------------------------------------------------------------------------------ placing a call

    private fun place(peer: CallPeer, kind: CallKind) {
        val state = callManager.state.value
        when {
            state.phase.isActive -> {
                // One call at a time: this screen is already showing it
                if (state.peer?.username != peer.username) toast("You're already on a call")
                dropPlacement()
            }
            state.phase == CallPhase.ENDED -> placeOnceIdle(peer, kind)
            else -> whenUnlocked(
                // Placing a call (and the permission dialog for it) is for the phone's owner
                onUnlocked = { askThenStart(peer, kind) },
                onRefused = { dropPlacement() },
            )
        }
    }

    // The previous call is still on its "call ended" moment; the manager only takes a new call from IDLE
    private fun placeOnceIdle(peer: CallPeer, kind: CallKind) {
        lifecycleScope.launch {
            val idle = withTimeoutOrNull(ENDED_WAIT_MS) {
                callManager.state.first { it.phase != CallPhase.ENDED }
            }
            if (placing?.peer != peer) return@launch
            if (idle == null || !lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                dropPlacement()
            } else {
                place(peer, kind)
            }
        }
    }

    private fun askThenStart(peer: CallPeer, kind: CallKind) {
        if (placing?.peer != peer) return // given up while the keyguard was up
        val required = CallPermissions.required(kind).filterNot(::isGranted)
        val optional = CallPermissions.optional().filterNot(::isGranted)
        val ask = when {
            required.isNotEmpty() -> required + optional
            // Bluetooth headsets: asked along with the others, and on its own only before the very first call
            optional.isNotEmpty() && !prefs.getBoolean(KEY_OPTIONAL_ASKED, false) -> optional
            else -> emptyList()
        }
        if (ask.isEmpty()) startPlacedCall(peer, kind) else launchPermissions(PermissionPurpose.Place(peer, kind), ask)
    }

    private fun startPlacedCall(peer: CallPeer, kind: CallKind) {
        if (!callManager.startCall(peer, kind)) {
            val busy = callManager.state.value.phase.isActive
            toast(if (busy) "You're already on a call" else "Couldn't start the call. Please try again.")
            dropPlacement()
            return
        }
        offerLockScreenHint()
        if (callManager.state.value.phase != CallPhase.IDLE) {
            placing = null
            placingStarted = false
            return
        }
        // The manager accepted the call but has not published it yet: hold the screen open a little longer
        placingStarted = true
        lifecycleScope.launch {
            delay(START_GRACE_MS)
            if (placingStarted && callManager.state.value.phase == CallPhase.IDLE) dropPlacement()
        }
    }

    private fun dropPlacement() {
        placing = null
        placingStarted = false
        notice = null
        if (callManager.state.value.phase == CallPhase.IDLE) closeScreen()
    }

    // ------------------------------------------------------------------------------------------------ answering

    // "Answer" on the notification and the Accept button
    private fun answer() {
        val state = callManager.state.value
        if (state.phase != CallPhase.INCOMING_RINGING || permissionPurpose != null) return
        val required = CallPermissions.required(state.kind).filterNot(::isGranted)
        if (required.isEmpty()) {
            // Nothing to ask for: answered on the spot, also over the lock screen
            callManager.accept()
            return
        }
        val ask = required + CallPermissions.optional().filterNot(::isGranted)
        // The system dialog can't be shown over the keyguard
        whenUnlocked(
            onUnlocked = { if (isRinging()) launchPermissions(PermissionPurpose.Answer, ask) },
            onRefused = {
                if (isRinging()) {
                    // Still locked. A microphone that is already granted is enough to take the call without video
                    if (CallPermissions.hasMicrophone(this)) {
                        callManager.accept()
                    } else {
                        toast("Unlock your phone to answer this call")
                    }
                }
            },
        )
    }

    private fun isRinging(): Boolean = callManager.state.value.phase == CallPhase.INCOMING_RINGING

    private fun toggleCamera() {
        val state = callManager.state.value
        when {
            state.localCameraOn -> callManager.setCameraEnabled(false)
            CallPermissions.hasCamera(this) -> callManager.setCameraEnabled(true)
            permissionPurpose != null -> Unit
            else -> whenUnlocked(
                onUnlocked = { launchPermissions(PermissionPurpose.Camera, listOf(Manifest.permission.CAMERA)) },
                onRefused = { },
            )
        }
    }

    // ------------------------------------------------------------------------------------------------ permissions

    private fun isGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun launchPermissions(purpose: PermissionPurpose, permissions: List<String>) {
        permissionPurpose = purpose
        permissionAskedAt = SystemClock.elapsedRealtime()
        rationaleBeforeAsking = permissions.associateWith { shouldShowRequestPermissionRationale(it) }
        val optional = CallPermissions.optional()
        if (permissions.any { it in optional }) prefs.edit().putBoolean(KEY_OPTIONAL_ASKED, true).apply()
        // The dialog is a prompt of this screen, not the user leaving a video call
        skipLeaveHint = true
        try {
            permissionLauncher.launch(permissions.toTypedArray())
        } catch (e: ActivityNotFoundException) {
            // No permission UI on this device: nothing can be granted, carry on with what the app holds
            Log.w(TAG, "Couldn't ask for the call permissions", e)
            onPermissionsAnswered()
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Couldn't ask for the call permissions", e)
            onPermissionsAnswered()
        }
    }

    private fun onPermissionsAnswered() {
        val purpose = permissionPurpose ?: return
        permissionPurpose = null
        val microphone = CallPermissions.hasMicrophone(this)
        when (purpose) {
            is PermissionPurpose.Place -> when {
                placing?.peer != purpose.peer -> Unit // given up meanwhile
                // Without the camera a video call is still placed; it starts with the camera off
                microphone -> startPlacedCall(purpose.peer, purpose.kind)
                else -> notice = CallPermissionNotice(
                    camera = false,
                    permanent = isPermanentlyDenied(Manifest.permission.RECORD_AUDIO),
                    peerName = purpose.peer.displayName,
                )
            }
            PermissionPurpose.Answer -> when {
                !isRinging() -> Unit // the caller gave up while the dialog was open
                microphone -> callManager.accept() // a denied camera only means answering without video
                else -> {
                    toast("Microphone access is needed to answer calls")
                    callManager.decline()
                }
            }
            PermissionPurpose.Camera -> when {
                !callManager.state.value.phase.isActive -> Unit
                CallPermissions.hasCamera(this) -> callManager.setCameraEnabled(true)
                isPermanentlyDenied(Manifest.permission.CAMERA) ->
                    notice = CallPermissionNotice(camera = true, permanent = true, peerName = null)
                else -> toast("Camera access is needed to turn on video")
            }
        }
    }

    // "Don't ask again" (or a second "Don't allow"): the dialog no longer appears, only Settings can change it.
    // A dialog that was merely dismissed looks the same to shouldShowRequestPermissionRationale, so it also has to
    // have been refused before, or have answered too fast to have been shown at all.
    private fun isPermanentlyDenied(permission: String): Boolean {
        if (shouldShowRequestPermissionRationale(permission)) return false
        val answeredInstantly = SystemClock.elapsedRealtime() - permissionAskedAt < INSTANT_DENIAL_MS
        return rationaleBeforeAsking[permission] == true || answeredInstantly
    }

    // The explanation shown after a refusal. For the microphone it ends the attempt: a call can't be placed without it.
    private fun dismissNotice() {
        val endsAttempt = notice?.camera == false
        notice = null
        if (endsAttempt) dropPlacement()
    }

    private fun retryAfterNotice() {
        val attempt = placing
        notice = null
        if (attempt == null) {
            if (callManager.state.value.phase == CallPhase.IDLE) closeScreen()
            return
        }
        askThenStart(attempt.peer, attempt.kind)
    }

    private fun openSettingsFromNotice() {
        val endsAttempt = notice?.camera == false
        notice = null
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            // Its own task, so "return to call" finds this screen on top again
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No app settings screen", e)
            toast("Open Settings to allow access for Artistry")
        }
        if (endsAttempt) dropPlacement()
    }

    // Runs `onUnlocked` right away when the phone is unlocked, otherwise after the user unlocked it for this
    private fun whenUnlocked(onUnlocked: () -> Unit, onRefused: () -> Unit) {
        val keyguard = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (keyguard == null || !keyguard.isKeyguardLocked) {
            onUnlocked()
            return
        }
        // Before Android 8 the keyguard can't be asked to step aside; the prompt then waits behind it
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            onUnlocked()
            return
        }
        if (unlocking) return
        unlocking = true
        keyguard.requestDismissKeyguard(
            this,
            object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() {
                    unlocking = false
                    onUnlocked()
                }

                override fun onDismissCancelled() {
                    unlocking = false
                    onRefused()
                }

                override fun onDismissError() {
                    unlocking = false
                    // Also reported when the keyguard went away by itself in the meantime
                    if (keyguard.isKeyguardLocked) onRefused() else onUnlocked()
                }
            },
        )
    }

    // ------------------------------------------------------------------------------------------------ lock-screen hint

    // Android 14+ can withhold full-screen notifications: an incoming call then only shows as a banner instead of
    // ringing over the lock screen. Offered once, while the first outgoing call rings.
    private fun offerLockScreenHint() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
        if (prefs.getBoolean(KEY_LOCK_SCREEN_HINT_OFFERED, false)) return
        if (NotificationManagerCompat.from(this).canUseFullScreenIntent()) return
        lockScreenHint = true
    }

    private fun rememberLockScreenHintOffered() {
        prefs.edit().putBoolean(KEY_LOCK_SCREEN_HINT_OFFERED, true).apply()
    }

    private fun dismissLockScreenHint() {
        lockScreenHint = false
        rememberLockScreenHintOffered()
    }

    private fun openFullScreenIntentSettings() {
        dismissLockScreenHint()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
        val intent = Intent(
            Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
            Uri.fromParts("package", packageName, null),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No full-screen notification settings", e)
            toast("Allow full-screen notifications for Artistry in Settings")
        }
    }

    // ------------------------------------------------------------------------------------------------ picture-in-picture

    // Worth a floating window: video is on screen and there is nothing to answer
    private fun wantsPictureInPicture(state: CallUiState): Boolean =
        state.showsVideo && state.phase.isActive && state.phase != CallPhase.INCOMING_RINGING

    private fun enterPictureInPicture(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) return false
        if (inPictureInPicture || isFinishing || !wantsPictureInPicture(callManager.state.value)) return false
        return try {
            enterPictureInPictureMode(CallPictureInPicture.params(autoEnter = true))
        } catch (e: RuntimeException) {
            // Not resizeable, or picture-in-picture switched off for the app: the screen is simply left
            Log.w(TAG, "Couldn't enter picture-in-picture", e)
            false
        }
    }

    // Keeps the aspect ratio and (Android 12+) auto-enter in step with the call: Home slides a video call into a
    // floating window, and never an audio call or a ringing one.
    private fun syncPictureInPicture(state: CallUiState) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) return
        val autoEnter = wantsPictureInPicture(state) && !isFinishing
        if (autoPictureInPicture == autoEnter) return
        autoPictureInPicture = autoEnter
        try {
            setPictureInPictureParams(CallPictureInPicture.params(autoEnter))
        } catch (e: RuntimeException) {
            Log.w(TAG, "Couldn't update picture-in-picture", e)
        }
    }

    private fun toast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "CallActivity"
        private const val PREFS = "call_screen_prefs"
        private const val KEY_OPTIONAL_ASKED = "optional_permissions_asked"
        private const val KEY_LOCK_SCREEN_HINT_OFFERED = "lock_screen_hint_offered"

        private const val START_GRACE_MS = 4_000L
        private const val ENDED_WAIT_MS = 4_000L
        private const val INSTANT_DENIAL_MS = 350L

        // The newest call screen, see claimScreen()
        private var current: WeakReference<CallActivity>? = null
    }
}

// Kept out of the activity so phones older than Android 8 never have to resolve the picture-in-picture classes
@RequiresApi(Build.VERSION_CODES.O)
private object CallPictureInPicture {
    // Portrait, like the call screen it shrinks from
    fun params(autoEnter: Boolean): PictureInPictureParams {
        val builder = PictureInPictureParams.Builder().setAspectRatio(Rational(9, 16))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) builder.setAutoEnterEnabled(autoEnter)
        return builder.build()
    }
}

/**
 * Whether a call screen is on screen right now (full screen or as a picture-in-picture window). The app's "return
 * to call" bar hides while it is: the screen itself is the way back.
 */
internal object CallScreenPresence {
    // Counted, not a flag: during a hand-over two copies of the screen overlap for a moment
    private var started = 0
    private val _visible = MutableStateFlow(false)
    val visible: StateFlow<Boolean> = _visible.asStateFlow()

    fun onScreenStarted() {
        started++
        _visible.value = true
    }

    // True when no call screen is left on screen
    fun onScreenStopped(): Boolean {
        started = (started - 1).coerceAtLeast(0)
        _visible.value = started > 0
        return started == 0
    }
}
