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
                anchor = mapCoordsToAnchor(customization.roi.primaryPositionX, customization.roi.primaryPositionY),
                scale = customization.camera.scale,
                offsetX = customization.camera.offsetX.dp,
                offsetY = customization.camera.offsetY.dp
            )
        }
        
        // 3. Apply ROI visibility
        candidateWidgets["secondary_roi"]?.let { roi ->
            candidateWidgets["secondary_roi"] = roi.copy(
                isVisible = customization.widget.visibilityMap["Secondary ROI"] ?: true
            )
        }
        
        // 4. Construct candidate blueprint
        val candidateBlueprint = baseBlueprint.copy(
            widgets = candidateWidgets
        )

        // 5. Resolve Constraints (Phase 1D: Anchor & Priority logic)
        return resolver.resolve(candidateBlueprint)
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
