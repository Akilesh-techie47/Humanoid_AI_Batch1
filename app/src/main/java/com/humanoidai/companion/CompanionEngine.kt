package com.humanoidai.companion

import android.content.Context
import com.humanoidai.voice.TTSManager
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
    private val ttsManager: TTSManager,
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

    fun wake() {
        if (_state.value != CompanionState.SLEEPING) return
        
        scope.launch {
            _state.value = CompanionState.WAKING
            
            // Initialise TTS (suspend)
            ttsManager.initialize()
            
            _state.value = CompanionState.OBSERVING
            
            // Start Master Loop
            startCompanionLoop()
            
            // Start Passive Listening (Hotword Detection)
            microphoneManager.startPassiveListening {
                _state.value = CompanionState.LISTENING
                ttsManager.speak("I'm listening.")
            }
            
            // Initial Greeting
            ttsManager.speak("Systems initialized. Humanoid AI is online.")
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

    fun shutdown() {
        sleep()
        ttsManager.shutdown()
    }
}
