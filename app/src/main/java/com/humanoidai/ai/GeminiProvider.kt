package com.humanoidai.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.mapNotNull

/**
 * AI Provider implementation for Google Gemini.
 * Optimized for high-speed responsiveness (Flash 1.5).
 */
class GeminiProvider(private val apiKey: String) : AIProvider {
    override val id: String = "Gemini 1.5 Flash"
    
    private var model: GenerativeModel? = null

    private val config = generationConfig {
        temperature = 0.85f // Increased for more natural "personality"
        topK = 40
        topP = 0.95f
        maxOutputTokens = 512 // Increased from 120 to allow detailed technical/educational responses
    }

    override suspend fun initialize() {
        try {
            if (model == null) {
                if (apiKey.isBlank() || !apiKey.startsWith("AIza")) {
                    android.util.Log.e("GeminiProvider", "CRITICAL: Invalid API Key format. Expected a key starting with 'AIza'. Current starts with: ${apiKey.take(4)}...")
                }
                model = GenerativeModel(
                    modelName = "gemini-1.5-flash-001", // Explicitly use a stable version
                    apiKey = apiKey,
                    generationConfig = config
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("GeminiProvider", "Failed to init GenerativeModel: ${e.message}")
        }
    }

    override suspend fun generate(request: AIRequest): AIResponse {
        val startTime = System.currentTimeMillis()
        initialize() // Ensure initialized
        
        var retryCount = 0
        val maxRetries = 2
        var lastException: Exception? = null

        while (retryCount <= maxRetries) {
            try {
                if (retryCount > 0) {
                    android.util.Log.d("GeminiProvider", "Retrying AI request... Attempt ${retryCount + 1}")
                    kotlinx.coroutines.delay(1000L * retryCount)
                }
                
                val responseStartTime = System.currentTimeMillis()
                val response = model?.generateContent(request.prompt)
                val text = response?.text ?: ""
                val responseEndTime = System.currentTimeMillis()
                
                android.util.Log.d("GeminiProvider", "[AI_RESPONSE_RAW] latency=${responseEndTime - responseStartTime}ms")
                
                if (text.isBlank()) {
                    throw Exception("Empty response from model")
                }

                return AIResponse(
                    text = text,
                    provider = id,
                    generationTimeMs = System.currentTimeMillis() - startTime
                )
            } catch (e: Exception) {
                lastException = e
                android.util.Log.w("GeminiProvider", "Generation attempt ${retryCount + 1} failed: ${e.message}")
                if (e.message?.contains("429") == true || e.message?.contains("Too Many Requests") == true) {
                    // Rate limit - back off longer
                    kotlinx.coroutines.delay(2000L * (retryCount + 1))
                }
                retryCount++
            }
        }

        android.util.Log.e("GeminiProvider", "All generation attempts failed. Last error: ${lastException?.message}")
        return AIResponse(
            text = "Neural link failed. Please check connection.",
            provider = id,
            error = lastException?.localizedMessage ?: "Unknown error",
            generationTimeMs = System.currentTimeMillis() - startTime
        )
    }


    override fun stream(request: AIRequest): Flow<String> {
        return model?.generateContentStream(request.prompt)?.mapNotNull { it.text } ?: emptyFlow()
    }

    override fun cancel() { }

    override fun release() {
        model = null
    }
}
