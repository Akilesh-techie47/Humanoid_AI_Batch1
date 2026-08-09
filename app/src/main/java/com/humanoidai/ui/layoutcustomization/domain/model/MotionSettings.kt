package com.humanoidai.ui.layoutcustomization.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class MotionSettings(
    val profile: String = "professional", // professional, aggressive, cinematic
    val durationMs: Int = 300,
    val springStrength: Float = 1.0f,
    val isEnabled: Boolean = true
)
