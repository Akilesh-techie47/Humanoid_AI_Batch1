package com.humanoidai.embodiment

import kotlinx.coroutines.flow.StateFlow

/**
 * Common interface for all display/visual feedback actuators.
 */
interface DisplayActuator {
    val status: StateFlow<HardwareStatus>
    fun showOverlay(id: String, content: Any?)
    fun hideOverlay(id: String)
    fun setBrightness(level: Float)
}

/**
 * Common interface for all audio output actuators.
 */
interface SpeakerActuator {
    val status: StateFlow<HardwareStatus>
    fun speak(text: String, priority: Int, onComplete: () -> Unit)
    fun stop()
    fun setVolume(level: Float)
}

/**
 * Common interface for all physical movement actuators.
 */
interface PhysicalActuator {
    val status: StateFlow<HardwareStatus>
    fun move(dx: Float, dy: Float, dz: Float)
    fun rotate(roll: Float, pitch: Float, yaw: Float)
}
