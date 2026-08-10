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
        _messages.value += message
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
