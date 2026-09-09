package com.humanoidai.companion

import android.content.Context
import android.util.Log
import com.humanoidai.ai.AIManager
import com.humanoidai.voice.VoiceEngine
import com.humanoidai.voice.TranscriptValidator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Master lifecycle controller. Owns the observe -> listen -> understand -> decide -> speak loop.
 * CEA v1.4: Refactored to standard Engine class to ensure thread stability on startup.
 */
class CompanionEngine(
    private val context: Context,
    private val voiceEngine: VoiceEngine,
    private val aiManager: AIManager,
    private val contextEngine: com.humanoidai.context.ContextEngine,
    private val microphoneManager: com.humanoidai.hearing.SpeechRecognizerManager,
    private val scope: CoroutineScope
) {

    private val _state = MutableStateFlow<CompanionState>(CompanionState.SLEEPING)
    val state: StateFlow<CompanionState> = _state.asStateFlow()

    private val _contextFlow = contextEngine.currentContext
    val contextFlow: StateFlow<com.humanoidai.context.CurrentContext> = _contextFlow

    // BDI Engine (Milestone C10)
    private val beliefEngine = com.humanoidai.bdi.BeliefEngine()
    private val desireEngine = com.humanoidai.bdi.DesireEngine()
    private val intentionEngine = com.humanoidai.bdi.IntentionEngine()

    private var loopJob: Job? = null

    init {
        aiManager.onSpeechStarted = {
            _state.value = CompanionState.SPEAKING
        }
    }

    fun wake() {
        if (_state.value != CompanionState.SLEEPING) return
        
        scope.launch {
            _state.value = CompanionState.WAKING
            
            // TTS is initialized by VoiceEngine/AndroidSpeechEngine internally
            _state.value = CompanionState.OBSERVING
            
            // Start Master Loop
            startCompanionLoop()
            
            // Start Passive Listening (Hotword Detection)
            startPassiveListening()
            
            // Initial Greeting
            voiceEngine.speak("Systems initialized. Aura 360 is online.")
        }
    }

    private fun startPassiveListening() {
        microphoneManager.startPassiveListening(
            onDetected = { handleWakeWordDetected() },
            onSpeechStarted = { handleBargeIn() }
        )
    }

    private fun handleWakeWordDetected() {
        Log.i("Aura360Engine", "[VOICE_PIPELINE] WAKE_WORD_MATCHED")
        voiceEngine.stop()
        _state.value = CompanionState.LISTENING
        
        voiceEngine.speak("I'm listening.") {
            // After speaking "I'm listening", start active listening for the command
            microphoneManager.startListening(
                onPartialResult = { /* UI updates handled via MicrophoneManager.partialTranscript Flow */ },
                onFinalResult = { command ->
                    if (TranscriptValidator.isValid(command)) {
                        val repaired = TranscriptValidator.normalize(command)
                        ask(repaired, "Owner", "Home")
                    } else {
                        Log.d("Aura360Engine", "[VOICE_PIPELINE] TRANSCRIPT_VALIDATION_FAILED: \"$command\"")
                        // Back to passive
                        _state.value = CompanionState.OBSERVING
                        startPassiveListening()
                    }
                },
                onError = { error ->
                    Log.e("Aura360Engine", "[VOICE_PIPELINE] STT_ERROR: $error")
                    _state.value = CompanionState.OBSERVING
                    startPassiveListening()
                }
            )
        }
    }

    private fun handleBargeIn() {
        // Interaction 2.0: Barge-in
        if (_state.value == CompanionState.SPEAKING || _state.value == CompanionState.THINKING) {
            Log.i("Aura360Engine", "[VOICE_PIPELINE] BARGE_IN_DETECTED (User spoke during AI activity)")
            voiceEngine.stop()
            _state.value = CompanionState.LISTENING
        }
    }

    private fun startCompanionLoop() {
        Log.i("Aura360Engine", "Companion Loop: Starting")
        loopJob?.cancel()
        loopJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                try {
                    val context = contextFlow.value
                    
                    // 1. UNDERSTAND
                    val beliefs = beliefEngine.updateBeliefs(context)
                    
                    // 2. DECIDE
                    val desires = desireEngine.generateDesires(beliefs)
                    val intention = intentionEngine.determineIntention(desires, beliefs)
                    
                    // 3. ACT (Stub)
                    if (intention != null) {
                        Log.d("Aura360Engine", "Loop: Intention formed - ${intention}")
                    }
                } catch (e: Exception) {
                    Log.e("Aura360Engine", "Loop Error: ${e.message}")
                }
                delay(2000)
            }
        }
    }

    fun sleep() {
        loopJob?.cancel()
        _state.value = CompanionState.SLEEPING
    }

    /**
     * Interaction 2.0: Coordinated Ask.
     * Manages the turn state transitions and AI interaction.
     */
    fun ask(question: String, ownerName: String, screen: String, onComplete: () -> Unit = {}) {
        scope.launch {
            _state.value = CompanionState.THINKING
            
            aiManager.ask(question, ownerName, screen) {
                _state.value = CompanionState.OBSERVING
                onComplete()
                
                // Restart passive listening
                startPassiveListening()
            }
        }
    }

    fun shutdown() {
        sleep()
        microphoneManager.stopListening()
        voiceEngine.shutdown()
    }
}
