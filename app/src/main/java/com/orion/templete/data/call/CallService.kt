package com.orion.templete.data.call

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.orion.templete.data.model.call.CallDirection
import com.orion.templete.data.model.call.CallKind
import com.orion.templete.data.model.call.CallPeer
import com.orion.templete.data.model.call.CallPhase
import com.orion.templete.data.model.call.CallUiState
import com.orion.templete.domain.call.CallManager
import com.orion.templete.domain.call.CallPermissions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/*
 * Keeps the process (and its microphone / camera) alive for the whole call and shows the call's notification.
 * It holds no call state: it renders CallManager.state and stops ITSELF when that is IDLE. Nothing ever calls
 * stopService() on it, because a stop that overtakes startForeground() crashes the app.
 *
 * Foreground types. Android decides when a service is STARTED whether it may use the microphone / camera once the
 * app is in the background, and a start from a push (app in the background) does not get that. So:
 *   - an incoming call that only rings runs as "phoneCall";
 *   - when the call is placed or answered, and whenever the call screen comes to the front, CallManagerImpl sends
 *     a new start command (the app is in the foreground then) and the service goes to phoneCall | microphone,
 *     plus camera while the camera is on. A type is only asked for when its permission is granted (Android 14
 *     throws otherwise), and a refusal falls back to phoneCall alone.
 */
@AndroidEntryPoint
class CallService : Service() {

    @Inject
    lateinit var callManager: CallManager

    @Inject
    lateinit var notifications: CallNotifications

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate + CoroutineExceptionHandler { _, error ->
            Log.e(TAG, "A call service task failed", error)
        }
    )

    // What the notification currently on screen was built from; a state change that alters none of it posts nothing
    private data class Shown(
        val phase: CallPhase,
        val callId: String?,
        val peer: CallPeer?,
        val kind: CallKind,
        val video: Boolean,
        val connectedAt: Long?,
        val types: Int,
        val withPhoto: Boolean,
    )

    private var shown: Shown? = null
    private var observer: Job? = null
    private var photoJob: Job? = null
    private var photoUrl: String? = null
    private var stopping = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Before anything else, on every start: a service started with startForegroundService() that does not
        // reach startForeground() in time takes the app down
        val state = try {
            callManager.state.value
        } catch (e: Exception) {
            Log.e(TAG, "The call state is not available", e)
            CallUiState()
        }
        show(state, force = true)
        if (state.phase == CallPhase.IDLE) {
            // The call was over before the service got going
            stopNow()
            return START_NOT_STICKY
        }
        stopping = false
        observe()
        loadPhoto(state)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun observe() {
        if (observer?.isActive == true) return
        observer = scope.launch {
            try {
                callManager.state.collect { state ->
                    try {
                        if (state.phase == CallPhase.IDLE) {
                            stopNow()
                        } else {
                            show(state, force = false)
                            loadPhoto(state)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(TAG, "Couldn't follow the call state", e)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "The call service lost the call state", e)
            }
        }
    }

    // startForeground() with the notification and the foreground types for this state
    private fun show(state: CallUiState, force: Boolean) {
        val types = foregroundTypes(state)
        val next = Shown(
            phase = state.phase,
            callId = state.callId,
            peer = state.peer,
            kind = state.kind,
            video = state.showsVideo,
            connectedAt = state.connectedAtElapsed,
            types = types,
            withPhoto = hasPhoto(state.peer),
        )
        if (!force && next == shown) return
        val notification = try {
            notifications.callNotification(state, inService = true)
        } catch (e: Exception) {
            Log.e(TAG, "Couldn't build the call notification", e)
            CallNotifications.fallbackNotification(this)
        }
        startForegroundSafely(notification, types)
        shown = next
    }

    private fun hasPhoto(peer: CallPeer?): Boolean = try {
        notifications.cachedAvatar(peer) != null
    } catch (e: Exception) {
        false
    }

    @SuppressLint("MissingPermission")
    private fun startForegroundSafely(notification: Notification, types: Int) {
        if (tryStartForeground(notification, types)) return
        // Microphone / camera are refused when the last start came from the background, or a permission was
        // taken away meanwhile. Give up the camera first, then the microphone: the call notification must stay up.
        val phoneCall = phoneCallType()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val withoutCamera = types and ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA.inv()
            if (withoutCamera != types && withoutCamera != phoneCall && tryStartForeground(notification, withoutCamera)) {
                return
            }
        }
        if (types != phoneCall && tryStartForeground(notification, phoneCall)) return
        // Not even that: show the notification without being in the foreground rather than nothing at all
        try {
            NotificationManagerCompat.from(this).notify(CallNotifications.CALL_NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't post the call notification", e)
        }
    }

    private fun tryStartForeground(notification: Notification, types: Int): Boolean = try {
        // Below Android 10 the types are ignored
        ServiceCompat.startForeground(this, CallNotifications.CALL_NOTIFICATION_ID, notification, types)
        true
    } catch (e: Exception) {
        // SecurityException (a type's permission is missing / not usable from the background) or, on Android 12+,
        // ForegroundServiceStartNotAllowedException
        Log.w(TAG, "startForeground was refused for types $types", e)
        false
    }

    private fun phoneCallType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL else 0

    private fun foregroundTypes(state: CallUiState): Int {
        var types = phoneCallType()
        // The microphone and camera types exist from Android 11
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return types
        // A call that only rings in records nothing yet (and was probably started from the background)
        val media = state.phase.isInCall || (state.direction == CallDirection.OUTGOING && state.phase.isRinging)
        if (!media) return types
        if (CallPermissions.hasMicrophone(this)) {
            types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        }
        if (state.localCameraOn && CallPermissions.hasCamera(this)) {
            types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
        }
        return types
    }

    // The notification never waits for the photo: it is loaded afterwards (once per person) and then shown
    private fun loadPhoto(state: CallUiState) {
        val peer = state.peer ?: return
        val url = peer.avatar?.takeIf { it.isNotBlank() } ?: return
        if (photoUrl == url) return
        photoUrl = url
        photoJob?.cancel()
        photoJob = scope.launch {
            try {
                if (notifications.loadAvatar(peer) == null || stopping) return@launch
                val current = callManager.state.value
                if (current.phase != CallPhase.IDLE && current.peer?.avatar == url) show(current, force = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Couldn't load the photo for the call notification", e)
            }
        }
    }

    // Only ever called after startForeground() (every start command begins with it). Calling it twice is harmless.
    private fun stopNow() {
        stopping = true
        observer?.cancel()
        observer = null
        photoJob?.cancel()
        try {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't leave the foreground", e)
        }
        stopSelf()
    }

    private companion object {
        const val TAG = "CallService"
    }
}
