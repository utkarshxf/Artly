package com.orion.templete.domain.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale

/**
 * Thin wrapper over the Android built-in TextToSpeech engine.
 * Reads artwork story text aloud in the reels feed. Free and works offline
 * with whatever voices are installed on the device.
 */
class StoryTtsManager(context: Context) {

    private var tts: TextToSpeech? = null
    private var ready = false
    private var pendingSpeak: (() -> Unit)? = null

    var onUtteranceDone: ((String) -> Unit)? = null
    var onUtteranceStarted: ((String) -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                pendingSpeak?.invoke()
                pendingSpeak = null
            } else {
                Log.w(TAG, "TTS init failed with status $status")
            }
        }
    }

    /** Whether [locale] can be spoken by the installed engine. */
    fun isLanguageUsable(locale: Locale): Boolean {
        val engine = tts ?: return false
        if (!ready) return false
        val result = engine.isLanguageAvailable(locale)
        return result == TextToSpeech.LANG_AVAILABLE ||
                result == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
                result == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
    }

    /** Offline device voices that can speak [locale], best first. */
    fun deviceVoicesFor(locale: Locale): List<Voice> {
        val engine = tts ?: return emptyList()
        if (!ready) return emptyList()
        return engine.voices
            ?.filter { it.locale.language == locale.language && !it.isNetworkConnectionRequired }
            ?.sortedByDescending { it.quality }
            ?: emptyList()
    }

    /**
     * Speak [text] aloud, replacing anything currently playing.
     * [utteranceId] lets the caller match done callbacks to a specific artwork.
     * If the engine is still initialising, the request is queued and runs once ready.
     */
    fun speak(utteranceId: String, text: String, locale: Locale, voice: Voice? = null) {
        val engine = tts ?: return
        val action = {
            engine.language = locale
            if (voice != null && voice.locale.language == locale.language) {
                engine.voice = voice
            }
            val params = Bundle()
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            Unit
        }
        if (ready) action() else pendingSpeak = action
    }

    fun stop() {
        pendingSpeak = null
        if (ready) tts?.stop()
    }

    fun shutdown() {
        pendingSpeak = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }

    private val progressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String) {
            onUtteranceStarted?.invoke(utteranceId)
        }

        override fun onDone(utteranceId: String) {
            onUtteranceDone?.invoke(utteranceId)
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String) {
            onUtteranceDone?.invoke(utteranceId)
        }
    }.also { listener ->
        // Attach immediately; safe even before init completes.
        tts?.setOnUtteranceProgressListener(listener)
    }

    private companion object {
        const val TAG = "StoryTtsManager"
    }
}
