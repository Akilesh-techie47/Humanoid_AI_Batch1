package com.humanoidai.ai

/**
 * Provider-independent error categories for AI operations.
 */
enum class AIError {
    NETWORK_ERROR,
    AUTHENTICATION_ERROR,
    RATE_LIMIT,
    MODEL_UNAVAILABLE,
    PROVIDER_UNAVAILABLE,
    INVALID_REQUEST,
    TIMEOUT,
    CANCELLED,
    SERVER_ERROR,
    UNSUPPORTED_CAPABILITY,
    UNKNOWN_ERROR
}
