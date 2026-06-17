package com.humanoidai.ai

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * A fake AI provider for testing and offline development
 */
class MockProvider : AIProvider {
    override val id: String = "MockAI-1.0"

    override suspend fun initialize() {
        delay(500) // Simulate loading
    }

    override suspend fun generate(request: AIRequest): AIResponse {
        val startTime = System.currentTimeMillis()
        delay(1500) // Simulate network/inference latency
        
        val text = when {
            request.prompt.contains("Who is in the room", ignoreCase = true) -> 
                "I currently see the owner and one other individual. I'm monitoring for any changes."
            request.prompt.contains("Battery", ignoreCase = true) -> 
                "Your battery is currently stable. I'll let you know if it drops significantly."
            else -> "I understand. I am continuing to monitor the environment and will provide proactive assistance as needed."
        }

        return AIResponse(
            text = text,
            provider = id,
            generationTimeMs = System.currentTimeMillis() - startTime
        )
    }

    override fun stream(request: AIRequest): Flow<String> = flow {
        val words = "This is a simulated streaming response from the Mock AI provider.".split(" ")
        for (word in words) {
            emit("$word ")
            delay(100)
        }
    }

    override fun cancel() { }

    override fun release() { }
}
