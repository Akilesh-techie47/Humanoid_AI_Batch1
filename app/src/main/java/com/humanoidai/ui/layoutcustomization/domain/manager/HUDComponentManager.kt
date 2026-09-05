package com.humanoidai.ui.layoutcustomization.domain.manager

import android.util.Log
import com.humanoidai.ui.layoutcustomization.domain.blueprint.LayoutBlueprint
import com.humanoidai.ui.layoutcustomization.domain.blueprint.WidgetBlueprint
import com.humanoidai.ui.layoutcustomization.domain.hud.HUDComponentRegistry
import com.humanoidai.ui.layoutcustomization.domain.hud.HUDComponentState
import com.humanoidai.ui.layoutcustomization.domain.adaptive.HUDAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Orchestrates the modular HUD components.
 * Part of Phase 2B & 3A.
 */
class HUDComponentManager {
    
    companion object {
        private const val TAG = "HUDComponent"
    }

    private val _components = MutableStateFlow<Map<String, WidgetBlueprint>>(emptyMap())
    val components: StateFlow<Map<String, WidgetBlueprint>> = _components.asStateFlow()

    init {
        registerDefaultComponents()
    }

    /**
     * Executes an adaptive action on the components.
     * Part of Phase 3A.
     */
    fun executeAction(action: HUDAction) {
        _components.update { current ->
            val updated = current.toMutableMap()
            when (action) {
                is HUDAction.ChangeState -> {
                    updated[action.componentId]?.let { widget ->
                        if (!widget.isLocked) {
                            updated[action.componentId] = widget.copy(
                                state = action.state,
                                isVisible = action.state != HUDComponentState.HIDDEN && action.state != HUDComponentState.DISABLED
                            )
                        }
                    }
                }
                is HUDAction.SetVisibility -> {
                    updated[action.componentId]?.let { widget ->
                        if (!widget.isLocked) {
                            updated[action.componentId] = widget.copy(isVisible = action.isVisible)
                        }
                    }
                }
                is HUDAction.Highlight -> {
                    updated[action.componentId]?.let { widget ->
                        // In this phase, highlighting might just increase alpha or scale slightly
                        updated[action.componentId] = widget.copy(alpha = 1.0f)
                    }
                }
                is HUDAction.Dim -> {
                    updated[action.componentId]?.let { widget ->
                        if (!widget.isLocked) {
                            updated[action.componentId] = widget.copy(alpha = action.alpha)
                        }
                    }
                }
            }
            updated
        }
    }


    private fun registerDefaultComponents() {
        val defaults = listOf(
            HUDComponentRegistry.CAMERA,
            HUDComponentRegistry.PRIMARY_ROI,
            HUDComponentRegistry.SECONDARY_ROI,
            HUDComponentRegistry.ASSISTANT,
            HUDComponentRegistry.BOTTOM_DOCK,
            HUDComponentRegistry.STATUS_BAR,
            HUDComponentRegistry.ALERTS,
            HUDComponentRegistry.NOTIFICATIONS,
            HUDComponentRegistry.FPS_COUNTER,
            HUDComponentRegistry.RECORDING_INDICATOR,
            HUDComponentRegistry.AI_THINKING,
            HUDComponentRegistry.QUICK_ACTIONS,
            HUDComponentRegistry.RADAR,
            HUDComponentRegistry.STATUS_INDICATORS
        ).associateWith { HUDComponentRegistry.getComponentDefaults(it) }
        
        _components.value = defaults

        Log.i(TAG, "Manager: Initialized with ${defaults.size} components")
    }

    /**
     * Updates the state of a specific component.
     */
    fun updateComponentState(id: String, newState: HUDComponentState) {
        _components.update { current ->
            current[id]?.let { widget ->
                val updated = widget.copy(
                    state = newState,
                    isVisible = newState != HUDComponentState.HIDDEN && newState != HUDComponentState.DISABLED
                )
                current + (id to updated)
            } ?: current
        }
    }

    /**
     * Bulk update from a layout blueprint (usually from the Engine).
     */
    fun applyBlueprint(blueprint: LayoutBlueprint) {
        _components.update { current ->
            val updated = current.toMutableMap()
            blueprint.widgets.forEach { (id, blueprintWidget) ->
                // Merge geometry from blueprint with system metadata from manager
                val existing = updated[id]
                if (existing != null) {
                    updated[id] = blueprintWidget.copy(
                        state = existing.state,
                        category = existing.category,
                        priority = existing.priority,
                        behaviors = existing.behaviors,
                        allowedZones = existing.allowedZones,
                        isLocked = existing.isLocked
                    )
                } else {
                    updated[id] = blueprintWidget
                }
            }
            updated
        }
    }

    /**
     * Returns components sorted by priority for the renderer.
     */
    fun getPrioritizedComponents(): List<WidgetBlueprint> {
        return _components.value.values.sortedByDescending { it.priority }
    }
}
