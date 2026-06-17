package com.humanoidai.ai

/**
 * Standardized response from any AI provider
 */
data class AIResponse(
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val provider: String,
    val confidence: Float = 1.0f,
    val generationTimeMs: Long = 0L,
    val tokenCount: Int = 0,
    val error: String? = null
)
