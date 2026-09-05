package com.humanoidai.ui.layoutcustomization.domain.hud

import androidx.compose.ui.unit.dp
import com.humanoidai.ui.layoutcustomization.domain.blueprint.*

/**
 * Centralized registry of all supported HUD Components.
 * Part of Phase 2B.
 */
object HUDComponentRegistry {
    
    val CAMERA = "camera"
    val PRIMARY_ROI = "primary_roi"
    val SECONDARY_ROI = "secondary_roi"
    val ASSISTANT = "assistant"
    val BOTTOM_DOCK = "bottom_dock"
    val STATUS_BAR = "status_bar"
    val ALERTS = "alerts"
    val NOTIFICATIONS = "notifications"
    val FPS_COUNTER = "fps_counter"
    val RECORDING_INDICATOR = "recording_indicator"
    val AI_THINKING = "ai_thinking"
    val QUICK_ACTIONS = "quick_actions"
    val RADAR = "radar"
    val STATUS_INDICATORS = "status_indicators"

    /**
     * Returns the default blueprint for a given component.
     */
    fun getComponentDefaults(id: String): WidgetBlueprint {
        return when (id) {
            CAMERA -> WidgetBlueprint(
                id = CAMERA,
                type = "aperture",
                priority = LayoutPriority.CAMERA,
                category = HUDComponentCategory.PERMANENT,
                behaviors = HUDComponentBehavior(canMove = false, canResize = false, canHide = false),
                allowedZones = listOf(WidgetZone.CAMERA_Aperture)
            )
            PRIMARY_ROI -> WidgetBlueprint(
                id = PRIMARY_ROI,
                type = "detection",
                priority = LayoutPriority.DECORATIVE, // Low priority to prevent touch blocking
                category = HUDComponentCategory.DETECTION,
                behaviors = HUDComponentBehavior(canMove = true, canResize = true, canFloat = true),
                allowedZones = listOf(WidgetZone.CAMERA_Aperture)
            )
            ASSISTANT -> WidgetBlueprint(
                id = ASSISTANT,
                type = "ai",
                priority = LayoutPriority.ASSISTANT,
                category = HUDComponentCategory.CONTEXT,
                behaviors = HUDComponentBehavior(canMove = true, canResize = true, canCollapse = true, canFloat = true),
                allowedZones = listOf(WidgetZone.BOTTOM_CONTROL, WidgetZone.LEFT_UTILITY, WidgetZone.RIGHT_UTILITY)
            )
            BOTTOM_DOCK -> WidgetBlueprint(
                id = BOTTOM_DOCK,
                type = "dock",
                priority = LayoutPriority.NOTIFICATIONS,
                category = HUDComponentCategory.PERMANENT,
                behaviors = HUDComponentBehavior(canMove = false, canResize = false, canHide = false),
                allowedZones = listOf(WidgetZone.BOTTOM_CONTROL)
            )
            ALERTS -> WidgetBlueprint(
                id = ALERTS,
                type = "system",
                priority = LayoutPriority.ALERT_PANEL,
                category = HUDComponentCategory.CONTEXT,
                behaviors = HUDComponentBehavior(canMove = false, canAnimate = true),
                allowedZones = listOf(WidgetZone.ALERT_BANNER, WidgetZone.TOP_INFO)
            )
            FPS_COUNTER -> WidgetBlueprint(
                id = FPS_COUNTER,
                type = "debug",
                priority = LayoutPriority.DECORATIVE,
                category = HUDComponentCategory.DEVELOPER,
                isVisible = false,
                allowedZones = listOf(WidgetZone.TOP_INFO)
            )
            AI_THINKING -> WidgetBlueprint(
                id = AI_THINKING,
                type = "ai",
                anchor = WidgetAnchor.TOP_CENTER,
                offsetY = 120.dp,
                priority = LayoutPriority.STATUS_INDICATORS,
                category = HUDComponentCategory.CONTEXT,
                isVisible = true, // Transient visibility handled by renderer content
                allowedZones = listOf(WidgetZone.CAMERA_Aperture)
            )

            SECONDARY_ROI -> WidgetBlueprint(
                id = SECONDARY_ROI,
                type = "detection",
                anchor = WidgetAnchor.CENTER_START,
                offsetX = 16.dp,
                priority = LayoutPriority.SECONDARY_ROI,
                category = HUDComponentCategory.DETECTION,
                allowedZones = listOf(WidgetZone.LEFT_UTILITY, WidgetZone.RIGHT_UTILITY)
            )
            RADAR -> WidgetBlueprint(
                id = RADAR,
                type = "sensor",
                anchor = WidgetAnchor.TOP_END,
                offsetX = (-16).dp,
                offsetY = 16.dp,
                priority = LayoutPriority.STATUS_INDICATORS,
                category = HUDComponentCategory.CONTEXT,
                allowedZones = listOf(WidgetZone.RIGHT_UTILITY, WidgetZone.LEFT_UTILITY)
            )
            NOTIFICATIONS -> WidgetBlueprint(
                id = NOTIFICATIONS,
                type = "system",
                anchor = WidgetAnchor.BOTTOM_START,
                offsetX = 20.dp,
                offsetY = (-120).dp,
                priority = LayoutPriority.NOTIFICATIONS,
                category = HUDComponentCategory.CONTEXT,
                allowedZones = listOf(WidgetZone.LEFT_UTILITY, WidgetZone.RIGHT_UTILITY)
            )
            QUICK_ACTIONS -> WidgetBlueprint(
                id = QUICK_ACTIONS,
                type = "control",
                priority = LayoutPriority.DECORATIVE,
                category = HUDComponentCategory.PERMANENT,
                allowedZones = listOf(WidgetZone.BOTTOM_CONTROL)
            )
            STATUS_INDICATORS -> WidgetBlueprint(
                id = STATUS_INDICATORS,
                type = "system",
                priority = LayoutPriority.STATUS_INDICATORS,
                category = HUDComponentCategory.PERMANENT,
                allowedZones = listOf(WidgetZone.TOP_INFO, WidgetZone.RIGHT_UTILITY)
            )
            STATUS_BAR -> WidgetBlueprint(
                id = STATUS_BAR,
                type = "system",
                priority = LayoutPriority.STATUS_INDICATORS,
                category = HUDComponentCategory.PERMANENT,
                behaviors = HUDComponentBehavior(canMove = false, canHide = false),
                allowedZones = listOf(WidgetZone.TOP_INFO)
            )
            else -> WidgetBlueprint(id = id)
        }
    }
}


