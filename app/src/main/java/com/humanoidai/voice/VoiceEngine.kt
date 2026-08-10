package com.humanoidai.voice

import android.content.Context
import android.util.Log

/**
 * Coordinates speech generation with the BDI state and persona.
 * Main entry point for AI vocalization.
 */
class VoiceEngine(context: Context) {

    private val formatter = SpeechFormatter()
    private val outputRouter: SpeechOutputEngine = SpeechOutputRouter(AndroidSpeechEngine(context))
    
    private var lastSpokenText = ""
    private var lastSpokenTime = 0L

    fun speak(text: String, tone: SpeechTone = SpeechTone.NEUTRAL, priority: Boolean = false, onComplete: () -> Unit = {}) {
        val now = System.currentTimeMillis()
        
        // Cooldown for repeated phrases (unless priority like alert)
        if (!priority && (text == lastSpokenText) && ((now - lastSpokenTime) < 15_000)) {
            onComplete()
            return
        }

        val chunks = formatter.splitIntoChunks(text)
        speakSequential(chunks, tone, onComplete)

        lastSpokenText = text
        lastSpokenTime = now
    }

    private fun speakSequential(chunks: List<String>, tone: SpeechTone, onAllComplete: () -> Unit) {
        if (chunks.isEmpty()) {
            onAllComplete()
            return
        }

        val head = chunks.first()
        val tail = chunks.drop(1)

        val ssml = formatter.format(head, tone)
        Log.d("VoiceEngine", "Speaking chunk: $head")
        
        outputRouter.speak(ssml, tone) {
            speakSequential(tail, tone, onAllComplete)
        }
    }

    fun stop() {
        outputRouter.stop()
    }

    fun shutdown() {
        outputRouter.shutdown()
    }
}
