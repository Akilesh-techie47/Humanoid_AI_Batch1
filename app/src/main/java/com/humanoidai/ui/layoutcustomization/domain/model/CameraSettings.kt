package com.humanoidai.ui.layoutcustomization.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class CameraSettings(
    val scale: Float = 1.0f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val padding: Float = 0f,
    val rotationPreference: Float = 0f, // reserved
    val isLocked: Boolean = false,
    val borderStyle: String = "standard", // reserved
    val apertureGlow: Boolean = true, // reserved
    val pulseStyle: String = "subtle" // reserved
)
