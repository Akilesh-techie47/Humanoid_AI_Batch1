package com.humanoidai.embodiment

import kotlinx.coroutines.flow.StateFlow

/**
 * Common interface for all visual sensors.
 */
interface VisionSensor {
    val status: StateFlow<HardwareStatus>
    fun startCapture()
    fun stopCapture()
    // Returns frame data or URI (abstracted for now)
    fun getLatestFrame(): Any? 
}

/**
 * Common interface for all audio sensors.
 */
interface AudioSensor {
    val status: StateFlow<HardwareStatus>
    fun startListening()
    fun stopListening()
    fun getIntensity(): Float
}

/**
 * Common interface for all motion/inertial sensors.
 */
interface MotionSensor {
    val status: StateFlow<HardwareStatus>
    fun getAcceleration(): FloatArray // x, y, z
    fun getRotation(): FloatArray // roll, pitch, yaw
}
