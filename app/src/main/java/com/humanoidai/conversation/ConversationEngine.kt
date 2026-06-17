package com.humanoidai.conversation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Manages the flow of conversation.
 * Implements state transitions and timeouts for passive modes.
 */
class ConversationEngine(private val scope: CoroutineScope) {

    private val _state = MutableStateFlow(ConversationState.IDLE)
    val state: StateFlow<ConversationState> = _state.asStateFlow()

    private var timeoutJob: Job? = null
    private val CONVERSATION_TIMEOUT = 30000L // 30 seconds

    fun updateState(newState: ConversationState) {
        _state.value = newState
        
        // Reset timeout if we enter a state that requires user interaction
        if (newState == ConversationState.WAITING) {
            startTimeOut()
        } else {
            timeoutJob?.cancel()
        }
    }

    private fun startTimeOut() {
        timeoutJob?.cancel()
        timeoutJob = scope.launch(Dispatchers.Default) {
            delay(CONVERSATION_TIMEOUT)
            if (_state.value == ConversationState.WAITING) {
                updateState(ConversationState.PASSIVE)
            }
        }
    }

    fun endConversation() {
        updateState(ConversationState.IDLE)
    }
}
