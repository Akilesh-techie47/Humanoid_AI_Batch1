package com.humanoidai.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.*

class VoiceEngine(context: Context) {
    private var tts: TextToSpeech? = null
    private var isReady = false
    private var lastSpokenText = ""
    private var lastSpokenTime = 0L

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Use a more natural, higher-quality voice if available
                val voice = tts?.voices?.find { 
                    it.locale == Locale.US && !it.isNetworkConnectionRequired 
                } ?: tts?.defaultVoice
                
                tts?.voice = voice
                tts?.setPitch(1.05f) // Slightly more energetic, less robotic
                tts?.setSpeechRate(0.95f) // More natural pacing
                isReady = true
            }
        }
    }

    fun speak(text: String, priority: Boolean = false) {
        val now = System.currentTimeMillis()
        // Prevent repeating the same greeting too often (30s cooldown)
        if (!priority && text == lastSpokenText && (now - lastSpokenTime) < 30_000) return

        if (isReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            lastSpokenText = text
            lastSpokenTime = now
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
