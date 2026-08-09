package com.humanoidai.ui.layoutcustomization.presentation.state

import androidx.compose.runtime.Immutable
import com.humanoidai.ui.layoutcustomization.domain.model.*

/**
 * Root immutable state object for Layout Customization.
 * Aggregates all sub-states into a single source of truth for the UI.
 */
@Immutable
data class LayoutCustomizationState(
    val selectedLayout: String = "top_aperture",
    val selectedPreset: String = "jarvis",
    
    val camera: CameraSettings = CameraSettings(),
    val roi: ROISettings = ROISettings(),
    val widget: WidgetSettings = WidgetSettings(),
    val motion: MotionSettings = MotionSettings(),
    val adaptive: AdaptiveHUDSettings = AdaptiveHUDSettings(),
    
    val lastUpdated: Long = System.currentTimeMillis()
)
