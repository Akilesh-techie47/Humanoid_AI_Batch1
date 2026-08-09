package com.humanoidai.ui.layoutcustomization.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.humanoidai.ui.layoutcustomization.domain.manager.LayoutCustomizationManager
import com.humanoidai.ui.layoutcustomization.domain.adaptive.HUDContext
import com.humanoidai.ui.layoutcustomization.presentation.event.LayoutCustomizationEvent
import com.humanoidai.ui.layoutcustomization.presentation.state.LayoutCustomizationState
import com.humanoidai.ui.layoutcustomization.domain.blueprint.WidgetBlueprint
import com.humanoidai.ui.layoutcustomization.domain.intent.IntentState
import com.humanoidai.ui.layoutcustomization.domain.memory.MemoryEntry
import com.humanoidai.ui.layoutcustomization.domain.memory.MemoryState
import com.humanoidai.ui.layoutcustomization.domain.perception.PerceptionState
import com.humanoidai.ui.layoutcustomization.domain.reasoning.DecisionState
import com.humanoidai.ui.layoutcustomization.domain.roi.ROIDecision
import com.humanoidai.ui.layoutcustomization.domain.workspace.WorkspaceProfile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * The single entry point for UI components to interact with the Layout Customization domain.
 * Part of CEA v2.0 Phase 1B, 2B, 3A, 3B, 3C, 3D, 4A, 4B, 4C.
 */
class LayoutCustomizationViewModel(
    private val manager: LayoutCustomizationManager
) : ViewModel() {

    val state: StateFlow<LayoutCustomizationState> = manager.state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LayoutCustomizationState()
    )

    /**
     * Unified perception of the environment.
     * Part of Phase 4C.
     */
    val perceptionState: StateFlow<PerceptionState> = manager.perceptionEngine.state
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PerceptionState()
        )


    /**
     * The active cognitive decisions.
     * Part of Phase 4B.
     */
    val decisionState: StateFlow<DecisionState> = manager.reasoningEngine.decisionState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DecisionState()
        )


    /**
     * The active user intent prediction.
     * Part of Phase 3D.
     */
    val currentIntent: StateFlow<IntentState> = manager.intentEngine.state
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = IntentState()
        )

    /**
     * The active cognitive memory state.
     * Part of Phase 4A.
     */
    val memoryState: StateFlow<MemoryState> = manager.memoryEngine.state
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MemoryState()
        )



    /**
     * The active workspace profile.
     * Part of Phase 3C.
     */
    val currentWorkspace: StateFlow<WorkspaceProfile> = manager.workspaceEngine.currentWorkspace
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = manager.workspaceEngine.currentWorkspace.value
        )


    /**
     * Live ROI decisions from the intelligence engine.
     * Part of Phase 3B.
     */
    val roiDecisions: StateFlow<List<ROIDecision>> = manager.roiEngine.decisions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )


    /**
     * Updates the environmental context to trigger adaptive behavior.
     * Part of Phase 3A.
     */
    fun updateContext(context: HUDContext) {
        manager.onContextChanged(context)
    }

    /**
     * Entry point for new detections to be processed by the ROI Intelligence Engine.
     * Part of Phase 3B.
     */
    /**
     * Records a cognitive event into the memory stack.
     * Part of Phase 4A.
     */
    /**
     * Updates individual modalities of the perception engine.
     * Part of Phase 4C.
     */
    fun recordMemory(entry: com.humanoidai.ui.layoutcustomization.domain.memory.MemoryEntry) {
        manager.memoryEngine.record(entry)
    }

    fun updateVisualPerception(visual: com.humanoidai.ui.layoutcustomization.domain.perception.VisualPerception) {
        manager.perceptionEngine.updateVisual(visual)
    }

    fun updateAudioPerception(audio: com.humanoidai.ui.layoutcustomization.domain.perception.AudioPerception) {
        manager.perceptionEngine.updateAudio(audio)
    }

    fun updateEnvironmentPerception(env: com.humanoidai.ui.layoutcustomization.domain.perception.EnvironmentalPerception) {
        manager.perceptionEngine.updateEnvironment(env)
    }



    /**
     * Entry point for new detections to be processed by the ROI Intelligence Engine.
     * Part of Phase 3B.
     */
    fun onDetectionsUpdated(candidates: List<com.humanoidai.ui.layoutcustomization.domain.roi.ROICandidate>) {
        manager.roiEngine.processDetections(candidates)
    }





    /**
     * Optimized stream of HUD components for the renderer.
     * Part of Phase 2B.
     */
    val hudComponents: StateFlow<List<WidgetBlueprint>> = manager.componentManager.components
        .map { it.values.sortedByDescending { widget -> widget.priority } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )


    /**
     * Entry point for UI events.
     */
    fun onEvent(event: LayoutCustomizationEvent) {
        val currentState = state.value
        val newState = when (event) {
            is LayoutCustomizationEvent.ChangeCameraScale -> {
                currentState.copy(camera = currentState.camera.copy(scale = event.scale))
            }
            is LayoutCustomizationEvent.ChangeCameraOffset -> {
                currentState.copy(camera = currentState.camera.copy(offsetX = event.x, offsetY = event.y))
            }
            is LayoutCustomizationEvent.ChangePrimaryROIPosition -> {
                currentState.copy(roi = currentState.roi.copy(
                    primaryPositionX = event.x, 
                    primaryPositionY = event.y,
                    primaryAutoPosition = event.auto
                ))
            }
            is LayoutCustomizationEvent.ChangeSecondaryROIPosition -> {
                currentState.copy(roi = currentState.roi.copy(
                    secondaryPositionX = event.x, 
                    secondaryPositionY = event.y,
                    secondaryAutoPosition = event.auto
                ))
            }
            is LayoutCustomizationEvent.ChangeWidgetDensity -> {
                currentState.copy(widget = currentState.widget.copy(density = event.density))
            }
            is LayoutCustomizationEvent.ChangeWidgetArrangement -> {
                currentState.copy(widget = currentState.widget.copy(arrangement = event.arrangement))
            }
            is LayoutCustomizationEvent.ToggleWidgetVisibility -> {
                val newVisibilityMap = currentState.widget.visibilityMap.toMutableMap()
                newVisibilityMap[event.widgetId] = event.visible
                currentState.copy(widget = currentState.widget.copy(visibilityMap = newVisibilityMap))
            }
            is LayoutCustomizationEvent.ChangeMotionProfile -> {
                currentState.copy(motion = currentState.motion.copy(profile = event.profile))
            }
            is LayoutCustomizationEvent.ChangeSelectedLayout -> {
                currentState.copy(selectedLayout = event.layoutId)
            }
            LayoutCustomizationEvent.ResetCustomization,

            LayoutCustomizationEvent.RestoreDefaults -> {
                // Delegate to manager for full reset
                manager.resetToDefaults()
                return 
            }
        }
        
        // Commit update via manager
        manager.commit(newState)
    }

    class Factory(private val manager: LayoutCustomizationManager) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return LayoutCustomizationViewModel(manager) as T
        }
    }
}
