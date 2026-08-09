package com.humanoidai.ui.layoutcustomization.domain.blueprint

import androidx.compose.runtime.Immutable

/**
 * The "Layout Operating System" model.
 * Single source of truth for the Camera UI Renderer.
 */
@Immutable
data class LayoutBlueprint(
    val version: String = "2.0.1C",
    val layoutId: String,
    val widgets: Map<String, WidgetBlueprint>,
    val lastComputed: Long = System.currentTimeMillis()
) {
    /**
     * Convenience getters for core system widgets.
     */
    val camera: WidgetBlueprint? get() = widgets["camera"]
    val primaryROI: WidgetBlueprint? get() = widgets["primary_roi"]
    val secondaryROI: WidgetBlueprint? get() = widgets["secondary_roi"]
    val assistant: WidgetBlueprint? get() = widgets["assistant"]
    val bottomDock: WidgetBlueprint? get() = widgets["bottom_dock"]
}
