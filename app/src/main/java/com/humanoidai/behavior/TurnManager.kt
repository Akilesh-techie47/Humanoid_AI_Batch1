package com.humanoidai.behavior

import android.util.Log
import com.humanoidai.voice.VoiceEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages interaction turns and handles interruptions.
 */
class TurnManager(private val voiceEngine: VoiceEngine) {

    companion object {
        private const val TAG = "TurnManager"
    }

    private val _state = MutableStateFlow(InteractionTurnState.IDLE)
    val state: StateFlow<InteractionTurnState> = _state.asStateFlow()

    fun onUserStartedSpeaking() {
        if (_state.value == InteractionTurnState.RESPONDING) {
            Log.i(TAG, "User interrupted AI speech. Pausing response.")
            voiceEngine.stop()
            _state.value = InteractionTurnState.LISTENING
        } else {
            _state.value = InteractionTurnState.LISTENING
        }
    }

    fun onUserFinishedSpeaking() {
        if (_state.value == InteractionTurnState.LISTENING) {
            _state.value = InteractionTurnState.THINKING
        }
    }

    fun onAiStartedResponding() {
        _state.value = InteractionTurnState.RESPONDING
    }

    fun onAiFinishedResponding() {
        if (_state.value == InteractionTurnState.RESPONDING) {
            _state.value = InteractionTurnState.IDLE
        }
    }

    fun onThinkingStarted() {
        _state.value = InteractionTurnState.THINKING
    }

    fun enterWaitingState() {
        _state.value = InteractionTurnState.WAITING_FOR_CLARIFICATION
    }
}
