package com.humanoidai.embodiment

import android.util.Log

/**
 * Validates physical actions against safety and security constraints.
 */
class SafetyController(private val registry: CapabilityRegistry) {

    companion object {
        private const val TAG = "SafetyController"
    }

    /**
     * Validates an action request. Returns true if safe to execute.
     */
    fun validateAction(request: ActionRequest): Boolean {
        // 1. Check Capability
        val requiredCapability = mapActionToCapability(request)
        if (requiredCapability != null && !registry.hasCapability(requiredCapability)) {
            Log.e(TAG, "Rejected: Embodiment lacks required capability: $requiredCapability")
            return false
        }

        // 2. Physical Safety Constraints
        return when (request) {
            is ActionRequest.Move -> {
                // Limit maximum displacement for safety
                request.dx in -10f..10f && request.dy in -10f..10f && request.dz in -10f..10f
            }
            is ActionRequest.Rotate -> {
                // Ensure rotation is within logical bounds
                request.pitch in -90f..90f
            }
            else -> true // Informational/Visual actions are generally safe
        }
    }

    private fun mapActionToCapability(request: ActionRequest): Capability? {
        return when (request) {
            is ActionRequest.Speak -> Capability.AUDIO_OUTPUT
            is ActionRequest.DisplayOverlay -> Capability.DISPLAY
            is ActionRequest.Move -> Capability.LOCOMOTION
            is ActionRequest.Rotate -> Capability.LOCOMOTION
            is ActionRequest.Vibrate -> Capability.HAPTICS
            is ActionRequest.Flash -> Capability.ILLUMINATION
        }
    }
}
