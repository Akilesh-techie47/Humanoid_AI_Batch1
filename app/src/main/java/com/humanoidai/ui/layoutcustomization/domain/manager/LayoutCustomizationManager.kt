package com.humanoidai.ui.layoutcustomization.domain.manager

import android.content.Context
import android.util.Log
import com.humanoidai.runtime.*
import com.humanoidai.ui.layoutcustomization.data.repository.LayoutCustomizationRepository
import com.humanoidai.ui.layoutcustomization.domain.model.LayoutCustomizationDefaults
import com.humanoidai.ui.layoutcustomization.domain.validator.LayoutCustomizationValidator
import com.humanoidai.ui.layoutcustomization.presentation.state.LayoutCustomizationState
import com.humanoidai.ui.layoutcustomization.domain.blueprint.LayoutBlueprint
import com.humanoidai.ui.layoutcustomization.domain.adaptive.AdaptiveHUDEngine
import com.humanoidai.ui.layoutcustomization.domain.adaptive.HUDContext
import com.humanoidai.ui.layoutcustomization.domain.intent.PredictiveIntentEngine
import com.humanoidai.ui.layoutcustomization.domain.memory.MemoryEngine
import com.humanoidai.ui.layoutcustomization.domain.memory.ReferenceResolver
import com.humanoidai.ui.layoutcustomization.domain.roi.SmartROIEngine
import com.humanoidai.ui.layoutcustomization.domain.workspace.WorkspaceDecisionEngine

import com.humanoidai.ui.layoutcustomization.domain.reasoning.ReasoningEngine
import com.humanoidai.ui.layoutcustomization.domain.reasoning.SituationState
import com.humanoidai.ui.layoutcustomization.domain.reasoning.VisualSituation
import com.humanoidai.ui.layoutcustomization.domain.perception.PerceptionEngine
import com.humanoidai.ui.layoutcustomization.domain.skill.SkillManager
import com.humanoidai.planning.GoalManager
import com.humanoidai.embodiment.EmbodimentManager
import com.humanoidai.distributed.CoordinationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Orchestrates the layout customization domain.
 * Connects persistence (Repository) with safety (Validator) and lifecycle (Manager).
 * Part of CEA v2.0 Phase 1B, 2B, 3A, 3B, 3C, 3D, 4A, 4B, 4C, 4D.
 */
class LayoutCustomizationManager(
    private val context: Context,
    private val repository: LayoutCustomizationRepository,
    private val validator: LayoutCustomizationValidator,
    private val engine: LayoutCustomizationEngine,
    val componentManager: HUDComponentManager = HUDComponentManager(),
    private val adaptiveEngine: AdaptiveHUDEngine = AdaptiveHUDEngine(),
    val roiEngine: SmartROIEngine = SmartROIEngine(),
    val workspaceEngine: WorkspaceDecisionEngine = WorkspaceDecisionEngine(),
    val intentEngine: PredictiveIntentEngine = PredictiveIntentEngine(),
    val memoryEngine: MemoryEngine = MemoryEngine(),
    val reasoningEngine: ReasoningEngine = ReasoningEngine(),
    val embodimentManager: EmbodimentManager = EmbodimentManager(),
    val perceptionEngine: PerceptionEngine = PerceptionEngine(context, embodimentManager),
    val skillManager: SkillManager = SkillManager(context),
    val behaviorEngine: com.humanoidai.behavior.BehaviorEngine? = null,
    val coordinationManager: CoordinationManager = CoordinationManager(embodimentManager),
    val goalManager: GoalManager = GoalManager(skillManager, behaviorEngine)
) {

    val referenceResolver: ReferenceResolver = ReferenceResolver(memoryEngine)

    companion object {
        private const val TAG = "LayoutCustomization"
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private val _state = MutableStateFlow(LayoutCustomizationDefaults.getInitialState())
    val state: StateFlow<LayoutCustomizationState> = _state.asStateFlow()

    /**
     * Entry point for Context updates to trigger adaptive HUD behavior.
     * Part of Phase 3A, 3C, 3D, 4B, 4C.
     */
    fun onContextChanged(context: HUDContext) {
        if (!state.value.adaptive.isEnabled) return
        
        scope.launch {
            AIRuntimeManager.getInstance(this@LayoutCustomizationManager.context).submitTask(
                name = "ReasoningCycle",
                priority = TaskPriority.HIGH,
                category = TaskCategory.REASONING
            ) {
                // 1. Predictive Intent (Future-preparedness)
                intentEngine.evaluate(context)

                // 2. Situation Building & Reasoning (Cognitive Layer - Phase 4B)
                val situation = SituationState(
                    visual = VisualSituation(
                        primaryROI = roiEngine.decisions.value.find { it.isPrimary },
                        secondaryROIs = roiEngine.decisions.value.filter { !it.isPrimary }
                    ),
                    context = context,
                    workspace = workspaceEngine.currentWorkspace.value,
                    intent = intentEngine.state.value,
                    memory = memoryEngine.state.value
                )
                
                val decisions = reasoningEngine.evaluate(situation)
                
                // 2.1 Propose & Submit Autonomous Goals (Phase 5C)
                val goals = reasoningEngine.proposeGoals(situation)
                goals.forEach { goalManager.submitGoal(it) }

                // 3. Decision Dispatching (Action Dispatcher logic)
                decisions.forEach { decision ->
                    decision.actions.forEach { action ->
                        componentManager.executeAction(action)
                    }
                }

                // 4. Legacy Workspace adaptations (High-level profile)
                val workspaceActions = workspaceEngine.evaluate(context)
                workspaceActions.forEach { componentManager.executeAction(it) }

                // 5. Granular Adaptive HUD adaptations (Detailed rules)
                val adaptiveActions = adaptiveEngine.evaluate(context)
                adaptiveActions.forEach { componentManager.executeAction(it) }
            }
        }
    }


    /**
     * Exposes the active Layout Blueprint for the Renderer.
     */
    val activeBlueprint: StateFlow<LayoutBlueprint> = state.map { customization ->
        val candidate = engine.computeBlueprint(customization.selectedLayout, customization)
        val validated = validator.validateBlueprint(candidate)
        
        // Sync with Component Manager (Phase 2B)
        componentManager.applyBlueprint(validated)
        
        validated
    }.stateIn(scope, SharingStarted.Eagerly, LayoutRegistry.getDefaultBlueprint("top_aperture"))


    init {
        Log.i(TAG, "Manager: Initializing system state")
        observeRepository()
        observeROIEngine()
    }

    private fun observeROIEngine() {
        scope.launch {
            roiEngine.decisions.collect { decisions ->
                // Sync cognitive focus with primary ROI if it's stable
                decisions.find { it.isPrimary }?.let { primary ->
                    memoryEngine.setFocus(primary.id)
                }
            }
        }
    }

    private fun observeRepository() {
        scope.launch {
            repository.state
                .onEach { Log.d(TAG, "Manager: State observed from repository") }
                .collect { newState ->
                    // Validate incoming state from disk
                    _state.value = validator.validate(newState)
                }
        }
    }

    /**
     * Commits a new state after validation.
     */
    fun commit(newState: LayoutCustomizationState) {
        val validatedState = validator.validate(newState)
        if (validatedState != _state.value) {
            Log.i(TAG, "Manager: Committing new state to persistence")
            _state.value = validatedState
            scope.launch {
                repository.saveState(validatedState)
            }
        }
    }

    /**
     * Factory reset for all customization parameters.
     */
    fun resetToDefaults() {
        Log.i(TAG, "Manager: Triggering factory reset")
        commit(LayoutCustomizationDefaults.getInitialState())
    }
}
