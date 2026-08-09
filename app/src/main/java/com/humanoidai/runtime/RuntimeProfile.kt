package com.humanoidai.runtime

/**
 * Defines the operational constraints and targets for different system states.
 */
enum class RuntimeProfile {
    /**
     * Balanced performance and battery life. Target: 30-60 FPS camera, standard AI latency.
     */
    BALANCED,

    /**
     * Maximize AI throughput and responsiveness. Target: Max FPS, minimal AI latency.
     */
    PERFORMANCE,

    /**
     * Prioritize battery life. Target: Reduced AI refresh rates, simplified animations.
     */
    BATTERY_SAVER,

    /**
     * Unrestricted performance with detailed diagnostics.
     */
    DEVELOPER
}

data class ProfileConstraints(
    val maxAiInferenceFrequencyMs: Long,
    val targetCameraFps: Int,
    val allowHighComplexityAnimations: Boolean,
    val backgroundTaskFrequencyMs: Long,
    val enableAggressiveMemoryCleanup: Boolean
)

fun RuntimeProfile.getConstraints(): ProfileConstraints = when (this) {
    RuntimeProfile.BALANCED -> ProfileConstraints(
        maxAiInferenceFrequencyMs = 100L,
        targetCameraFps = 30,
        allowHighComplexityAnimations = true,
        backgroundTaskFrequencyMs = 5000L,
        enableAggressiveMemoryCleanup = false
    )
    RuntimeProfile.PERFORMANCE -> ProfileConstraints(
        maxAiInferenceFrequencyMs = 33L,
        targetCameraFps = 60,
        allowHighComplexityAnimations = true,
        backgroundTaskFrequencyMs = 2000L,
        enableAggressiveMemoryCleanup = false
    )
    RuntimeProfile.BATTERY_SAVER -> ProfileConstraints(
        maxAiInferenceFrequencyMs = 500L,
        targetCameraFps = 15,
        allowHighComplexityAnimations = false,
        backgroundTaskFrequencyMs = 15000L,
        enableAggressiveMemoryCleanup = true
    )
    RuntimeProfile.DEVELOPER -> ProfileConstraints(
        maxAiInferenceFrequencyMs = 0L,
        targetCameraFps = 60,
        allowHighComplexityAnimations = true,
        backgroundTaskFrequencyMs = 1000L,
        enableAggressiveMemoryCleanup = false
    )
}
