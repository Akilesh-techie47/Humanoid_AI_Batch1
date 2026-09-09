package com.humanoidai.ai

/**
 * Standardized response from any AI provider
 */
data class AIResponse(
    val text: String,
    val requestId: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val provider: String,
    val model: String? = null,
    val confidence: Float = 1.0f,
    val generationTimeMs: Long = 0L,
    val tokenCount: Int = 0,
    val error: String? = null,
    val errorCategory: AIError? = null,
    val structuredData: Map<String, Any>? = null,
    val toolCalls: List<Any>? = null,
    val metadata: Map<String, Any> = emptyMap()
)
