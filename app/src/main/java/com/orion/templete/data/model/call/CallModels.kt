package com.orion.templete.data.model.call

// Domain models for 1:1 audio / video calls (Agora). The backend owns the call's lifecycle (calls/{callId} in
// Firestore); CallManager turns it, the Agora engine and the user's taps into one CallUiState.

enum class CallKind(val wire: String) {
    AUDIO("audio"), VIDEO("video");

    companion object {
        fun fromWire(value: String?): CallKind = if (value == VIDEO.wire) VIDEO else AUDIO
    }
}

// calls/{callId}.status
enum class CallStatus(val wire: String, val isFinal: Boolean) {
    RINGING("ringing", false),
    ACCEPTED("accepted", false),
    ENDED("ended", true),
    DECLINED("declined", true),
    CANCELLED("cancelled", true),
    MISSED("missed", true),
    BUSY("busy", true);

    companion object {
        fun fromWire(value: String?): CallStatus? = entries.firstOrNull { it.wire == value }
    }
}

data class CallPeer(
    val username: String,
    val name: String? = null,
    val avatar: String? = null,
) {
    val displayName: String get() = name?.takeIf { it.isNotBlank() } ?: username
}

enum class CallDirection { OUTGOING, INCOMING }

enum class CallPhase {
    IDLE,
    // Outgoing: asking the backend to place the call (can take a while when the server is waking up)
    OUTGOING_STARTING,
    // Outgoing: the other phone is ringing
    OUTGOING_RINGING,
    // Incoming: this phone is ringing
    INCOMING_RINGING,
    // Answered; joining the channel / waiting for the other side's media
    CONNECTING,
    CONNECTED,
    // Connected before, network dropped; Agora is retrying
    RECONNECTING,
    // Over: endReason says why. Shown briefly, then the state returns to IDLE
    ENDED;

    val isActive: Boolean get() = this != IDLE && this != ENDED
    val isRinging: Boolean get() = this == OUTGOING_STARTING || this == OUTGOING_RINGING || this == INCOMING_RINGING
    val isInCall: Boolean get() = this == CONNECTING || this == CONNECTED || this == RECONNECTING
}

enum class CallEndReason {
    HUNG_UP,            // this user ended / cancelled / declined
    REMOTE_HUNG_UP,     // the other person ended the call
    DECLINED,           // outgoing: they declined
    BUSY,               // outgoing: they are on another call
    NO_ANSWER,          // outgoing: nobody picked up
    MISSED,             // incoming: the caller gave up before this phone answered
    ANSWERED_ELSEWHERE, // incoming: answered on another device of this account
    CONNECTION_LOST,
    FAILED,             // couldn't place / join the call; see CallUiState.message
}

enum class AudioRoute { EARPIECE, SPEAKER, BLUETOOTH, WIRED_HEADSET }

// An incoming-call push (FCM "incoming_call")
data class IncomingCallInvite(
    val callId: String,
    val kind: CallKind,
    val caller: CallPeer,
    val conversationId: String,
    val sentAt: Long,           // epoch ms, server clock
    val ringTimeoutSec: Int,
)

data class CallUiState(
    val phase: CallPhase = CallPhase.IDLE,
    val callId: String? = null,
    val direction: CallDirection? = null,
    val peer: CallPeer? = null,
    val conversationId: String? = null,
    // How the call was started; the screen shows the video layout whenever localCameraOn || remoteCameraOn
    val kind: CallKind = CallKind.AUDIO,
    val micMuted: Boolean = false,
    val localCameraOn: Boolean = false,
    val frontCamera: Boolean = true,
    val audioRoute: AudioRoute = AudioRoute.EARPIECE,
    val availableRoutes: Set<AudioRoute> = setOf(AudioRoute.EARPIECE, AudioRoute.SPEAKER),
    val remoteJoined: Boolean = false,
    val remoteMicMuted: Boolean = false,
    val remoteCameraOn: Boolean = false,
    // SystemClock.elapsedRealtime() when both sides were connected; the call timer counts from here
    val connectedAtElapsed: Long? = null,
    val weakNetwork: Boolean = false,
    val endReason: CallEndReason? = null,
    // User-facing text for FAILED (e.g. "Calls are being set up. Please try again later.")
    val message: String? = null,
) {
    val showsVideo: Boolean get() = localCameraOn || remoteCameraOn
}

// Push payload contract shared by the backend, ChatMessagingService and CallManager
object CallPush {
    const val TYPE_INCOMING_CALL = "incoming_call"
    const val TYPE_CALL_ENDED = "call_ended"

    const val KEY_TYPE = "type"
    const val KEY_CALL_ID = "callId"
    const val KEY_KIND = "kind"
    const val KEY_CALLER = "caller"
    const val KEY_CALLER_NAME = "callerName"
    const val KEY_CALLER_AVATAR = "callerAvatar"
    // Who the push is for: a phone that still holds another account's FCM token must ignore the call
    const val KEY_CALLEE = "callee"
    const val KEY_CONVERSATION_ID = "conversationId"
    const val KEY_SENT_AT = "sentAt"
    const val KEY_RING_TIMEOUT_SEC = "ringTimeoutSec"
    const val KEY_REASON = "reason"

    const val REASON_CANCELLED = "cancelled"
    const val REASON_MISSED = "missed"
    const val REASON_ANSWERED = "answered"
    const val REASON_DECLINED = "declined"

    const val DEFAULT_RING_TIMEOUT_SEC = 45
}
