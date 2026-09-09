package com.humanoidai.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Base implementation for offline/on-device AI.
 * Can be extended to use MediaPipe, TFLite, or ONNX.
 */
class OfflineAIProvider : AIProvider {
    override val id: String = "offline-local"
    override val name: String = "Humanoid Local Engine"
    override val capabilities: Set<AICapability> = setOf(
        AICapability.TEXT,
        AICapability.LOCAL,
        AICapability.FAST_RESPONSE
    )

    override suspend fun isAvailable(): Boolean = true

    override suspend fun initialize() {
        // Initialize local model if available
    }

    override suspend fun generate(request: AIRequest): AIResponse {
        val startTime = System.currentTimeMillis()
        
        // Placeholder for local inference
        return AIResponse(
            text = "I am processing this locally. [Offline Mode]",
            provider = id,
            model = "local-lite",
            requestId = request.requestId,
            generationTimeMs = System.currentTimeMillis() - startTime
        )
    }

    override fun stream(request: AIRequest): Flow<String> {
        return flowOf("I am processing this locally. [Offline Mode]")
    }

    override fun cancel() {}
    override fun release() {}
}
