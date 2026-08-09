package com.humanoidai.ui.layoutcustomization.presentation.event

import com.humanoidai.ui.layoutcustomization.domain.model.*

/**
 * Lifecycle-aware UI events for triggering state updates in the engine.
 */
sealed class LayoutCustomizationEvent {
    data class ChangeCameraScale(val scale: Float) : LayoutCustomizationEvent()
    data class ChangeCameraOffset(val x: Float, val y: Float) : LayoutCustomizationEvent()
    data class ChangePrimaryROIPosition(val x: Float, val y: Float, val auto: Boolean = false) : LayoutCustomizationEvent()
    data class ChangeSecondaryROIPosition(val x: Float, val y: Float, val auto: Boolean = false) : LayoutCustomizationEvent()
    data class ChangeWidgetDensity(val density: String) : LayoutCustomizationEvent()
    data class ChangeWidgetArrangement(val arrangement: String) : LayoutCustomizationEvent()
    data class ToggleWidgetVisibility(val widgetId: String, val visible: Boolean) : LayoutCustomizationEvent()
    data class ChangeMotionProfile(val profile: String) : LayoutCustomizationEvent()
    data class ChangeSelectedLayout(val layoutId: String) : LayoutCustomizationEvent()
    
    object ResetCustomization : LayoutCustomizationEvent()

    object RestoreDefaults : LayoutCustomizationEvent()
}
