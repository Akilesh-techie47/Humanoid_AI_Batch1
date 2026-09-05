package com.humanoidai.companion

import android.content.Context
import android.util.Log
import com.humanoidai.ai.AIManager
import com.humanoidai.voice.VoiceEngine
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
            microphoneManager.startPassiveListening(
                onDetected = {
                    Log.i("HumanoidEngine", "[VOICE] WAKE_WORD_DETECTED (via CompanionEngine)")
                    voiceEngine.stop()
                    _state.value = CompanionState.LISTENING
                    
                    voiceEngine.speak("I'm listening.") {
                        // After speaking "I'm listening", start active listening for the command
                        microphoneManager.startListening(
                            onPartialResult = { /* UI update handled by flow */ },
                            onFinalResult = { command ->
                                if (command.isNotBlank() && command != "RETRY_PROMPT") {
                                    ask(command, "Owner", "Home")
                                } else {
                                    // Back to passive
                                    _state.value = CompanionState.OBSERVING
                                    wake() // Re-init wake word
                                }
                            }
                        )
                    }
                },
                onSpeechStarted = {
                    // Interaction 2.0: Barge-in
                    if (_state.value == CompanionState.SPEAKING) {
                        Log.i("HumanoidEngine", "[VOICE] BARGE_IN_DETECTED (User spoke during AI speech)")
                        voiceEngine.stop()
                        _state.value = CompanionState.LISTENING
                    }
                }
            )
            
            // Initial Greeting
            voiceEngine.speak("Systems initialized. Humanoid AI is online.")
        }
    }

    private fun startCompanionLoop() {
        android.util.Log.i("HumanoidEngine", "Companion Loop: Starting")
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
                        android.util.Log.d("HumanoidEngine", "Loop: Intention formed - ${intention}")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("HumanoidEngine", "Loop Error: ${e.message}")
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
            
            // Interaction 2.0: AI response streaming start
            // The vocalization happens inside aiManager.ask via speakStream
            // We need a way to know when speaking starts.
            
            aiManager.ask(question, ownerName, screen) {
                _state.value = CompanionState.OBSERVING
                onComplete()
                
                // Restart passive listening
                microphoneManager.startPassiveListening(
                    onDetected = {
                        _state.value = CompanionState.LISTENING
                        voiceEngine.speak("I'm listening.")
                    },
                    onSpeechStarted = {
                        if (_state.value == CompanionState.SPEAKING) {
                            voiceEngine.stop()
                            _state.value = CompanionState.LISTENING
                        }
                    }
                )
            }
        }
    }

    fun shutdown() {
        sleep()
        voiceEngine.shutdown()
    }
}
