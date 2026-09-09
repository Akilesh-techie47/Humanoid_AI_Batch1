package com.humanoidai.ai

import android.util.Log
import com.humanoidai.ui.customization.AIMode
import com.humanoidai.ui.customization.AppearanceSettings

/**
 * Logic for provider selection based on availability and capabilities.
 */
class AIRouter(private val registry: AIProviderRegistry) {

    private val providerHealth = mutableMapOf<String, ProviderHealthState>()

    data class ProviderHealthState(
        var status: ProviderStatus = ProviderStatus.AVAILABLE,
        var retryAt: Long = 0L,
        var lastError: String? = null,
        var failureCount: Int = 0
    )

    enum class ProviderStatus {
        AVAILABLE,
        RATE_LIMITED,
        UNAVAILABLE,
        DEGRADED
    }

    /**
     * Selects the best available provider for the given request.
     */
    suspend fun route(request: AIRequest, settings: AppearanceSettings): AIProvider {
        Log.d("AIRouter", "[AI_ROUTER] Routing request ${request.requestId} (Mode: ${settings.aiMode})")

        val ollama = registry.get("ollama-local") as? OllamaProvider
        val groq = registry.get("groq-cloud") as? GroqProvider
        val openRouter = registry.get("openrouter-cloud") as? OpenRouterProvider

        // Sync configs from settings
        ollama?.updateConfig(settings.ollamaEndpoint, settings.ollamaModel)
        groq?.updateConfig(settings.groqModel)
        openRouter?.updateConfig(settings.openrouterModel)

        return when (settings.aiMode) {
            AIMode.LOCAL_ONLY -> {
                if (isProviderHealthy("ollama-local") && ollama != null && ollama.isAvailable()) ollama
                else registry.get("offline-local") ?: registry.get("mock-provider")!!
            }
            AIMode.CLOUD_ONLY -> {
                selectBestCloudProvider(settings.cloudPriority)
            }
            AIMode.AUTOMATIC -> {
                // Prefer Ollama if available and healthy
                if (isProviderHealthy("ollama-local") && ollama != null && ollama.isAvailable()) {
                    Log.d("AIRouter", "[AI_ROUTER] Selected Local: ${ollama.id}")
                    ollama
                } else {
                    selectBestCloudProvider(settings.cloudPriority)
                }
            }
        }
    }

    private suspend fun selectBestCloudProvider(priority: List<String>): AIProvider {
        // 1. Try priority list first
        for (providerId in priority) {
            val provider = registry.get(providerId)
            if (provider != null && provider.isAvailable() && isProviderHealthy(providerId)) {
                Log.d("AIRouter", "[AI_ROUTER] Selected priority provider: $providerId")
                return provider
            }
        }
        
        // 2. Fallback to any healthy cloud provider
        val allCloud = listOf("groq-cloud", "gemini-1.5-flash", "openrouter-cloud")
        for (providerId in allCloud) {
            if (priority.contains(providerId)) continue
            val provider = registry.get(providerId)
            if (provider != null && provider.isAvailable() && isProviderHealthy(providerId)) {
                Log.w("AIRouter", "[AI_ROUTER] Priority failed, selecting fallback: $providerId")
                return provider
            }
        }

        return registry.get("offline-local") ?: registry.get("mock-provider")!!
    }

    fun getProviderStatus(providerId: String): ProviderStatus {
        val health = providerHealth[providerId] ?: return ProviderStatus.AVAILABLE
        if (health.status == ProviderStatus.AVAILABLE) return ProviderStatus.AVAILABLE
        
        if (System.currentTimeMillis() > health.retryAt) {
            Log.i("AIRouter", "[AI_ROUTER] Recovery time reached for $providerId")
            health.status = ProviderStatus.AVAILABLE
            return ProviderStatus.AVAILABLE
        }
        return health.status
    }

    private fun isProviderHealthy(providerId: String): Boolean {
        return getProviderStatus(providerId) == ProviderStatus.AVAILABLE
    }

    fun reportFailure(providerId: String, error: AIError, metadata: Map<String, Any> = emptyMap()) {
        val health = providerHealth.getOrPut(providerId) { ProviderHealthState() }
        health.failureCount++
        
        when (error) {
            AIError.RATE_LIMIT -> {
                // Try to get retry-after from metadata, default to 60s
                val retryAfter = metadata["retryAfter"]?.toString()?.toLongOrNull() ?: 60L
                health.status = ProviderStatus.RATE_LIMITED
                health.retryAt = System.currentTimeMillis() + (retryAfter * 1000L)
                Log.e("AIRouter", "[AI_LIMIT] $providerId rate limited. Retry after ${retryAfter}s")
            }
            AIError.NETWORK_ERROR, AIError.TIMEOUT -> {
                health.status = ProviderStatus.DEGRADED
                val backoff = (10000L * health.failureCount).coerceAtMost(60000L)
                health.retryAt = System.currentTimeMillis() + backoff
                Log.w("AIRouter", "[AI_HEALTH] $providerId network error. Backoff: ${backoff}ms")
            }
            AIError.AUTHENTICATION_ERROR -> {
                health.status = ProviderStatus.UNAVAILABLE
                health.retryAt = Long.MAX_VALUE // Needs user action
                Log.e("AIRouter", "[AI_HEALTH] $providerId auth failed. Disabled.")
            }
            else -> {
                health.status = ProviderStatus.DEGRADED
                health.retryAt = System.currentTimeMillis() + 10000L
            }
        }
    }

    /**
     * Finds the next best cloud provider if the current one fails.
     */
    suspend fun routeCloudFallback(priority: List<String>, failedProviderId: String): AIProvider? {
        Log.w("AIRouter", "[AI_FAILOVER] Provider $failedProviderId failed. Finding next best...")
        // Just reuse the selection logic but exclude the one that just failed
        val filteredPriority = priority.filter { it != failedProviderId }
        val next = selectBestCloudProvider(filteredPriority)
        
        // Return null only if we've exhausted all options and hit mock
        return if (next.id == "mock-provider" && failedProviderId != "mock-provider") null else next
    }
}
