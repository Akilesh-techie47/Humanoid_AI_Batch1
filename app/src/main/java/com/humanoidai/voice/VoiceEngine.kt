package com.humanoidai.voice

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect

/**
 * Coordinates speech generation with the BDI state and persona.
 * Main entry point for AI vocalization.
 */
class VoiceEngine(
    context: Context,
    private val outputRouter: SpeechOutputEngine = SpeechOutputRouter(AndroidSpeechEngine(context))
) {

    private val formatter = SpeechFormatter()
    
    private var lastSpokenText = ""
    private var lastSpokenTime = 0L

    private val speechScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var streamJob: Job? = null

    /**
     * Interaction 2.0: Instant Vocalization.
     * Speaks a text immediately, cancelling any previous speech.
     */
    fun speak(text: String, tone: SpeechTone = SpeechTone.NEUTRAL, priority: Boolean = false, onComplete: () -> Unit = {}) {
        val now = System.currentTimeMillis()
        Log.d("VoiceEngine", "[VOICE_PIPELINE] TTS_INPUT: \"${text.take(20)}...\"")
        
        // Cooldown for repeated phrases (unless priority like alert)
        if (!priority && (text == lastSpokenText) && ((now - lastSpokenTime) < 5_000)) {
            onComplete()
            return
        }

        stop() // Immediate interruption

        val chunks = formatter.splitIntoChunks(text)
        speakSequential(chunks, tone, onComplete)

        lastSpokenText = text
        lastSpokenTime = now
    }

    /**
     * Interaction 2.0: Stream-aware vocalization.
     * Starts speaking as soon as natural sentences arrive from the AI.
     */
    suspend fun speakStream(phraseFlow: Flow<String>, tone: SpeechTone = SpeechTone.NEUTRAL) {
        stop()
        coroutineScope {
            val job = launch {
                phraseFlow.collect { phrase ->
                    if (!isActive) return@collect
                    
                    // Natural unit received
                    Log.d("VoiceEngine", "[VOICE_PIPELINE] TTS_STREAM_PHRASE: $phrase")
                    val chunks = formatter.splitIntoChunks(phrase)
                    
                    // Wait for completion of current phrase before next one to avoid overlap
                    val latch = CompletableDeferred<Unit>()
                    speakSequential(chunks, tone) {
                        latch.complete(Unit)
                    }
                    latch.await()
                }
            }
            streamJob = job
            job.join()
        }
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
        streamJob?.cancel()
        streamJob = null
        outputRouter.stop()
    }

    fun shutdown() {
        outputRouter.shutdown()
    }
}
