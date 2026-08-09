package com.humanoidai.ui.layoutcustomization.domain.validator

import android.util.Log
import com.humanoidai.ui.layoutcustomization.domain.blueprint.LayoutBlueprint
import com.humanoidai.ui.layoutcustomization.presentation.state.LayoutCustomizationState

/**
 * Validates state updates and final blueprints before they are committed or rendered.
 * Ensures the AI Operating System remains stable and visually correct.
 */
class LayoutCustomizationValidator {
    
    companion object {
        private const val TAG = "LayoutCustomization"
        private const val MIN_SCALE = 0.5f
        private const val MAX_SCALE = 2.0f
        private const val MAX_OFFSET = 1000f 
    }

    /**
     * Validates and repairs the incoming state to ensure safety.
     */
    fun validate(state: LayoutCustomizationState): LayoutCustomizationState {
        var validated = state
        
        // 1. Camera Scale Validation
        if (state.camera.scale < MIN_SCALE || state.camera.scale > MAX_SCALE) {
            Log.w(TAG, "Validation Correction: Camera Scale out of bounds (${state.camera.scale})")
            validated = validated.copy(
                camera = validated.camera.copy(scale = state.camera.scale.coerceIn(MIN_SCALE, MAX_SCALE))
            )
        }
        
        // 2. Camera Offset Validation
        if (Math.abs(state.camera.offsetX) > MAX_OFFSET || Math.abs(state.camera.offsetY) > MAX_OFFSET) {
            Log.w(TAG, "Validation Correction: Camera Offset exceeds safe bounds")
            validated = validated.copy(
                camera = validated.camera.copy(
                    offsetX = state.camera.offsetX.coerceIn(-MAX_OFFSET, MAX_OFFSET),
                    offsetY = state.camera.offsetY.coerceIn(-MAX_OFFSET, MAX_OFFSET)
                )
            )
        }
        
        // 3. Motion Profile Fallback
        val validProfiles = listOf("instant", "minimal", "smooth", "cinematic", "professional")
        if (state.motion.profile.lowercase() !in validProfiles) {
            Log.w(TAG, "Validation Correction: Invalid Motion Profile (${state.motion.profile}), defaulting to Professional")
            validated = validated.copy(
                motion = validated.motion.copy(profile = "professional")
            )
        }

        return validated
    }

    /**
     * Validates a generated blueprint to ensure no critical UI overlaps or out-of-bounds.
     */
    fun validateBlueprint(blueprint: LayoutBlueprint): LayoutBlueprint {
        Log.d(TAG, "Validator: Checking blueprint safety for ${blueprint.layoutId}")
        
        // Phase 1C Placeholder: Ensure camera widget exists
        if (blueprint.camera == null) {
            Log.e(TAG, "Validator: ERROR - Missing camera definition in blueprint!")
        }
        
        return blueprint
    }
}
