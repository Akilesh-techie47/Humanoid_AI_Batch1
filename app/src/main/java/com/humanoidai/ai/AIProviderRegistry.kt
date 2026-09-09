package com.humanoidai.ai

/**
 * Central registry for all AI providers.
 */
class AIProviderRegistry {
    private val providers = mutableMapOf<String, AIProvider>()

    fun register(provider: AIProvider) {
        providers[provider.id] = provider
    }

    fun get(id: String): AIProvider? = providers[id]

    fun getAll(): List<AIProvider> = providers.values.toList()
}
