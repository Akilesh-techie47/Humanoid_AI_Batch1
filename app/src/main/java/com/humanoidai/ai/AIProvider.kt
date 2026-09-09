package com.humanoidai.ai

import kotlinx.coroutines.flow.Flow

/**
 * Capabilities that an AI provider can declare.
 */
enum class AICapability {
    TEXT,
    STREAMING,
    VISION,
    TOOL_USE,
    TOOLS,
    LOCAL,
    CLOUD,
    FAST_RESPONSE,
    REASONING,
    CODING,
    LONG_CONTEXT,
    AUDIO,
    VIDEO
}

/**
 * Common interface for all AI models (Gemini, Groq, Ollama, etc.)
 */
interface AIProvider {
    /**
     * Unique identifier for the provider (e.g., "gemini-1.5-flash")
     */
    val id: String

    /**
     * Human-readable name for the provider
     */
    val name: String

    /**
     * Set of capabilities supported by this provider
     */
    val capabilities: Set<AICapability>

    /**
     * Check if the provider is currently configured and available.
     * Includes health check and quota status.
     */
    suspend fun isAvailable(): Boolean

    /**
     * Internal health check for the provider.
     */
    suspend fun healthCheck(): Boolean = isAvailable()

    /**
     * Perform any asynchronous initialization
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
