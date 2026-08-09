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

    private val VOICE_PITCH = 0.76f
    private val SPEECH_RATE = 1.05f

    suspend fun initialize(): Boolean = suspendCancellableCoroutine { continuation ->
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Aggressive Male Voice Selection - Filter out female markers
                val maleVoice = tts?.voices?.filter { 
                    it.locale.language == Locale.US.language && 
                    !it.name.lowercase().contains("female") &&
                    !it.name.lowercase().contains("soft") &&
                    !it.name.lowercase().contains("high")
                }?.find { 
                    it.name.lowercase().contains("male") || 
                    it.name.lowercase().contains("low") ||
                    it.name.lowercase().contains("sfg") ||
                    it.name.lowercase().contains("iol")
                } ?: tts?.voices?.find { 
                    it.locale.language == Locale.US.language && !it.isNetworkConnectionRequired 
                } ?: tts?.defaultVoice
                
                tts?.voice = maleVoice
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

        // Dynamic Language Detection for TTS
        val locale = if (containsTamil(text)) Locale("ta", "IN") else Locale.US
        tts?.setLanguage(locale)

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { onComplete() }
            override fun onError(utteranceId: String?) { onComplete() }
        })

        val params = android.os.Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "humanoid_speech")
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "humanoid_speech")
    }

    private fun containsTamil(text: String): Boolean {
        for (char in text) {
            if (char.code in 0x0B80..0x0BFF) return true
        }
        return false
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
    }
}
