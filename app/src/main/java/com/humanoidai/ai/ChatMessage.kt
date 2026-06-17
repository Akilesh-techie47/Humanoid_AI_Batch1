package com.humanoidai.ai

import java.util.UUID

/**
 * A single entry in a conversation
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
