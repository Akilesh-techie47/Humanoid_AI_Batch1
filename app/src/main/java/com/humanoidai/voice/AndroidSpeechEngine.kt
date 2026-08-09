package com.humanoidai.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.*

/**
 * Standard Android TTS implementation.
 */
class AndroidSpeechEngine(context: Context) : SpeechOutputEngine {

    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isReady = true
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
            }
        }
    }

    override fun speak(text: String, tone: SpeechTone, onComplete: () -> Unit) {
        if (!isReady) {
            onComplete()
            return
        }

        // Apply a "Strong, Friendly Male" base (Lower pitch for masculinity, confident rate)
        val basePitch = 0.76f // Enforced deep male resonance
        val baseRate = 1.05f  // Confident, professional pacing

        val (rateMult, pitchMult) = when (tone) {
            SpeechTone.URGENT -> 1.12f to 1.08f
            SpeechTone.ALERT -> 1.08f to 1.04f
            SpeechTone.CONCERNED -> 0.92f to 0.96f
            SpeechTone.PLAYFUL -> 1.05f to 1.12f
            else -> 1.0f to 1.0f
        }

        tts?.setSpeechRate(baseRate * rateMult)
        tts?.setPitch(basePitch * pitchMult)

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { onComplete() }
            override fun onError(utteranceId: String?) { onComplete() }
        })

        val params = android.os.Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "humanoid_speech")
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "humanoid_speech")
    }

    override fun stop() {
        tts?.stop()
    }

    override fun shutdown() {
        tts?.shutdown()
    }
}
