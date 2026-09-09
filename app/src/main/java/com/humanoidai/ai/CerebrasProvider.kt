package com.humanoidai.ai

import com.humanoidai.ai.networking.GenericAiClient
import com.humanoidai.ai.networking.GenericAiException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class CerebrasProvider(
    private val apiKey: String,
    private var model: String = "llama3.1-70b"
) : AIProvider {
    override val id: String = "cerebras-cloud"
    override val name: String = "Cerebras Cloud"
    override val capabilities: Set<AICapability> = setOf(
        AICapability.TEXT,
        AICapability.STREAMING,
        AICapability.CLOUD,
        AICapability.REASONING,
        AICapability.LONG_CONTEXT
    )

    private var client: GenericAiClient? = null

    override suspend fun isAvailable(): Boolean = apiKey.isNotBlank()

    override suspend fun initialize() {
        if (client == null && apiKey.isNotBlank()) {
            client = GenericAiClient(apiKey, "https://api.cerebras.ai/v1/chat/completions", model)
        }
    }

    override suspend fun generate(request: AIRequest): AIResponse {
        val startTime = System.currentTimeMillis()
        if (client == null) initialize()
        
        return try {
            val text = client?.generate(request) ?: throw Exception("Client not initialized")
            AIResponse(
                text = text,
                provider = id,
                model = model,
                requestId = request.requestId,
                generationTimeMs = System.currentTimeMillis() - startTime
            )
        } catch (e: GenericAiException) {
            AIResponse(
                text = "Cerebras failed.",
                provider = id,
                error = e.message,
                errorCategory = if (e.code == 429) AIError.RATE_LIMIT else AIError.SERVER_ERROR,
                generationTimeMs = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            AIResponse(
                text = "Cerebras offline.",
                provider = id,
                error = e.message,
                errorCategory = AIError.NETWORK_ERROR,
                generationTimeMs = System.currentTimeMillis() - startTime
            )
        }
    }

    override fun stream(request: AIRequest): Flow<String> {
        return client?.stream(request) ?: emptyFlow()
    }

    override fun cancel() {}
    override fun release() { client = null }
}
