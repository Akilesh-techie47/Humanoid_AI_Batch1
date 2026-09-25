package com.humanoidai.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Base implementation for offline/on-device AI.
 * Currently reports as unavailable until a local model is deployed.
 */
class OfflineAIProvider : AIProvider {
    override val id: String = "offline-local"
    override val name: String = "Aura 360 Local Engine"
    override val capabilities: Set<AICapability> = setOf(
        AICapability.TEXT,
        AICapability.LOCAL
    )

    override suspend fun isAvailable(): Boolean = false // Honestly reporting as unavailable

    override suspend fun initialize() {}

    override suspend fun generate(request: AIRequest): AIResponse {
        return AIResponse(
            text = "Local intelligence engine is not yet initialized.",
            provider = id,
            model = "local-lite",
            requestId = request.requestId,
            error = "LOCAL_MODEL_MISSING"
        )
    }

    override fun stream(request: AIRequest): Flow<String> {
        return flowOf("Local intelligence engine is offline.")
    }

    override fun cancel() {}
    override fun release() {}
}
