package com.humanoidai.memory

import com.humanoidai.ai.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages short-term conversation history.
 */
class ConversationMemory {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    fun addMessage(message: ChatMessage) {
        _messages.value = _messages.value + message
    }

    fun updateLastAiMessage(text: String) {
        val current = _messages.value
        if (current.isNotEmpty() && !current.last().isUser) {
            val updated = current.dropLast(1) + current.last().copy(text = text)
            _messages.value = updated
        }
    }

    fun getHistorySnippet(limit: Int = 10): String {
        return _messages.value.takeLast(limit).joinToString("\n") { 
            "${if (it.isUser) "User" else "Humanoid"}: ${it.text}" 
        }
    }

    fun clear() {
        _messages.value = emptyList()
    }
}
