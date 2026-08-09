package com.humanoidai.embodiment

import com.humanoidai.ui.layoutcustomization.domain.perception.PerceptionState

/**
 * The unified world state as perceived by the active embodiment.
 */
data class WorldState(
    val activeEmbodimentId: String,
    val identityId: String = "humanoid_default",
    val connectedEmbodimentIds: Set<String>,
    val remoteInstances: List<String> = emptyList(), // instance IDs
    val environment: EnvironmentModel = EnvironmentModel(),
    val perception: PerceptionState = PerceptionState(),
    val availableCapabilities: Set<Capability> = emptySet(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * High-level model of the surrounding physical environment.
 */
data class EnvironmentModel(
    val lightingLevel: Float = 1.0f, // 0.0 to 1.0
    val backgroundNoiseLevel: Float = 0.0f, // 0.0 to 1.0
    val activeUserPresent: Boolean = false,
    val detectedObjectCount: Int = 0,
    val thermalPressure: Int = 0 // PowerManager.THERMAL_STATUS_*
)
