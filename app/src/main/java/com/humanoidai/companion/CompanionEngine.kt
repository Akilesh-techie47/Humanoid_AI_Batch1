package com.humanoidai.companion

import android.content.Context
import com.humanoidai.ai.AIManager
import com.humanoidai.alerts.AlertEngine
import com.humanoidai.ml.FaceEnrollmentManager
import com.humanoidai.ml.FaceRecognitionManager
import com.humanoidai.ml.OwnerEnrollmentManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.*

/**
 * The central hub for the Humanoid AI Companion.
 * Coordinates vision, hearing, context, and proactive behavior.
 */
class CompanionEngine(
    private val context: Context,
    private val aiManager: AIManager,
    private val ownerManager: OwnerEnrollmentManager,
    private val enrollmentManager: FaceEnrollmentManager,
    private val recognitionManager: FaceRecognitionManager,
    private val alertEngine: AlertEngine,
    private val contextEngine: com.humanoidai.context.ContextEngine,
    private val attentionManager: com.humanoidai.attention.AttentionManager,
    private val microphoneManager: com.humanoidai.hearing.MicrophoneManager,
    private val voiceEngine: com.humanoidai.voice.VoiceEngine
) {
    private val _state = MutableStateFlow(CompanionState.SLEEPING)
    val state: StateFlow<CompanionState> = _state.asStateFlow()

    private val greetingSystem = GreetingSystem(ownerManager)
    
    // New Architectural Engines (Phase 12 Integration)
    private val conversationEngine = com.humanoidai.conversation.ConversationEngine(kotlinx.coroutines.MainScope())
    private val longTermMemory = com.humanoidai.memory.LongTermMemory(context)
    private val proactiveEngine = com.humanoidai.proactive.ProactiveEngine(longTermMemory) { message ->
        voiceEngine.speak(message)
    }

    // BDI Engine Components
    private val beliefEngine = com.humanoidai.bdi.BeliefEngine()
    private val desireEngine = com.humanoidai.bdi.DesireEngine()
    private val intentionEngine = com.humanoidai.bdi.IntentionEngine()

    suspend fun initialize() {
        _state.value = CompanionState.WAKING_UP
        
        // Initial context update
        contextEngine.updateOwner(ownerManager.getOwnerName())
        contextEngine.updateAlerts(alertEngine.unreadCount.value)

        val greeting = greetingSystem.generateGreeting()
        voiceEngine.speak(greeting)

        _state.value = CompanionState.IDLE
        
        startCompanionLoop()
        
        // Start Passive Listening for wake word
        startPassiveListening()
    }

    private fun startPassiveListening() {
        microphoneManager.startPassiveListening {
            // Wake word detected!
            _state.value = CompanionState.LISTENING
            voiceEngine.speak("I'm listening.")
            
            // In a real flow, we'd then trigger startListening with prompt callbacks.
            // For now, we transition state and the UI/Companion loop handles the rest.
        }
    }

    private fun startCompanionLoop() {
        kotlinx.coroutines.MainScope().launch {
            while (true) {
                val context = contextEngine.currentContext.value
                
                // 1. Attention Manager
                attentionManager.evaluate(context)
                
                // 2. BDI Decision Cycle
                val beliefs = beliefEngine.updateBeliefs(context)
                val desires = desireEngine.generateDesires(beliefs)
                val intention = intentionEngine.determineIntention(desires, beliefs)
                
                // 3. Proactive Execution
                if (intention != null) {
                    proactiveEngine.evaluate(context)
                }
                
                delay(2000) // Heartbeat every 2 seconds
            }
        }
    }

    fun wakeUp() {
        _state.value = CompanionState.IDLE
    }

    fun sleep() {
        _state.value = CompanionState.SLEEPING
    }

    fun updateState(newState: CompanionState) {
        _state.value = newState
    }
}
