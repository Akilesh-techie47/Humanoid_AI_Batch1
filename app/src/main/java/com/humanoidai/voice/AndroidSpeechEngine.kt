package com.humanoidai.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.*
import android.util.Log

/**
 * Enhanced Android TTS implementation.
 * Supports multilingual detection and optimized male persona.
 */
class AndroidSpeechEngine(context: Context) : SpeechOutputEngine {

    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isReady = true
                Log.d("AndroidSpeechEngine", "[TTS_INIT] Success")
                configureVoice()
            } else {
                Log.e("AndroidSpeechEngine", "[TTS_INIT] Failed: $status")
            }
        }
    }

    private fun configureVoice() {
        val maleVoice = tts?.voices?.filter { 
            it.locale.language == Locale.US.language && 
            !it.name.lowercase().contains("female") &&
            !it.name.lowercase().contains("soft")
        }?.find { 
            it.name.lowercase().contains("male") || it.name.lowercase().contains("low")
        } ?: tts?.defaultVoice
        
        tts?.voice = maleVoice
    }

    override fun speak(text: String, tone: SpeechTone, onComplete: () -> Unit) {
        if (!isReady) {
            Log.w("AndroidSpeechEngine", "[VOICE] TTS_NOT_READY")
            onComplete()
            return
        }

        // Sanitize for natural speech (remove markdown symbols)
        val sanitized = text
            .replace(Regex("<[^>]*>"), "") // Remove HTML tags
            .replace(Regex("[*_`#]"), "") // Remove common markdown symbols
            .replace(Regex("\\[(.*?)]"), "$1") // Remove bracketed content but keep inner text
            .trim()

        if (sanitized.isEmpty()) {
            onComplete()
            return
        }

        val ttsRequestedTime = System.currentTimeMillis()
        Log.i("AndroidSpeechEngine", "[VOICE] TTS_START prompt=\"${sanitized.take(30)}...\"")

        // Language detection
        val locale = if (containsTamil(sanitized)) Locale("ta", "IN") else Locale.US
        tts?.setLanguage(locale)

        // Pacing & Pitch
        val basePitch = 0.80f
        val baseRate = 1.05f

        val (rateMult, pitchMult) = when (tone) {
            SpeechTone.URGENT -> 1.12f to 1.08f
            SpeechTone.ALERT -> 1.08f to 1.04f
            SpeechTone.CONCERNED -> 0.92f to 0.96f
            SpeechTone.PLAYFUL -> 1.05f to 1.12f
            else -> 1.0f to 1.0f
        }

        tts?.setSpeechRate(baseRate * rateMult)
        tts?.setPitch(basePitch * pitchMult)

        var ttsStartedTime = 0L

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                ttsStartedTime = System.currentTimeMillis()
                Log.d("AndroidSpeechEngine", "[VOICE] TTS_EXECUTE id=$utteranceId, startup_latency=${ttsStartedTime - ttsRequestedTime}ms")
            }
            override fun onDone(utteranceId: String?) {
                val ttsEndedTime = System.currentTimeMillis()
                Log.i("AndroidSpeechEngine", "[VOICE] TTS_END id=$utteranceId, duration=${ttsEndedTime - ttsStartedTime}ms")
                onComplete()
            }
            override fun onError(utteranceId: String?) {
                Log.e("AndroidSpeechEngine", "[VOICE] TTS_ERROR id=$utteranceId")
                onComplete()
            }
        })


        val params = android.os.Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "humanoid_speech_" + System.currentTimeMillis())
        
        Log.i("AndroidSpeechEngine", "[TTS_EXECUTE] speaking sanitized text")
        tts?.speak(sanitized, TextToSpeech.QUEUE_FLUSH, params, params.getString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID))
    }



    private fun containsTamil(text: String): Boolean {
        for (char in text) {
            if (char.code in 0x0B80..0x0BFF) return true
        }
        return false
    }

    override fun stop() {
        tts?.stop()
    }

    override fun shutdown() {
        tts?.shutdown()
    }
}
