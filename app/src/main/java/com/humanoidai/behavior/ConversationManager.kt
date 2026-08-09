package com.humanoidai.behavior

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Manages conversational continuity, topics, and pending actions.
 */
class ConversationManager {

    private val _currentTopic = MutableStateFlow<String?>(null)
    val currentTopic: StateFlow<String?> = _currentTopic.asStateFlow()

    private val _pendingClarification = MutableStateFlow<String?>(null)
    val pendingClarification: StateFlow<String?> = _pendingClarification.asStateFlow()

    private val _history = MutableStateFlow<List<ConversationTurn>>(emptyList())
    val history: StateFlow<List<ConversationTurn>> = _history.asStateFlow()

    fun updateTopic(topic: String?) {
        _currentTopic.value = topic
    }

    fun setClarificationRequest(question: String?) {
        _pendingClarification.value = question
    }

    fun addTurn(turn: ConversationTurn) {
        _history.update { (it + turn).takeLast(50) }
    }

    fun clearHistory() {
        _history.value = emptyList()
        _currentTopic.value = null
        _pendingClarification.value = null
    }
}

data class ConversationTurn(
    val userText: String?,
    val aiResponse: String,
    val timestamp: Long = System.currentTimeMillis()
)
