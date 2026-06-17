package com.humanoidai.ai

import kotlinx.coroutines.flow.Flow

/**
 * Common interface for all AI models (Gemini, SmolLM2, Mock, etc.)
 */
interface AIProvider {
    /**
     * Unique identifier for the provider
     */
    val id: String

    /**
     * Perform any asynchronous initialization (loading weights, etc.)
     */
    suspend fun initialize()

    /**
     * Generate a one-shot response for the given request
     */
    suspend fun generate(request: AIRequest): AIResponse

    /**
     * Optional: Stream response word-by-word
     */
    fun stream(request: AIRequest): Flow<String>

    /**
     * Cancel any ongoing generation
     */
    fun cancel()

    /**
     * Cleanup resources
     */
    fun release()
}
