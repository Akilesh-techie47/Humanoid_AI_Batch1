package com.humanoidai.bdi

/**
 * A Belief represents something the AI thinks is true about the world.
 */
data class Belief(
    val key: String,
    val value: Any,
    val certainty: Float = 1.0f
)

/**
 * A Desire represents a high-level goal the AI wants to achieve.
 */
enum class Desire {
    MAINTAIN_SECURITY,
    SOCIAL_INTERACTION,
    SYSTEM_MAINTENANCE,
    USER_ASSISTANCE,
    ENVIRONMENT_MONITORING,
    IDLE
}

/**
 * An Intention is a concrete action the AI has decided to take.
 */
data class Intention(
    val desire: Desire,
    val action: () -> Unit,
    val priority: Int
)
