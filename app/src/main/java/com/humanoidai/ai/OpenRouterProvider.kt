package com.humanoidai.ai

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * AI Provider implementation for OpenRouter.
 * Provides access to a wide range of models via unified API.
 */
class OpenRouterProvider(
    private val apiKey: String = "",
    private var model: String = "google/gemini-flash-1.5"
) : AIProvider {
    override val id: String = "openrouter-cloud"
    override val name: String = "OpenRouter"
    override val capabilities: Set<AICapability> = setOf(
        AICapability.TEXT, 
        AICapability.STREAMING, 
        AICapability.CLOUD
    )

    private var client: OpenRouterClient? = null

    override suspend fun isAvailable(): Boolean {
        return apiKey.isNotBlank() && apiKey.startsWith("sk-or-")
    }

    override suspend fun initialize() {
        if (apiKey.isNotBlank() && client == null) {
            client = OpenRouterClient(apiKey, model)
        }
    }

    fun updateConfig(newModel: String) {
        if (model != newModel) {
            model = newModel
            client?.updateModel(model)
        }
    }

    override suspend fun generate(request: AIRequest): AIResponse {
        val startTime = System.currentTimeMillis()
        if (client == null) initialize()

        return try {
            val text = client?.generate(request) ?: throw Exception("OpenRouter not initialized")
            AIResponse(
                text = text,
                provider = id,
                model = model,
                requestId = request.requestId,
                generationTimeMs = System.currentTimeMillis() - startTime
            )
        } catch (e: OpenRouterException) {
            Log.e("OpenRouterProvider", "OR API Error: ${e.message} | Code: ${e.code}")
            AIResponse(
                text = "OpenRouter reached a limit or returned an error.",
                provider = id,
                requestId = request.requestId,
                error = e.localizedMessage,
                errorCategory = when (e.code) {
                    401 -> AIError.AUTHENTICATION_ERROR
                    429 -> AIError.RATE_LIMIT
                    else -> AIError.SERVER_ERROR
                },
                generationTimeMs = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            Log.e("OpenRouterProvider", "Error: ${e.message}")
            AIResponse(
                text = "OpenRouter link failed.",
                provider = id,
                requestId = request.requestId,
                error = e.localizedMessage,
                errorCategory = when {
                    e.message?.contains("timeout") == true -> AIError.TIMEOUT
                    else -> AIError.NETWORK_ERROR
                },
                generationTimeMs = System.currentTimeMillis() - startTime
            )
        }
    }

    override fun stream(request: AIRequest): Flow<String> {
        return client?.stream(request) ?: emptyFlow()
    }

    override fun cancel() { }

    override fun release() {
        client = null
    }
}
