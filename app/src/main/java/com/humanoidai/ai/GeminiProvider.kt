package com.humanoidai.ai

import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * AI Provider implementation for Google Gemini
 */
class GeminiProvider(private val apiKey: String) : AIProvider {
    override val id: String = "Gemini 1.5 Flash"
    
    private var model: GenerativeModel? = null

    override suspend fun initialize() {
        try {
            if (model == null) {
                model = GenerativeModel(
                    modelName = "gemini-1.5-flash",
                    apiKey = apiKey
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("GeminiProvider", "Failed to init GenerativeModel: ${e.message}")
        }
    }

    override suspend fun generate(request: AIRequest): AIResponse {
        val startTime = System.currentTimeMillis()
        initialize() // Ensure initialized
        
        return try {
            val response = model?.generateContent(request.prompt)
            AIResponse(
                text = response?.text ?: "No response from Gemini.",
                provider = id,
                generationTimeMs = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            AIResponse(
                text = "Connection error: ${e.localizedMessage}",
                provider = id,
                error = e.localizedMessage,
                generationTimeMs = System.currentTimeMillis() - startTime
            )
        }
    }

    override fun stream(request: AIRequest): Flow<String> = emptyFlow()

    override fun cancel() { }

    override fun release() {
        model = null
    }
}
