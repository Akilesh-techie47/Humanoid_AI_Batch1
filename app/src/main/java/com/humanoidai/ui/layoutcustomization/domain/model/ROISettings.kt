package com.humanoidai.ui.layoutcustomization.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class ROISettings(
    val primaryPositionX: Float = 0f,
    val primaryPositionY: Float = 0f,
    val primaryAutoPosition: Boolean = true,
    val secondaryPositionX: Float = 0f,
    val secondaryPositionY: Float = 0f,
    val secondaryAutoPosition: Boolean = true,
    val size: Float = 1.0f,
    val autoResize: Boolean = true,
    val cornerStyle: String = "rounded", // reserved
    val glow: Boolean = true, // reserved
    val animationMode: String = "smooth" // reserved
)
