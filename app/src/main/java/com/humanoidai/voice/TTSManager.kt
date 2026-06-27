package com.humanoidai.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*
import kotlin.coroutines.resume

/**
 * Android TextToSpeech wrapper with natural personality settings.
 */
class TTSManager(private val context: Context) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val VOICE_PITCH = 1.05f
    private val SPEECH_RATE = 0.95f

    suspend fun initialize(): Boolean = suspendCancellableCoroutine { continuation ->
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val voice = tts?.voices?.find { 
                    it.locale == Locale.US && !it.isNetworkConnectionRequired 
                } ?: tts?.defaultVoice
                
                tts?.voice = voice
                tts?.setPitch(VOICE_PITCH)
                tts?.setSpeechRate(SPEECH_RATE)
                isInitialized = true
                continuation.resume(true)
            } else {
                continuation.resume(false)
            }
        }
    }

    fun speak(text: String, onComplete: () -> Unit = {}) {
        if (!isInitialized) return

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { onComplete() }
            override fun onError(utteranceId: String?) { onComplete() }
        })

        val params = android.os.Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "")
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "humanoid_speech")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
    }
}
