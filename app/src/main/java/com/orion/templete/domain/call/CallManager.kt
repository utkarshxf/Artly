package com.orion.templete.domain.call

import android.view.SurfaceView
import com.orion.templete.data.model.call.CallKind
import com.orion.templete.data.model.call.CallPeer
import com.orion.templete.data.model.call.CallUiState
import com.orion.templete.data.model.call.IncomingCallInvite
import kotlinx.coroutines.flow.StateFlow

/**
 * The one place that knows about the current call. It combines the backend's call lifecycle (calls/{callId}),
 * the Agora engine and the user's actions into [state]; the call screen, the foreground service and the
 * notifications only render that state. All methods are main-thread, cheap and never throw.
 */
interface CallManager {
    val state: StateFlow<CallUiState>

    // Whether the server can place calls (null = not known yet). Call buttons are hidden while this is false.
    val callsEnabled: StateFlow<Boolean?>

    // Asks the backend whether calls are set up (this also wakes a sleeping backend). Cached; safe to call often.
    fun refreshConfig()

    /**
     * Places a call. Returns false and does nothing when another call is active or the peer is not callable.
     * The caller must already hold RECORD_AUDIO; a video call without CAMERA starts with the camera off.
     */
    fun startCall(peer: CallPeer, kind: CallKind): Boolean

    // Push "incoming_call": rings (service, notification, ringtone) unless it is stale, a duplicate, or this phone
    // is already on a call (then the new call is declined as busy).
    fun onIncomingCall(invite: IncomingCallInvite)

    // Push "call_ended": the caller gave up, or the call was answered / declined on another device.
    fun onCallEndedPush(callId: String, reason: String, kind: CallKind, caller: CallPeer, conversationId: String)

    // INCOMING_RINGING -> CONNECTING. Needs RECORD_AUDIO (the call screen asks for it first).
    fun accept()

    // INCOMING_RINGING -> over
    fun decline()

    // Cancels an outgoing ring, or ends the call in progress
    fun hangUp()

    fun setMicMuted(muted: Boolean)

    // Needs CAMERA. Turning it on in an audio call upgrades it to video for both sides.
    fun setCameraEnabled(enabled: Boolean)

    fun switchCamera()

    // Speaker on, or back to the earpiece / connected headset
    fun setSpeakerOn(on: Boolean)

    // Video surfaces belong to the call screen. Pass null when a view leaves the screen so its canvas is released.
    fun setLocalVideoView(view: SurfaceView?)
    fun setRemoteVideoView(view: SurfaceView?)

    // The call screen is on screen (true) or gone (false). Drives the proximity lock (screen off at the ear) and
    // tells the manager whether it has to bring the call screen up itself.
    fun setCallScreenVisible(visible: Boolean)
}
