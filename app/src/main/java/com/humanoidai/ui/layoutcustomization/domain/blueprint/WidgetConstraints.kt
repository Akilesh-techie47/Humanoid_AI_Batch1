package com.humanoidai.ui.layoutcustomization.domain.blueprint

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Priorities for layout resolution. Higher priority widgets get their preferred zones first.
 */
object LayoutPriority {
    const val CAMERA = 0 // Background layer
    const val PRIMARY_ROI = 5 // Background vision overlay
    const val SECONDARY_ROI = 8
    const val DECORATIVE = 10
    const val STATUS_INDICATORS = 50
    const val NOTIFICATIONS = 70
    const val ASSISTANT = 80
    const val ALERT_PANEL = 95
}

/**
 * Defines how a widget can be resized by the layout engine.
 */
enum class WidgetResizability {
    FIXED,    // Cannot be resized (e.g., Camera)
    FLEXIBLE, // Can resize within bounds (e.g., Assistant)
    ADAPTIVE  // Automatically resizes based on content/priority (e.g., ROI)
}

/**
 * Logical zones division of the HUD.
 */
enum class WidgetZone {
    TOP_INFO,
    CAMERA_Aperture,
    LEFT_UTILITY,
    RIGHT_UTILITY,
    BOTTOM_CONTROL,
    NOTIFICATION_STACK,
    ALERT_BANNER,
    RESERVED_EXPANSION
}

/**
 * Container for widget-specific layout constraints.
 */
data class WidgetConstraints(
    val priority: Int = LayoutPriority.DECORATIVE,
    val resizability: WidgetResizability = WidgetResizability.FIXED,
    val preferredAnchor: WidgetAnchor = WidgetAnchor.CENTER,
    val allowedAnchors: List<WidgetAnchor> = emptyList(),
    val minMargin: Dp = 8.dp,
    val preferredMargin: Dp = 16.dp,
    val maxExpansion: Float = 1.0f,
    val protectedZones: List<WidgetZone> = emptyList()
)
