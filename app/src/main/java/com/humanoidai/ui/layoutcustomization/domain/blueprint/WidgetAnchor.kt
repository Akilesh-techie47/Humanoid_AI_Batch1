package com.humanoidai.ui.layoutcustomization.domain.blueprint

/**
 * Semantic anchors for normalized widget positioning.
 * Independent of screen size/pixels.
 */
enum class WidgetAnchor {
    TOP_START, TOP_CENTER, TOP_END,
    CENTER_START, CENTER, CENTER_END,
    BOTTOM_START, BOTTOM_CENTER, BOTTOM_END,
    
    // Adaptive Camera Anchors (Phase 1D)
    CAMERA_TOP, CAMERA_BOTTOM, CAMERA_LEFT, CAMERA_RIGHT, CAMERA_CENTER,

    ABS_COORDS // For pixel-perfect manual overrides
}

/**
 * Rendering layers (Z-Order) for HUD components.
 */
object WidgetLayer {
    const val BACKGROUND = 1
    const val GRID = 2
    const val CAMERA = 3
    const val ROI = 4
    const val ASSISTANT = 5
    const val NOTIFICATIONS = 6
    const val ALERTS = 7
    const val DIALOGS = 8
}
