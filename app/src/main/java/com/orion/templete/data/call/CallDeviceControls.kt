package com.orion.templete.data.call

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.annotation.RequiresApi
import com.orion.templete.data.model.call.AudioRoute
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/*
 * The phone around a call: audio focus (music pauses while it rings and during the call), the two wake locks, and
 * which audio outputs are connected. The audio MODE is left alone on purpose: Agora manages it itself.
 * Main thread only; nothing here throws.
 */
@Singleton
class CallDeviceControls @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val audioManager: AudioManager? by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }
    private val powerManager: PowerManager? by lazy {
        context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    }

    // Focus changes are not acted on: a call is not something to duck or pause, and Agora owns the audio session
    private val focusListener = AudioManager.OnAudioFocusChangeListener { }

    // An AudioFocusRequest (API 26+); typed loosely so this class also loads on Android 7
    private var focusRequest: Any? = null
    private var focusRequested = false

    // Keeps the CPU running while a call rings or is connected, so its timeouts fire on time with the screen off
    private val cpuLock: PowerManager.WakeLock? by lazy {
        try {
            powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "artistry:call")
                ?.apply { setReferenceCounted(false) }
        } catch (e: Exception) {
            Log.w(TAG, "No wake lock for calls", e)
            null
        }
    }

    // Screen off (and touch ignored) while the phone is held to the ear
    private val proximityLock: PowerManager.WakeLock? by lazy {
        try {
            val manager = powerManager
            if (manager != null && manager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
                manager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "artistry:call-proximity")
                    .apply { setReferenceCounted(false) }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "No proximity lock for calls", e)
            null
        }
    }

    fun requestAudioFocus() {
        if (focusRequested) return
        val manager = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requestFocusApi26(manager)
            } else {
                @Suppress("DEPRECATION")
                manager.requestAudioFocus(
                    focusListener, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
            }
            focusRequested = true
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't request audio focus", e)
        }
    }

    fun abandonAudioFocus() {
        if (!focusRequested) return
        focusRequested = false
        val manager = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                abandonFocusApi26(manager)
            } else {
                @Suppress("DEPRECATION")
                manager.abandonAudioFocus(focusListener)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't abandon audio focus", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun requestFocusApi26(manager: AudioManager) {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener(focusListener)
            .build()
        focusRequest = request
        manager.requestAudioFocus(request)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun abandonFocusApi26(manager: AudioManager) {
        val request = focusRequest as? AudioFocusRequest
        focusRequest = null
        if (request != null) manager.abandonAudioFocusRequest(request)
    }

    fun setCpuAwake(on: Boolean) = hold(cpuLock, on)

    fun setProximityScreenOff(on: Boolean) = hold(proximityLock, on)

    private fun hold(lock: PowerManager.WakeLock?, on: Boolean) {
        if (lock == null) return
        try {
            if (on) {
                // The timeout only guards against a lock that is never released; a call ends long before
                if (!lock.isHeld) lock.acquire(MAX_HOLD_MS)
            } else if (lock.isHeld) {
                lock.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't switch a call wake lock", e)
        }
    }

    // Where a call's sound can go right now. Bluetooth counts only when it can carry a call (not a music speaker).
    fun availableRoutes(): Set<AudioRoute> {
        val routes = LinkedHashSet<AudioRoute>()
        var earpiece = false
        var wired = false
        var bluetooth = false
        try {
            val devices: Array<AudioDeviceInfo> =
                audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS) ?: emptyArray<AudioDeviceInfo>()
            for (device in devices) {
                when (device.type) {
                    AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> earpiece = true
                    AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES, TYPE_USB_HEADSET ->
                        wired = true
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO, TYPE_HEARING_AID, TYPE_BLE_HEADSET -> bluetooth = true
                    else -> Unit
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't list the audio outputs", e)
            earpiece = true
        }
        // With a wired headset plugged in the earpiece is out of reach
        if (wired) routes.add(AudioRoute.WIRED_HEADSET) else if (earpiece) routes.add(AudioRoute.EARPIECE)
        routes.add(AudioRoute.SPEAKER)
        if (bluetooth) routes.add(AudioRoute.BLUETOOTH)
        return routes
    }

    // The route a call is expected to use before Agora reports the real one: a connected headset wins, otherwise
    // the speaker when asked for (or when there is no earpiece, as on a tablet)
    fun expectedRoute(speaker: Boolean, routes: Set<AudioRoute>): AudioRoute = when {
        AudioRoute.WIRED_HEADSET in routes -> AudioRoute.WIRED_HEADSET
        AudioRoute.BLUETOOTH in routes -> AudioRoute.BLUETOOTH
        speaker || AudioRoute.EARPIECE !in routes -> AudioRoute.SPEAKER
        else -> AudioRoute.EARPIECE
    }

    private companion object {
        const val TAG = "CallDevice"
        const val MAX_HOLD_MS = 4 * 60 * 60 * 1000L

        // AudioDeviceInfo constants newer than minSdk 24 (plain ints, safe to compare on any version)
        const val TYPE_USB_HEADSET = 22 // API 26
        const val TYPE_HEARING_AID = 23 // API 28
        const val TYPE_BLE_HEADSET = 26 // API 31
    }
}
