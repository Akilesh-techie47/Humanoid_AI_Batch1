package com.humanoidai.ai

import com.humanoidai.ai.networking.GenericAiClient
import com.humanoidai.ai.networking.GenericAiException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class NvidiaProvider(
    private val apiKey: String,
    private var model: String = "nvidia/llama-3.1-8b-instruct"
) : AIProvider {
    override val id: String = "nvidia-cloud"
    override val name: String = "NVIDIA NIM"
    override val capabilities: Set<AICapability> = setOf(
        AICapability.TEXT,
        AICapability.STREAMING,
        AICapability.CLOUD
    )

    private var client: GenericAiClient? = null

    override suspend fun isAvailable(): Boolean = apiKey.isNotBlank()

    override suspend fun initialize() {
        if (client == null && apiKey.isNotBlank()) {
            client = GenericAiClient(apiKey, "https://integrate.api.nvidia.com/v1/chat/completions", model)
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
                text = "NVIDIA failed.",
                provider = id,
                error = e.message,
                errorCategory = if (e.code == 429) AIError.RATE_LIMIT else AIError.SERVER_ERROR,
                generationTimeMs = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            AIResponse(
                text = "NVIDIA offline.",
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
