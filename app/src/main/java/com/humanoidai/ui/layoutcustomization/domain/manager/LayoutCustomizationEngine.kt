package com.humanoidai.ui.layoutcustomization.domain.manager

import android.util.Log
import androidx.compose.ui.unit.dp
import com.humanoidai.ui.layoutcustomization.domain.blueprint.LayoutBlueprint
import com.humanoidai.ui.layoutcustomization.presentation.state.LayoutCustomizationState

/**
 * The Heart of the Customization System.
 * Transforms Structural Layouts + Personalization into a Layout Blueprint.
 */
class LayoutCustomizationEngine(
    private val resolver: LayoutConstraintResolver
) {

    companion object {
        private const val TAG = "LayoutCustomization"
    }

    /**
     * Pipeline: Load Defaults -> Apply Overrides -> Resolve Constraints -> Generate Blueprint.
     */
    fun computeBlueprint(
        layoutId: String,
        customization: LayoutCustomizationState
    ): LayoutBlueprint {
        Log.i(TAG, "Engine: Generating Blueprint for $layoutId")
        
        // 1. Load default structural geometry
        val baseBlueprint = LayoutRegistry.getDefaultBlueprint(layoutId)
        val candidateWidgets = baseBlueprint.widgets.toMutableMap()
        
        // 2. Apply user personalization overrides
        candidateWidgets["camera"]?.let { cam ->
            candidateWidgets["camera"] = cam.copy(
                anchor = if (customization.roi.primaryAutoPosition) cam.anchor else mapCoordsToAnchor(customization.roi.primaryPositionX, customization.roi.primaryPositionY),
                scale = customization.camera.scale,
                offsetX = customization.camera.offsetX.dp,
                offsetY = customization.camera.offsetY.dp
            )
        }
        
        candidateWidgets["secondary_roi"]?.let { roi ->
            candidateWidgets["secondary_roi"] = roi.copy(
                anchor = if (customization.roi.secondaryAutoPosition) roi.anchor else mapCoordsToAnchor(customization.roi.secondaryPositionX, customization.roi.secondaryPositionY),
                isVisible = customization.widget.visibilityMap["Secondary ROI"] ?: true
            )
        }

        // Apply Global Visibility Map
        candidateWidgets.forEach { (id, widget) ->
            val settingsKey = mapWidgetIdToSettingsKey(id)
            if (settingsKey != null) {
                candidateWidgets[id] = widget.copy(
                    isVisible = customization.widget.visibilityMap[settingsKey] ?: widget.isVisible
                )
            }
        }

        // Apply Density (Global Scale tweak)
        val densityScale = when(customization.widget.density.lowercase()) {
            "minimal" -> 0.7f
            "compact" -> 0.85f
            "expanded" -> 1.15f
            "command center" -> 1.3f
            else -> 1.0f
        }
        candidateWidgets.forEach { (id, widget) ->
            if (id != "camera") {
                candidateWidgets[id] = widget.copy(scale = widget.scale * densityScale)
            }
        }
        
        // 4. Construct candidate blueprint
        val candidateBlueprint = baseBlueprint.copy(
            widgets = candidateWidgets
        )

        // 5. Resolve Constraints (Phase 1D: Anchor & Priority logic)
        return resolver.resolve(candidateBlueprint)
    }

    private fun mapWidgetIdToSettingsKey(id: String): String? {
        return when(id) {
            "primary_roi" -> "ROI Labels"
            "secondary_roi" -> "Secondary ROI"
            "assistant" -> "AI Assistant"
            "status_bar" -> "AI Status"
            "notifications" -> "Notification Stack"
            "alerts" -> "Alert Panel"
            "radar" -> "Distance Indicator"
            "status_indicators" -> "Sensor Status"
            "object_details" -> "Object Details"
            else -> null
        }
    }




    private fun mapCoordsToAnchor(x: Float, y: Float): com.humanoidai.ui.layoutcustomization.domain.blueprint.WidgetAnchor {
        return when {
            x < 0.3f && y < 0.3f -> com.humanoidai.ui.layoutcustomization.domain.blueprint.WidgetAnchor.TOP_START
            x > 0.7f && y < 0.3f -> com.humanoidai.ui.layoutcustomization.domain.blueprint.WidgetAnchor.TOP_END
            y < 0.3f -> com.humanoidai.ui.layoutcustomization.domain.blueprint.WidgetAnchor.TOP_CENTER
            x < 0.3f && y > 0.7f -> com.humanoidai.ui.layoutcustomization.domain.blueprint.WidgetAnchor.BOTTOM_START
            x > 0.7f && y > 0.7f -> com.humanoidai.ui.layoutcustomization.domain.blueprint.WidgetAnchor.BOTTOM_END
            y > 0.7f -> com.humanoidai.ui.layoutcustomization.domain.blueprint.WidgetAnchor.BOTTOM_CENTER
            x < 0.3f -> com.humanoidai.ui.layoutcustomization.domain.blueprint.WidgetAnchor.CENTER_START
            x > 0.7f -> com.humanoidai.ui.layoutcustomization.domain.blueprint.WidgetAnchor.CENTER_END
            else -> com.humanoidai.ui.layoutcustomization.domain.blueprint.WidgetAnchor.CENTER
        }
    }
}
