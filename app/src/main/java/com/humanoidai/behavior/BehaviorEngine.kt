package com.humanoidai.behavior

import android.util.Log
import com.humanoidai.voice.VoiceEngine
import com.humanoidai.voice.SpeechTone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The central orchestrator for system behavior and multimodal interaction.
 */
class BehaviorEngine(
    private val voiceEngine: VoiceEngine,
    private val microphoneManager: com.humanoidai.hearing.SpeechRecognizerManager? = null,
    private val embodimentManager: com.humanoidai.embodiment.EmbodimentManager? = null
) {

    companion object {
        private const val TAG = "BehaviorEngine"
    }

    val conversationManager = ConversationManager()
    val turnManager = TurnManager(voiceEngine)
    val responseComposer = ResponseComposer()
    val notificationCoordinator = NotificationCoordinator()

    private val _communicationLevel = MutableStateFlow(CommunicationLevel.BALANCED)
    val communicationLevel: StateFlow<CommunicationLevel> = _communicationLevel.asStateFlow()

    private val _personality = MutableStateFlow(PersonalityProfile.FRIENDLY)
    val personality: StateFlow<PersonalityProfile> = _personality.asStateFlow()

    fun setCommunicationLevel(level: CommunicationLevel) {
        _communicationLevel.value = level
    }

    fun setPersonality(profile: PersonalityProfile) {
        _personality.value = profile
    }

    /**
     * Entry point for new interaction requests.
     */
    fun deliverResponse(response: InteractionResponse) {
        notificationCoordinator.post(response) { readyResponse ->
            executeMultimodalResponse(readyResponse)
        }
    }

    private fun executeMultimodalResponse(response: InteractionResponse) {
        Log.i(TAG, "Delivering response: ${response.text}")
        
        if (response.channels.contains(InteractionChannel.VOICE) && response.spokenText.isNotEmpty()) {
            if (embodimentManager != null) {
                embodimentManager.executeAction(
                    com.humanoidai.embodiment.ActionRequest.Speak(
                        text = response.spokenText,
                        priority = response.priority.ordinal * 25 // Map to 0-100
                    )
                )
            } else {
                turnManager.onAiStartedResponding()
                
                // Interaction 2.0: Enable Barge-in by keeping mic active or ready
                microphoneManager?.startPassiveListening(
                    onDetected = {
                        // Wake word while speaking
                        voiceEngine.stop()
                        turnManager.onUserStartedSpeaking()
                    },
                    onSpeechStarted = {
                        // Natural Barge-in: Human started talking
                        Log.i(TAG, "Natural Barge-in detected. Interrupting AI.")
                        voiceEngine.stop()
                        turnManager.onUserStartedSpeaking()
                    }
                )
                
                voiceEngine.speak(response.spokenText, tone = mapPriorityToTone(response.priority)) {
                    turnManager.onAiFinishedResponding()
                    microphoneManager?.startPassiveListening(
                        onDetected = {
                            // Logic for wake word after AI finishes
                        }
                    )
                }
            }
        }

        // Add to conversation history
        conversationManager.addTurn(ConversationTurn(null, response.text))
        
        // TODO: Signal UI to display text and HUD animations
    }

    private fun mapPriorityToTone(priority: InteractionPriority): SpeechTone {
        return when (priority) {
            InteractionPriority.CRITICAL -> SpeechTone.URGENT
            InteractionPriority.HIGH -> SpeechTone.ALERT
            InteractionPriority.LOW -> SpeechTone.NEUTRAL
            InteractionPriority.NORMAL -> SpeechTone.NEUTRAL
        }
    }
}
