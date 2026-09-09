package com.humanoidai.ai

import android.util.Log
import com.humanoidai.ui.customization.AIMode
import com.humanoidai.ui.customization.AppearanceSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import java.io.IOException

/**
 * Orchestrates AI requests through the router and providers.
 * Handles failover, latency measurement, and request lifecycle.
 */
class AIOrchestrator(
    private val router: AIRouter
) {

    suspend fun generate(request: AIRequest, settings: AppearanceSettings): AIResponse {
        var provider = router.route(request, settings)
        Log.i("AIOrchestrator", "[AI_REQUEST] provider=${provider.id} prompt=\"${request.prompt.take(50)}...\"")
        
        val startTime = System.currentTimeMillis()
        var response = provider.generate(request)
        
        // Handle failure and retry with fallback
        if (response.error != null) {
            router.reportFailure(provider.id, response.errorCategory ?: AIError.UNKNOWN_ERROR, response.metadata)
            
            // Intelligent Fallback
            val fallbackProvider = router.routeCloudFallback(settings.cloudPriority, provider.id)
            if (fallbackProvider != null) {
                Log.w("AIOrchestrator", "[AI_FAILOVER] ${provider.id} failed, attempting ${fallbackProvider.id} fallback")
                provider = fallbackProvider
                response = fallbackProvider.generate(request)
            }
        }

        val duration = System.currentTimeMillis() - startTime
        Log.i("AIOrchestrator", "[AI_RESPONSE] provider=${response.provider} duration=${duration}ms success=${response.error == null}")
        
        return response
    }

    fun stream(request: AIRequest, settings: AppearanceSettings): Flow<String> = flow {
        var provider = router.route(request, settings)
        Log.i("AIOrchestrator", "[AI_STREAM_START] provider=${provider.id} id=${request.requestId}")

        try {
            provider.stream(request).collect { emit(it) }
        } catch (e: Exception) {
            Log.w("AIOrchestrator", "[AI_STREAM_ERROR] ${provider.id} failed, checking failover: ${e.message}")
            
            val errorCategory = when {
                e is IOException -> AIError.NETWORK_ERROR
                else -> AIError.UNKNOWN_ERROR
            }
            
            router.reportFailure(provider.id, errorCategory)

            val fallbackProvider = router.routeCloudFallback(settings.cloudPriority, provider.id)
            if (fallbackProvider != null) {
                Log.i("AIOrchestrator", "[AI_FAILOVER_STREAM] Switching to ${fallbackProvider.id}")
                fallbackProvider.stream(request).collect { emit(it) }
                return@flow
            }
            throw e
        }
    }.onStart { 
        Log.d("AIOrchestrator", "[AI_STREAM_COLLECT_START] id=${request.requestId}") 
    }.onCompletion { 
        Log.i("AIOrchestrator", "[AI_STREAM_COLLECT_END] id=${request.requestId}") 
    }

    private fun mapRequestToCapability(request: AIRequest): AICapability {
        return when {
            request.requiresVision || request.type == AIRequestType.VISION -> AICapability.VISION
            request.requiresReasoning || request.type == AIRequestType.REASONING -> AICapability.REASONING
            request.requiresCoding || request.type == AIRequestType.CODING -> AICapability.CODING
            else -> AICapability.TEXT
        }
    }
}
