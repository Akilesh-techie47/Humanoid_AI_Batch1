package com.humanoidai.ui.layoutcustomization.domain.model

import com.humanoidai.ui.layoutcustomization.presentation.state.LayoutCustomizationState

/**
 * Default values and factory for the initial state of the customization engine.
 */
object LayoutCustomizationDefaults {
    
    fun getInitialState() = LayoutCustomizationState()
    
    // Default categories for DataStore namespaces
    const val NS_CAMERA = "camera"
    const val NS_ROI = "roi"
    const val NS_WIDGET = "widget"
    const val NS_MOTION = "motion"
    const val NS_ADAPTIVE = "adaptive"
}
