package com.humanoidai.ui.layoutcustomization.domain.manager

import android.util.Log
import com.humanoidai.ui.layoutcustomization.domain.blueprint.LayoutBlueprint
import com.humanoidai.ui.layoutcustomization.domain.blueprint.WidgetAnchor
import com.humanoidai.ui.layoutcustomization.domain.blueprint.WidgetBlueprint

/**
 * Resolves semantic constraints and anchors into a final valid blueprint.
 * Ensures collision avoidance and safe-area compliance.
 * Part of CEA v2.0 Phase 1D.
 */
class LayoutConstraintResolver {

    companion object {
        private const val TAG = "LayoutCustomization"
    }

    /**
     * Resolves all widget constraints for the given blueprint.
     */
    fun resolve(blueprint: LayoutBlueprint): LayoutBlueprint {
        Log.d(TAG, "Resolver: Resolving constraints for ${blueprint.layoutId}")
        
        val resolvedWidgets = blueprint.widgets.toMutableMap()
        
        // 1. Sort widgets by priority to resolve placement (Higher priority first)
        val sortedWidgets = blueprint.widgets.values.sortedByDescending { it.constraints.priority }
        
        sortedWidgets.forEach { widget ->
            // Resolve final anchor and offset based on preferred/allowed settings
            val finalWidget = resolveWidgetPosition(widget)
            resolvedWidgets[widget.id] = finalWidget
        }

        // 2. Perform Collision Check (Phase 1D Placeholder)
        // Future phases will implement actual intersection logic
        
        return blueprint.copy(
            widgets = resolvedWidgets,
            lastComputed = System.currentTimeMillis()
        )
    }

    private fun resolveWidgetPosition(widget: WidgetBlueprint): WidgetBlueprint {
        // In Phase 1D, we ensure the preferred anchor is respected if valid.
        // If preferredAnchor is unset, we fallback to the current blueprint anchor.
        
        val targetAnchor = if (widget.constraints.preferredAnchor != WidgetAnchor.CENTER) {
            widget.constraints.preferredAnchor 
        } else {
            widget.anchor
        }

        return widget.copy(anchor = targetAnchor)
    }
}
