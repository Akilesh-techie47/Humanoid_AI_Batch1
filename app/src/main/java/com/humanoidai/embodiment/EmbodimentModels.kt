package com.humanoidai.embodiment

/**
 * Supported types of AI physical manifestations.
 */
enum class EmbodimentType {
    PHONE,
    ROBOT,
    DRONE,
    GLASSES,
    DESKTOP,
    IOT_DEVICE,
    SIMULATED
}

/**
 * Specific hardware or software capabilities.
 */
enum class Capability {
    VISION,
    AUDIO_INPUT,
    AUDIO_OUTPUT,
    DISPLAY,
    LOCOMOTION,
    MANIPULATION,
    HAPTICS,
    ILLUMINATION,
    CONNECTIVITY_WIFI,
    CONNECTIVITY_BLUETOOTH,
    GPS
}

/**
 * Operational status of a hardware component.
 */
enum class HardwareStatus {
    HEALTHY,
    DEGRADED,
    ERROR,
    DISCONNECTED,
    UNAVAILABLE
}

/**
 * Profile defining an embodiment's characteristics.
 */
data class EmbodimentProfile(
    val id: String,
    val type: EmbodimentType,
    val capabilities: Set<Capability>,
    val manufacturer: String,
    val model: String,
    val performanceClass: PerformanceClass = PerformanceClass.MEDIUM
)

enum class PerformanceClass {
    LOW,
    MEDIUM,
    HIGH,
    EXTREME
}

/**
 * Standard request for a physical action.
 */
sealed class ActionRequest {
    data class Speak(val text: String, val priority: Int = 50) : ActionRequest()
    data class Vibrate(val pattern: LongArray) : ActionRequest()
    data class Flash(val on: Boolean) : ActionRequest()
    data class DisplayOverlay(val componentId: String, val visible: Boolean) : ActionRequest()
    data class Move(val dx: Float, val dy: Float, val dz: Float) : ActionRequest() // Forward, Right, Up
    data class Rotate(val roll: Float, val pitch: Float, val yaw: Float) : ActionRequest()
}
