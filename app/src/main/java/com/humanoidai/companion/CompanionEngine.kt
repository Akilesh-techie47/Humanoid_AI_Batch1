package com.humanoidai.companion

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.humanoidai.context.CurrentContext
import com.humanoidai.voice.TTSManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Master lifecycle controller. Owns the observe -> listen -> understand -> decide -> speak loop.
 */
class CompanionEngine(
    private val context: Context,
    private val ttsManager: TTSManager,
    private val contextEngine: com.humanoidai.context.ContextEngine,
    private val microphoneManager: com.humanoidai.hearing.SpeechRecognizerManager
) : ViewModel() {

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
        
        viewModelScope.launch {
            _state.value = CompanionState.WAKING
            
            // Initialise TTS
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
        loopJob?.cancel()
        loopJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                val context = contextFlow.value
                
                // 1. OBSERVE (Updated via contextEngine)
                
                // 2. LISTEN (Updated via contextEngine/microphoneManager)
                
                // 3. UNDERSTAND (BDI Step 1: Update Beliefs)
                val beliefs = beliefEngine.updateBeliefs(context)
                
                // 4. DECIDE (BDI Step 2 & 3: Generate Desires & Intentions)
                val desires = desireEngine.generateDesires(beliefs)
                val intention = intentionEngine.determineIntention(desires, beliefs)
                
                // 5. SPEAK IF NEEDED
                if (intention != null) {
                    // Logic to fulfill intention
                }
                
                delay(1500) // Reduced frequency to 1.5s to prevent UI stutter
            }
        }
    }

    fun sleep() {
        loopJob?.cancel()
        _state.value = CompanionState.SLEEPING
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }
}
