package com.humanoidai.ai

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * AI Provider implementation for Groq LPU Cloud.
 * Specialized for ultra-low latency inference.
 */
class GroqProvider(
    private val apiKey: String = "",
    private var model: String = "llama3-8b-8192"
) : AIProvider {
    override val id: String = "groq-cloud"
    override val name: String = "Groq LPU Cloud"
    override val capabilities: Set<AICapability> = setOf(
        AICapability.TEXT, 
        AICapability.STREAMING, 
        AICapability.CLOUD,
        AICapability.FAST_RESPONSE
    )

    private var client: GroqClient? = null

    override suspend fun isAvailable(): Boolean {
        return apiKey.isNotBlank() && apiKey.startsWith("gsk_")
    }

    override suspend fun initialize() {
        if (apiKey.isNotBlank() && client == null) {
            client = GroqClient(apiKey, model)
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
            val text = client?.generate(request) ?: throw Exception("Groq not initialized")
            AIResponse(
                text = text,
                provider = id,
                model = model,
                requestId = request.requestId,
                generationTimeMs = System.currentTimeMillis() - startTime
            )
        } catch (e: GroqException) {
            Log.e("GroqProvider", "Groq API Error: ${e.message} | Metadata: ${e.metadata}")
            AIResponse(
                text = "Groq LPU reached a limit. Switching to fallback...",
                provider = id,
                requestId = request.requestId,
                error = e.localizedMessage,
                errorCategory = when (e.code) {
                    401 -> AIError.AUTHENTICATION_ERROR
                    429 -> AIError.RATE_LIMIT
                    else -> AIError.SERVER_ERROR
                },
                metadata = e.metadata,
                generationTimeMs = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            Log.e("GroqProvider", "Generic Error: ${e.message}")
            AIResponse(
                text = "Groq LPU offline. Connection failed.",
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

    override fun cancel() {
        // OkHttp handles cancellation via call.cancel() if we were to expose it
    }

    override fun release() {
        client = null
    }
}
