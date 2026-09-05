package com.humanoidai.ui.layoutcustomization.domain.manager

import androidx.compose.ui.unit.dp
import com.humanoidai.ui.layoutcustomization.domain.blueprint.*
import com.humanoidai.ui.layoutcustomization.domain.hud.HUDComponentRegistry

/**
 * Registry of all available widgets and their default structural positions.
 * Enhanced for Phase 2B to support all 8 Structural Layouts.
 */
object LayoutRegistry {

    fun getDefaultBlueprint(layoutId: String): LayoutBlueprint {
        val widgets = mutableMapOf<String, WidgetBlueprint>()
        
        when (layoutId.uppercase()) {
            "TOP_APERTURE" -> {
                widgets[HUDComponentRegistry.CAMERA] = HUDComponentRegistry.getComponentDefaults(HUDComponentRegistry.CAMERA).copy(
                    anchor = WidgetAnchor.CENTER,
                    offsetY = 0.dp,
                    width = 280.dp, // Large Hero Element
                    height = 280.dp
                )
                widgets[HUDComponentRegistry.STATUS_BAR] = HUDComponentRegistry.getComponentDefaults(HUDComponentRegistry.STATUS_BAR).copy(
                    anchor = WidgetAnchor.TOP_CENTER,
                    offsetY = 0.dp
                )
                widgets[HUDComponentRegistry.RADAR] = HUDComponentRegistry.getComponentDefaults(HUDComponentRegistry.RADAR).copy(
                    anchor = WidgetAnchor.TOP_END,
                    offsetX = (-20).dp,
                    offsetY = 60.dp
                )
                widgets[HUDComponentRegistry.ASSISTANT] = HUDComponentRegistry.getComponentDefaults(HUDComponentRegistry.ASSISTANT).copy(
                    anchor = WidgetAnchor.BOTTOM_CENTER,
                    offsetY = (-10).dp // Tighter to bottom
                )
                widgets[HUDComponentRegistry.ALERTS] = HUDComponentRegistry.getComponentDefaults(HUDComponentRegistry.ALERTS).copy(
                    anchor = WidgetAnchor.TOP_CENTER,
                    offsetY = 80.dp // Clearly below header identity
                )
                widgets[HUDComponentRegistry.STATUS_INDICATORS] = HUDComponentRegistry.getComponentDefaults(HUDComponentRegistry.STATUS_INDICATORS).copy(
                    isVisible = false // Clean UI, no technical metrics over center
                )
                widgets[HUDComponentRegistry.SECONDARY_ROI] = HUDComponentRegistry.getComponentDefaults(HUDComponentRegistry.SECONDARY_ROI).copy(
                    anchor = WidgetAnchor.CENTER,
                    offsetX = 0.dp,
                    offsetY = 0.dp
                )
                widgets[HUDComponentRegistry.NOTIFICATIONS] = HUDComponentRegistry.getComponentDefaults(HUDComponentRegistry.NOTIFICATIONS).copy(
                    anchor = WidgetAnchor.BOTTOM_START,
                    offsetX = 20.dp,
                    offsetY = (-140).dp,
                    isVisible = false // Move to secondary to reduce clutter
                )
            }
            "SIDE_SPLIT" -> {
                widgets[HUDComponentRegistry.CAMERA] = HUDComponentRegistry.getComponentDefaults(HUDComponentRegistry.CAMERA).copy(
                    anchor = WidgetAnchor.CENTER_START,
                    offsetX = 20.dp,
                    width = 180.dp,
                    height = 180.dp
                )
                widgets[HUDComponentRegistry.ASSISTANT] = HUDComponentRegistry.getComponentDefaults(HUDComponentRegistry.ASSISTANT).copy(
                    anchor = WidgetAnchor.BOTTOM_END,
                    offsetX = (-20).dp,
                    offsetY = (-140).dp
                )
            }
            "CORNER_TACTICAL" -> {
                widgets[HUDComponentRegistry.CAMERA] = HUDComponentRegistry.getComponentDefaults(HUDComponentRegistry.CAMERA).copy(
                    anchor = WidgetAnchor.TOP_END,
                    offsetX = (-16).dp,
                    offsetY = 80.dp,
                    width = 140.dp,
                    height = 140.dp
                )
            }
            "BOTTOM_FOCUS" -> {
                widgets[HUDComponentRegistry.CAMERA] = HUDComponentRegistry.getComponentDefaults(HUDComponentRegistry.CAMERA).copy(
                    anchor = WidgetAnchor.BOTTOM_CENTER,
                    offsetY = (-160).dp,
                    width = 220.dp,
                    height = 220.dp
                )
            }
            "CENTRAL_HUB" -> {
                widgets[HUDComponentRegistry.CAMERA] = HUDComponentRegistry.getComponentDefaults(HUDComponentRegistry.CAMERA).copy(
                    anchor = WidgetAnchor.CENTER,
                    width = 220.dp,
                    height = 220.dp
                )
            }
            "MINIMAL_FLOATING" -> {
                widgets[HUDComponentRegistry.CAMERA] = HUDComponentRegistry.getComponentDefaults(HUDComponentRegistry.CAMERA).copy(
                    anchor = WidgetAnchor.TOP_START,
                    offsetX = 20.dp,
                    offsetY = 80.dp,
                    width = 140.dp,
                    height = 140.dp
                )
            }
            "BLUEPRINT" -> {
                widgets[HUDComponentRegistry.CAMERA] = HUDComponentRegistry.getComponentDefaults(HUDComponentRegistry.CAMERA).copy(
                    anchor = WidgetAnchor.CENTER,
                    width = 300.dp,
                    height = 300.dp,
                    alpha = 0.8f
                )
                widgets[HUDComponentRegistry.STATUS_BAR] = HUDComponentRegistry.getComponentDefaults(HUDComponentRegistry.STATUS_BAR).copy(
                    anchor = WidgetAnchor.TOP_START,
                    offsetX = 20.dp
                )
                widgets[HUDComponentRegistry.SECONDARY_ROI] = HUDComponentRegistry.getComponentDefaults(HUDComponentRegistry.SECONDARY_ROI).copy(
                    anchor = WidgetAnchor.BOTTOM_START,
                    offsetX = 20.dp,
                    offsetY = (-100).dp
                )
            }
            "TACTICAL" -> {
                widgets[HUDComponentRegistry.CAMERA] = HUDComponentRegistry.getComponentDefaults(HUDComponentRegistry.CAMERA).copy(
                    anchor = WidgetAnchor.TOP_END,
                    offsetX = (-20).dp,
                    offsetY = 20.dp,
                    width = 160.dp,
                    height = 160.dp
                )
                widgets[HUDComponentRegistry.ALERTS] = HUDComponentRegistry.getComponentDefaults(HUDComponentRegistry.ALERTS).copy(
                    anchor = WidgetAnchor.BOTTOM_CENTER,
                    offsetY = (-120).dp
                )
            }
            "DATA_STACK" -> {
                widgets[HUDComponentRegistry.CAMERA] = HUDComponentRegistry.getComponentDefaults(HUDComponentRegistry.CAMERA).copy(
                    anchor = WidgetAnchor.CENTER,
                    width = 400.dp, // Large background-like
                    height = 400.dp,
                    alpha = 0.5f
                )
                widgets[HUDComponentRegistry.SECONDARY_ROI] = HUDComponentRegistry.getComponentDefaults(HUDComponentRegistry.SECONDARY_ROI).copy(
                    anchor = WidgetAnchor.CENTER_START,
                    offsetX = 16.dp
                )
            }
            "SYMMETRIC_GRID" -> {
                widgets[HUDComponentRegistry.CAMERA] = HUDComponentRegistry.getComponentDefaults(HUDComponentRegistry.CAMERA).copy(
                    anchor = WidgetAnchor.CENTER,
                    width = 200.dp,
                    height = 200.dp
                )
                widgets[HUDComponentRegistry.RADAR] = HUDComponentRegistry.getComponentDefaults(HUDComponentRegistry.RADAR).copy(
                    anchor = WidgetAnchor.TOP_START,
                    offsetX = 20.dp,
                    offsetY = 20.dp
                )
                widgets[HUDComponentRegistry.NOTIFICATIONS] = HUDComponentRegistry.getComponentDefaults(HUDComponentRegistry.NOTIFICATIONS).copy(
                    anchor = WidgetAnchor.TOP_END,
                    offsetX = (-20).dp,
                    offsetY = 20.dp
                )
            }
            else -> {
                // Default fallback to TOP_APERTURE
                return getDefaultBlueprint("TOP_APERTURE")
            }
        }

        
        // Add mandatory permanent widgets if not present
        if (!widgets.containsKey(HUDComponentRegistry.BOTTOM_DOCK)) {
            widgets[HUDComponentRegistry.BOTTOM_DOCK] = HUDComponentRegistry.getComponentDefaults(HUDComponentRegistry.BOTTOM_DOCK).copy(
                anchor = WidgetAnchor.BOTTOM_CENTER,
                height = 80.dp
            )
        }

        if (!widgets.containsKey(HUDComponentRegistry.STATUS_BAR)) {
             widgets[HUDComponentRegistry.STATUS_BAR] = HUDComponentRegistry.getComponentDefaults(HUDComponentRegistry.STATUS_BAR).copy(
                anchor = WidgetAnchor.TOP_CENTER,
                offsetY = 0.dp
            )
        }

        if (!widgets.containsKey(HUDComponentRegistry.ASSISTANT)) {
             widgets[HUDComponentRegistry.ASSISTANT] = HUDComponentRegistry.getComponentDefaults(HUDComponentRegistry.ASSISTANT).copy(
                anchor = WidgetAnchor.BOTTOM_CENTER,
                offsetY = (-20).dp
            )
        }
        
        return LayoutBlueprint(layoutId = layoutId, widgets = widgets)
    }
}

