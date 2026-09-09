package com.humanoidai.ai

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Real implementation for Ollama AI Provider (Local Laptop execution).
 */
class OllamaProvider(
    private var endpoint: String = "http://192.168.1.60:11434",
    private var model: String = "phi:latest"
) : AIProvider {
    override val id: String = "ollama-local"
    override val name: String = "Ollama (Local Laptop)"
    override val capabilities: Set<AICapability> = setOf(
        AICapability.TEXT, 
        AICapability.STREAMING, 
        AICapability.LOCAL
    )

    private var client = OllamaClient(endpoint, model)

    fun updateConfig(newEndpoint: String, newModel: String) {
        if (endpoint != newEndpoint || model != newModel) {
            endpoint = newEndpoint
            model = newModel
            client = OllamaClient(endpoint, model)
            Log.i("OllamaProvider", "Config updated: $endpoint | $model")
        }
    }

    override suspend fun isAvailable(): Boolean {
        return try {
            if (!client.checkHealth()) return false
            val models = client.listModels()
            models.any { it.startsWith(model.split(":")[0]) } // Match base model name
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun initialize() { }

    override suspend fun generate(request: AIRequest): AIResponse {
        val startTime = System.currentTimeMillis()
        return try {
            val text = client.generate(request.prompt)
            AIResponse(
                text = text,
                provider = id,
                model = model,
                requestId = request.requestId,
                generationTimeMs = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            Log.e("OllamaProvider", "Error: ${e.message}")
            AIResponse(
                text = "Local AI offline. Connecting to cloud...",
                provider = id,
                requestId = request.requestId,
                error = e.localizedMessage,
                errorCategory = AIError.NETWORK_ERROR,
                generationTimeMs = System.currentTimeMillis() - startTime
            )
        }
    }

    override fun stream(request: AIRequest): Flow<String> {
        return try {
            client.stream(request.prompt)
        } catch (e: Exception) {
            emptyFlow()
        }
    }

    override fun cancel() { }
    override fun release() { }
}
