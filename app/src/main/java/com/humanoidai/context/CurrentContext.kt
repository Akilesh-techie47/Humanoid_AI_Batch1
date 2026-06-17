package com.humanoidai.context

/**
 * Snapshot of the application and environment state for the Humanoid AI Companion.
 * Everything in the app updates this shared state.
 */
data class CurrentContext(
    val ownerName: String = "Unknown",
    val visiblePeople: List<String> = emptyList(),
    val primaryPerson: String? = null,
    val unknownCount: Int = 0,
    val time: String = "",
    val date: String = "",
    val batteryLevel: Int = -1,
    val cameraState: String = "Inactive",
    val currentAlerts: Int = 0,
    val environment: String = "Indoor",
    val activity: String = "Idle",
    val currentScreen: String = "None",
    val noiseLevel: String = "Quiet",
    val brightness: String = "Normal",
    val weather: String = "Unknown",
    val lastConversationSnippet: String? = null
)
