package com.orion.templete.data.call

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/*
 * The sounds of a call that is not connected yet.
 * - Incoming: the phone's own ringtone (looping) and a repeating vibration, as far as the ringer mode allows:
 *   silent or Do Not Disturb -> nothing, vibrate -> vibration only. The notification channel itself is silent.
 * - Outgoing: the ringback tone the caller hears while the other phone rings.
 * stop() ends all of it and is safe to call at any time. Main thread only; nothing here throws.
 */
@Singleton
class CallRinger @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var player: MediaPlayer? = null
    private var vibrating = false
    private var ringback: ToneGenerator? = null
    private var ringbackLoud = false

    private val ringAttributes: AudioAttributes by lazy {
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    }

    fun startRinging() {
        stop()
        val mode = try {
            (context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)?.ringerMode
                ?: AudioManager.RINGER_MODE_NORMAL
        } catch (e: Exception) {
            AudioManager.RINGER_MODE_NORMAL
        }
        if (mode == AudioManager.RINGER_MODE_SILENT || isDoNotDisturbOn()) return
        startVibration()
        if (mode == AudioManager.RINGER_MODE_NORMAL) startRingtone()
    }

    // loud = the call will use the speaker (or a headset): the tone goes where media plays. Otherwise it plays
    // at the ear, like a phone call.
    fun startRingback(loud: Boolean) {
        if (ringback != null && ringbackLoud == loud) return
        stopRingback()
        try {
            val stream = if (loud) AudioManager.STREAM_MUSIC else AudioManager.STREAM_VOICE_CALL
            val generator = ToneGenerator(stream, RINGBACK_VOLUME)
            ringback = generator
            ringbackLoud = loud
            generator.startTone(ToneGenerator.TONE_SUP_RINGTONE)
        } catch (e: Exception) {
            // The system ran out of audio tracks; the call works without the tone
            Log.w(TAG, "Couldn't play the ringback tone", e)
            stopRingback()
        }
    }

    fun stop() {
        stopRingtone()
        stopVibration()
        stopRingback()
    }

    private fun isDoNotDisturbOn(): Boolean = try {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val filter = manager?.currentInterruptionFilter ?: NotificationManager.INTERRUPTION_FILTER_ALL
        filter != NotificationManager.INTERRUPTION_FILTER_ALL && filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
    } catch (e: Exception) {
        false
    }

    private fun startRingtone() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE) ?: return
        val created = MediaPlayer()
        player = created
        try {
            created.setAudioAttributes(ringAttributes)
            created.setDataSource(context, uri)
            created.isLooping = true
            // Prepared off the main thread; if the ringing stopped meanwhile this player is no longer the current one
            created.setOnPreparedListener { prepared ->
                if (player === prepared) {
                    try {
                        prepared.start()
                    } catch (e: Exception) {
                        Log.w(TAG, "Couldn't start the ringtone", e)
                    }
                }
            }
            created.setOnErrorListener { failed, what, extra ->
                Log.w(TAG, "Ringtone error $what/$extra")
                if (player === failed) stopRingtone()
                true
            }
            created.prepareAsync()
        } catch (e: Exception) {
            // No ringtone set, or it can't be read: ring silently (the vibration and the notification remain)
            Log.w(TAG, "Couldn't play the ringtone", e)
            stopRingtone()
        }
    }

    private fun stopRingtone() {
        val current = player ?: return
        player = null
        try {
            current.setOnPreparedListener(null)
            current.setOnErrorListener(null)
        } catch (e: Exception) {
            // already released
        }
        try {
            current.release() // allowed in any state, also while still preparing
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't release the ringtone", e)
        }
    }

    private fun startVibration() {
        try {
            val vibrator = vibrator() ?: return
            if (!vibrator.hasVibrator()) return
            // Marked as a ringtone so the system lets it run while the app is in the background
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                @Suppress("DEPRECATION")
                vibrator.vibrate(VibrationEffect.createWaveform(VIBRATION_PATTERN, 0), ringAttributes)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(VIBRATION_PATTERN, 0, ringAttributes)
            }
            vibrating = true
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't vibrate", e)
        }
    }

    private fun stopVibration() {
        if (!vibrating) return
        vibrating = false
        try {
            vibrator()?.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't stop the vibration", e)
        }
    }

    private fun vibrator(): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            defaultVibratorApi31()
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun defaultVibratorApi31(): Vibrator? =
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator

    private fun stopRingback() {
        val current = ringback ?: return
        ringback = null
        try {
            current.stopTone()
        } catch (e: Exception) {
            // nothing to do
        }
        try {
            current.release()
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't release the ringback tone", e)
        }
    }

    private companion object {
        const val TAG = "CallRinger"
        const val RINGBACK_VOLUME = 80 // percent of the stream's volume

        // wait, buzz, pause; repeated from the start
        val VIBRATION_PATTERN = longArrayOf(0L, 900L, 1100L)
    }
}
