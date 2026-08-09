package com.humanoidai.voice

/**
 * Routes speech requests to the best available engine.
 * Currently uses Android System TTS, but ready for ElevenLabs/Piper integration.
 */
class SpeechOutputRouter(
    private val defaultEngine: SpeechOutputEngine
) : SpeechOutputEngine {

    override fun speak(text: String, tone: SpeechTone, onComplete: () -> Unit) {
        // Implementation logic to try primary (network) then fallback to offline
        defaultEngine.speak(text, tone, onComplete)
    }

    override fun stop() {
        defaultEngine.stop()
    }

    override fun shutdown() {
        defaultEngine.shutdown()
    }
}
