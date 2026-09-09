package com.humanoidai.ai

import android.graphics.BitmapFactory
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.InvalidAPIKeyException
import com.google.ai.client.generativeai.type.ResponseStoppedException
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.mapNotNull

/**
 * AI Provider implementation for Google Gemini.
 * Optimized for high-speed responsiveness (Flash 1.5).
 */
class GeminiProvider(private val apiKey: String) : AIProvider {
    override val id: String = "gemini-1.5-flash"
    override val name: String = "Google Gemini 1.5 Flash"
    override val capabilities: Set<AICapability> = setOf(
        AICapability.TEXT,
        AICapability.STREAMING,
        AICapability.VISION,
        AICapability.CLOUD
    )

    override suspend fun isAvailable(): Boolean {
        return apiKey.isNotBlank() && apiKey.startsWith("AIza")
    }
    
    private var model: GenerativeModel? = null

    private val config = generationConfig {
        temperature = 0.85f // Increased for more natural "personality"
        topK = 40
        topP = 0.95f
        maxOutputTokens = 512 // Increased from 120 to allow detailed technical/educational responses
    }

    private fun ensureInitialized() {
        if (model == null) {
            try {
                if (!apiKey.startsWith("AIza")) {
                    Log.e("GeminiProvider", "CRITICAL: Invalid API Key format.")
                }
                model = GenerativeModel(
                    modelName = "gemini-1.5-flash-001",
                    apiKey = apiKey,
                    generationConfig = config
                )
            } catch (e: Exception) {
                Log.e("GeminiProvider", "Failed to init GenerativeModel: ${e.message}")
            }
        }
    }

    override suspend fun initialize() {
        ensureInitialized()
    }

    override suspend fun generate(request: AIRequest): AIResponse {
        val startTime = System.currentTimeMillis()
        ensureInitialized()
        
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
                
                // Interaction 2.0: Multi-turn aware request using chat session
                val history = request.history.map { msg ->
                    content(if (msg.isUser) "user" else "model") {
                        text(msg.text)
                    }
                }
                
                val chat = model?.startChat(history)
                
                val response = if (request.imageData != null) {
                    val bitmap = BitmapFactory.decodeByteArray(request.imageData, 0, request.imageData.size)
                    val content = content {
                        image(bitmap)
                        text(request.prompt)
                    }
                    model?.generateContent(content)
                } else {
                    chat?.sendMessage(request.prompt)
                }
                
                val text = response?.text ?: ""
                val responseEndTime = System.currentTimeMillis()
                
                android.util.Log.d("GeminiProvider", "[AI_RESPONSE_RAW] latency=${responseEndTime - responseStartTime}ms")
                
                if (text.isBlank()) {
                    throw Exception("Empty response from model")
                }

                return AIResponse(
                    text = text,
                    provider = id,
                    model = "gemini-1.5-flash-001",
                    requestId = request.requestId,
                    generationTimeMs = System.currentTimeMillis() - startTime
                )
            } catch (e: Exception) {
                lastException = e
                android.util.Log.w("GeminiProvider", "Generation attempt ${retryCount + 1} failed: ${e.message}")
                
                // Classify Gemini SDK exceptions
                if (e is ResponseStoppedException ||
                    e.message?.contains("SAFETY") == true || 
                    e.message?.contains("blocked") == true) {
                    // Content filtered - do not retry
                    break
                }
                
                if (e.message?.contains("429") == true || 
                    e.message?.contains("quota") == true || 
                    e.message?.contains("overloaded") == true) {
                    // Rate limit or overloaded - back off longer
                    kotlinx.coroutines.delay(2000L * (retryCount + 1))
                }
                retryCount++
            }
        }

        android.util.Log.e("GeminiProvider", "All generation attempts failed. Last error: ${lastException?.message}")
        val errorCategory = when {
            lastException?.message?.contains("429") == true || 
            lastException?.message?.contains("quota") == true ||
            lastException?.message?.contains("overloaded") == true -> AIError.RATE_LIMIT
            lastException?.message?.contains("network") == true -> AIError.NETWORK_ERROR
            lastException?.message?.contains("auth") == true || 
            lastException is InvalidAPIKeyException -> AIError.AUTHENTICATION_ERROR
            else -> AIError.UNKNOWN_ERROR
        }

        return AIResponse(
            text = "Google Gemini link failed.",
            provider = id,
            requestId = request.requestId,
            error = lastException?.localizedMessage ?: "Unknown error",
            errorCategory = errorCategory,
            generationTimeMs = System.currentTimeMillis() - startTime
        )
    }


    override fun stream(request: AIRequest): Flow<String> {
        ensureInitialized()
        
        return if (request.imageData != null) {
            val bitmap = BitmapFactory.decodeByteArray(request.imageData, 0, request.imageData.size)
            val content = content {
                image(bitmap)
                text(request.prompt)
            }
            model?.generateContentStream(content)?.mapNotNull { it.text } ?: emptyFlow()
        } else {
            model?.generateContentStream(request.prompt)?.mapNotNull { it.text } ?: emptyFlow()
        }
    }

    override fun cancel() { }

    override fun release() {
        model = null
    }
}
