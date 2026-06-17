package com.humanoidai.ai

/**
 * Encapsulates a request to an AI provider
 */
data class AIRequest(
    val prompt: String,
    val temperature: Float = 0.7f,
    val maxTokens: Int = 512,
    val stopSequences: List<String> = emptyList()
)
