package com.humanoidai.ai

/**
 * UI-friendly representation of the AI's current state
 */
sealed class AIState {
    object Idle : AIState()
    object Loading : AIState()
    data class Success(val response: AIResponse) : AIState()
    data class Error(val message: String) : AIState()
}
