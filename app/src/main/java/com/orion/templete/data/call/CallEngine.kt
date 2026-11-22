package com.orion.templete.data.call

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.SurfaceView
import com.orion.templete.data.model.call.AudioRoute
import dagger.hilt.android.qualifiers.ApplicationContext
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.CameraCapturerConfiguration
import io.agora.rtc2.video.VideoCanvas
import io.agora.rtc2.video.VideoEncoderConfiguration
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

// What happened to this phone's connection to the call's channel
enum class EngineConnection {
    RECONNECTING, // the network dropped; Agora keeps retrying
    CONNECTED,
    FAILED,       // Agora gave up (rejected, banned, bad token...)
    REPLACED,     // the same account joined the channel from another device
}

/*
 * The Agora RTC engine: one for the process, created lazily ON THE MAIN THREAD with the App ID the backend hands
 * out, and never destroyed (tearing it down is what crashed the reference app). At the end of a call the camera is
 * stopped and the channel is left; the engine stays.
 *
 * Every method must be called on the main thread and never throws: each SDK call is wrapped. SDK callbacks arrive
 * on an SDK thread and are re-posted to the main thread; the ones still queued when the call is left are dropped.
 */
@Singleton
class CallEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    // Always called on the main thread
    interface Listener {
        fun onJoined()
        fun onRemoteJoined(uid: Int)
        // dropped = the other phone vanished (no network) instead of leaving
        fun onRemoteLeft(uid: Int, dropped: Boolean)
        fun onConnection(event: EngineConnection)
        // The channel token is about to expire, has expired, or was refused
        fun onTokenExpiring()
        fun onRemoteMicMuted(uid: Int, muted: Boolean)
        fun onRemoteCamera(uid: Int, on: Boolean)
        fun onWeakNetwork(weak: Boolean)
        fun onAudioRoute(route: AudioRoute)
        // The camera could not be opened (permission gone, no camera)
        fun onCameraFailed()
    }

    var listener: Listener? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var engine: RtcEngine? = null
    private var createdAppId: String? = null

    // Bumped when a call is left: callbacks posted before that belong to the old call
    private val epoch = AtomicInteger(0)

    private var joinRequested = false
    private var cameraOn = false
    private var localView: SurfaceView? = null
    private var remoteView: SurfaceView? = null
    private var remoteViewUid = 0

    val isCreated: Boolean get() = engine != null

    // Creates the engine if needed. False when it can't be created (blank App ID, native library missing).
    fun ensureCreated(appId: String): Boolean {
        if (engine != null) {
            if (createdAppId != appId) Log.w(TAG, "The App ID changed; the engine keeps the one it was created with")
            return true
        }
        if (appId.isBlank()) return false
        try {
            val config = RtcEngineConfig()
            config.mContext = context.applicationContext
            config.mAppId = appId
            config.mEventHandler = eventHandler
            config.mChannelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            config.mAudioScenario = Constants.AUDIO_SCENARIO_MEETING
            val created: RtcEngine? = RtcEngine.create(config)
            if (created == null) {
                Log.e(TAG, "The call engine was not created")
                return false
            }
            engine = created
            createdAppId = appId
        } catch (e: Exception) {
            Log.e(TAG, "Couldn't create the call engine", e)
            return false
        } catch (e: LinkageError) {
            // The native library is missing for this CPU (e.g. an x86 emulator image without ARM translation)
            Log.e(TAG, "The call engine isn't available on this device", e)
            return false
        }
        // The reference app's production settings; no private "che.*" parameters
        sdk("enableAudio") { it.enableAudio() }
        sdk("setAudioProfile") {
            it.setAudioProfile(Constants.AUDIO_PROFILE_SPEECH_STANDARD, Constants.AUDIO_SCENARIO_MEETING)
        }
        sdk("enableVideo") { it.enableVideo() }
        sdk("setVideoEncoderConfiguration") {
            it.setVideoEncoderConfiguration(
                VideoEncoderConfiguration(
                    VideoEncoderConfiguration.VD_640x480,
                    VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_30,
                    VideoEncoderConfiguration.STANDARD_BITRATE,
                    VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE,
                )
            )
        }
        // The video module is on so the other side's camera can be shown at any time; this phone's camera only
        // runs while a call asks for it
        sdk("enableLocalVideo") { it.enableLocalVideo(false) }
        return true
    }

    // Outgoing ring: resolves the channel ahead of time so joining is quicker once the callee picks up
    fun preload(credentials: CallCredentials) {
        sdk("preloadChannel") { it.preloadChannel(credentials.token, credentials.channel, credentials.uid) }
    }

    // False when the channel could not be joined at all
    fun join(credentials: CallCredentials, speakerByDefault: Boolean, micMuted: Boolean): Boolean {
        if (!ensureCreated(credentials.appId)) return false
        if (joinRequested) sdk("leaveChannel") { it.leaveChannel() } // never two channels at once
        // Has to be set before joining; Agora still prefers a connected headset
        sdk("setDefaultAudioRoutetoSpeakerphone") { it.setDefaultAudioRoutetoSpeakerphone(speakerByDefault) }
        val options = ChannelMediaOptions()
        options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
        options.publishMicrophoneTrack = true
        options.publishCameraTrack = cameraOn
        options.autoSubscribeAudio = true
        options.autoSubscribeVideo = true
        val joined = sdk("joinChannel") {
            it.joinChannel(credentials.token, credentials.channel, credentials.uid, options)
        }
        joinRequested = joined
        // Mute = stop sending, not "switch the microphone off": the latter restarts the audio device
        if (joined) sdk("muteLocalAudioStream") { it.muteLocalAudioStream(micMuted) }
        return joined
    }

    // End of a call: camera off, canvases released, channel left. Safe to call at any time.
    fun leave() {
        epoch.incrementAndGet()
        if (engine == null) {
            localView = null
            remoteView = null
            return
        }
        bindLocalView(null)
        bindRemoteView(null, remoteViewUid)
        if (cameraOn) {
            sdk("stopPreview") { it.stopPreview() }
            sdk("enableLocalVideo") { it.enableLocalVideo(false) }
        }
        cameraOn = false
        if (joinRequested) {
            sdk("leaveChannel") { it.leaveChannel() }
            sdk("muteLocalAudioStream") { it.muteLocalAudioStream(false) } // the next call starts unmuted
        }
        joinRequested = false
    }

    fun renewToken(token: String) {
        sdk("renewToken") { it.renewToken(token) }
    }

    fun setMicMuted(muted: Boolean) {
        sdk("muteLocalAudioStream") { it.muteLocalAudioStream(muted) }
    }

    // Camera capture + local preview, and (once in the channel) publishing it. A no-op until the engine exists.
    fun setCamera(on: Boolean, front: Boolean) {
        if (engine == null || on == cameraOn) return
        if (on) {
            // Before the camera starts, so the first frame already comes from the side the screen shows
            sdk("setCameraCapturerConfiguration") {
                it.setCameraCapturerConfiguration(
                    CameraCapturerConfiguration(
                        if (front) CameraCapturerConfiguration.CAMERA_DIRECTION.CAMERA_FRONT
                        else CameraCapturerConfiguration.CAMERA_DIRECTION.CAMERA_REAR
                    )
                )
            }
            sdk("enableLocalVideo") { it.enableLocalVideo(true) }
            sdk("startPreview") { it.startPreview() }
        } else {
            sdk("stopPreview") { it.stopPreview() }
            sdk("enableLocalVideo") { it.enableLocalVideo(false) }
        }
        cameraOn = on
        if (joinRequested) {
            val options = ChannelMediaOptions()
            options.publishCameraTrack = on
            options.publishMicrophoneTrack = true
            sdk("updateChannelMediaOptions") { it.updateChannelMediaOptions(options) }
        }
    }

    fun switchCamera(): Boolean = cameraOn && sdk("switchCamera") { it.switchCamera() }

    // Only works inside a channel (before that the default route decides); a connected headset still wins
    fun setSpeaker(on: Boolean): Boolean =
        joinRequested && sdk("setEnableSpeakerphone") { it.setEnableSpeakerphone(on) }

    // The screen's SurfaceView for this phone's camera; null releases the canvas. force = set it again even if
    // it is the same view (after joining).
    fun bindLocalView(view: SurfaceView?, force: Boolean = false) {
        // Nothing can be bound before the engine exists; the manager binds again as soon as it does
        if (engine == null) return
        if (view === localView && !(force && view != null)) return
        localView = view
        sdk("setupLocalVideo") { it.setupLocalVideo(VideoCanvas(view, VideoCanvas.RENDER_MODE_HIDDEN, 0)) }
    }

    // The screen's SurfaceView for the other person's video; null releases the canvas
    fun bindRemoteView(view: SurfaceView?, uid: Int, force: Boolean = false) {
        if (engine == null) return
        if (view === remoteView && uid == remoteViewUid && !(force && view != null)) return
        if (view == null && remoteView == null) return
        remoteView = view
        remoteViewUid = uid
        sdk("setupRemoteVideo") { it.setupRemoteVideo(VideoCanvas(view, VideoCanvas.RENDER_MODE_HIDDEN, uid)) }
    }

    // Runs one SDK call; true when it reported success. The engine never throws through here.
    private inline fun sdk(what: String, block: (RtcEngine) -> Int): Boolean {
        val rtc = engine ?: return false
        return try {
            val result = block(rtc)
            if (result != 0) Log.w(TAG, "$what returned $result")
            result == 0
        } catch (e: Exception) {
            Log.w(TAG, "$what failed", e)
            false
        }
    }

    // SDK thread -> main thread, unless the call it was about has been left in the meantime
    private fun deliver(block: (Listener) -> Unit) {
        val posted = epoch.get()
        mainHandler.post {
            if (posted != epoch.get()) return@post
            val target = listener ?: return@post
            try {
                block(target)
            } catch (e: Exception) {
                Log.e(TAG, "A call event could not be handled", e)
            }
        }
    }

    private val eventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            deliver { it.onJoined() }
        }

        override fun onRejoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            deliver { it.onConnection(EngineConnection.CONNECTED) }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            deliver { it.onRemoteJoined(uid) }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            when (reason) {
                Constants.USER_OFFLINE_QUIT -> deliver { it.onRemoteLeft(uid, false) }
                Constants.USER_OFFLINE_DROPPED -> deliver { it.onRemoteLeft(uid, true) }
                else -> Unit // became audience: not possible in a 1:1 call
            }
        }

        override fun onConnectionStateChanged(state: Int, reason: Int) {
            Log.i(TAG, "Connection state $state (reason $reason)")
            val event = when {
                reason == Constants.CONNECTION_CHANGED_SAME_UID_LOGIN -> EngineConnection.REPLACED
                state == Constants.CONNECTION_STATE_RECONNECTING -> EngineConnection.RECONNECTING
                state == Constants.CONNECTION_STATE_CONNECTED -> EngineConnection.CONNECTED
                state == Constants.CONNECTION_STATE_FAILED -> EngineConnection.FAILED
                else -> null // connecting, or disconnected because this phone left
            }
            if (event != null) deliver { it.onConnection(event) }
        }

        override fun onConnectionLost() {
            // 10 s without the server; Agora keeps trying and the manager's own deadline decides when to give up
            Log.w(TAG, "Connection lost")
        }

        override fun onTokenPrivilegeWillExpire(token: String?) {
            deliver { it.onTokenExpiring() }
        }

        override fun onRequestToken() {
            deliver { it.onTokenExpiring() }
        }

        override fun onUserMuteAudio(uid: Int, muted: Boolean) {
            deliver { it.onRemoteMicMuted(uid, muted) }
        }

        override fun onUserMuteVideo(uid: Int, muted: Boolean) {
            deliver { it.onRemoteCamera(uid, !muted) }
        }

        override fun onRemoteVideoStateChanged(uid: Int, state: Int, reason: Int, elapsed: Int) {
            when (state) {
                Constants.REMOTE_VIDEO_STATE_STARTING, Constants.REMOTE_VIDEO_STATE_DECODING ->
                    deliver { it.onRemoteCamera(uid, true) }
                Constants.REMOTE_VIDEO_STATE_STOPPED -> deliver { it.onRemoteCamera(uid, false) }
                else -> Unit // frozen / failed: a network hiccup, the camera is still on
            }
        }

        override fun onNetworkQuality(uid: Int, txQuality: Int, rxQuality: Int) {
            if (uid != 0) return // 0 = this phone
            val worst = maxOf(txQuality, rxQuality)
            when {
                worst == Constants.QUALITY_EXCELLENT || worst == Constants.QUALITY_GOOD -> deliver { it.onWeakNetwork(false) }
                worst >= Constants.QUALITY_POOR && worst <= Constants.QUALITY_DOWN -> deliver { it.onWeakNetwork(true) }
                else -> Unit // unknown / still measuring
            }
        }

        override fun onAudioRouteChanged(routing: Int) {
            val route = when (routing) {
                Constants.AUDIO_ROUTE_EARPIECE -> AudioRoute.EARPIECE
                Constants.AUDIO_ROUTE_SPEAKERPHONE, Constants.AUDIO_ROUTE_LOUDSPEAKER -> AudioRoute.SPEAKER
                Constants.AUDIO_ROUTE_BLUETOOTH_DEVICE_HFP, Constants.AUDIO_ROUTE_BLUETOOTH_DEVICE_A2DP ->
                    AudioRoute.BLUETOOTH
                Constants.AUDIO_ROUTE_HEADSET, Constants.AUDIO_ROUTE_HEADSETNOMIC, Constants.AUDIO_ROUTE_USBDEVICE,
                Constants.AUDIO_ROUTE_USB_HEADSET, Constants.AUDIO_ROUTE_HDMI -> AudioRoute.WIRED_HEADSET
                else -> null
            }
            if (route != null) deliver { it.onAudioRoute(route) }
        }

        override fun onLocalVideoStateChanged(source: Constants.VideoSourceType?, state: Int, reason: Int) {
            if (state != Constants.LOCAL_VIDEO_STREAM_STATE_FAILED) return
            Log.w(TAG, "Local video failed (reason $reason)")
            if (reason == Constants.LOCAL_VIDEO_STREAM_REASON_DEVICE_NO_PERMISSION ||
                reason == Constants.LOCAL_VIDEO_STREAM_REASON_DEVICE_NOT_FOUND
            ) {
                deliver { it.onCameraFailed() }
            }
        }

        override fun onError(err: Int) {
            // Most errors are informational; only the ones that really end a connection arrive (also) as a
            // connection state, which is what the call reacts to
            Log.w(TAG, "Engine error $err")
            if (err == Constants.ERR_TOKEN_EXPIRED || err == Constants.ERR_INVALID_TOKEN) {
                deliver { it.onTokenExpiring() }
            }
        }
    }

    private companion object {
        const val TAG = "CallEngine"
    }
}
