package com.humanoidai.ui.layoutcustomization.domain.adaptive

import android.util.Log
import com.humanoidai.ui.layoutcustomization.domain.hud.HUDComponentRegistry
import com.humanoidai.ui.layoutcustomization.domain.hud.HUDComponentState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The intelligence layer that translates Context into Actions.
 * Part of Phase 3A.
 */
class AdaptiveHUDEngine {

    companion object {
        private const val TAG = "AdaptiveHUD"
        private const val MIN_COOLDOWN_MS = 1000L
    }

    private val _lastDecisionTime = MutableStateFlow(0L)
    private val rules = mutableListOf<AdaptiveHUDRule>()

    init {
        registerDefaultRules()
    }

    private fun registerDefaultRules() {
        rules.add(DetectionRule())
        rules.add(AssistantRule())
        rules.add(AlertRule())
    }

    /**
     * Re-evaluates the HUD state based on the provided context.
     * Returns a list of actions to be executed.
     */
    fun evaluate(context: HUDContext): List<HUDAction> {
        val currentTime = System.currentTimeMillis()
        if (currentTime - _lastDecisionTime.value < MIN_COOLDOWN_MS) {
            return emptyList()
        }

        Log.d(TAG, "Engine: Evaluating context at $currentTime")
        
        // 1. Collect all suggested actions from rules
        val suggestedActions = rules
            .sortedByDescending { it.priority }
            .flatMap { rule -> 
                val actions = rule.evaluate(context)
                if (actions.isNotEmpty()) {
                    Log.d(TAG, "Rule [${rule.name}] suggested ${actions.size} actions")
                }
                actions
            }

        // 2. Conflict Resolution (Priority-based)
        // For Phase 3A, we'll keep the first action suggested for each component (highest priority rule wins)
        val finalActions = mutableMapOf<String, HUDAction>()
        suggestedActions.forEach { action ->
            val targetId = when (action) {
                is HUDAction.ChangeState -> action.componentId
                is HUDAction.SetVisibility -> action.componentId
                is HUDAction.Highlight -> action.componentId
                is HUDAction.Dim -> action.componentId
            }
            if (!finalActions.containsKey(targetId)) {
                finalActions[targetId] = action
            }
        }

        if (finalActions.isNotEmpty()) {
            _lastDecisionTime.value = currentTime
        }

        return finalActions.values.toList()
    }

    // --- Built-in Rule Implementations ---

    private class DetectionRule : AdaptiveHUDRule {
        override val name = "Detection"
        override val priority = RulePriority.MEDIUM

        override fun evaluate(context: HUDContext): List<HUDAction> {
            return when (context.detection) {
                DetectionState.NONE -> listOf(
                    HUDAction.SetVisibility(HUDComponentRegistry.SECONDARY_ROI, false),
                    HUDAction.ChangeState(HUDComponentRegistry.PRIMARY_ROI, HUDComponentState.COMPACT)
                )
                DetectionState.SINGLE_OBJECT -> listOf(
                    HUDAction.SetVisibility(HUDComponentRegistry.PRIMARY_ROI, true),
                    HUDAction.ChangeState(HUDComponentRegistry.PRIMARY_ROI, HUDComponentState.EXPANDED)
                )
                DetectionState.MULTIPLE_OBJECTS -> listOf(
                    HUDAction.SetVisibility(HUDComponentRegistry.PRIMARY_ROI, true),
                    HUDAction.SetVisibility(HUDComponentRegistry.SECONDARY_ROI, true),
                    HUDAction.ChangeState(HUDComponentRegistry.PRIMARY_ROI, HUDComponentState.FOCUSED)
                )
            }
        }
    }

    private class AssistantRule : AdaptiveHUDRule {
        override val name = "Assistant"
        override val priority = RulePriority.HIGH

        override fun evaluate(context: HUDContext): List<HUDAction> {
            return when (context.assistant) {
                AssistantState.IDLE -> listOf(
                    HUDAction.ChangeState(HUDComponentRegistry.ASSISTANT, HUDComponentState.COLLAPSED)
                )
                AssistantState.LISTENING, AssistantState.THINKING -> listOf(
                    HUDAction.ChangeState(HUDComponentRegistry.ASSISTANT, HUDComponentState.COMPACT)
                )
                AssistantState.SPEAKING -> listOf(
                    HUDAction.ChangeState(HUDComponentRegistry.ASSISTANT, HUDComponentState.EXPANDED),
                    HUDAction.Dim(HUDComponentRegistry.PRIMARY_ROI, 0.5f)
                )
            }
        }
    }

    private class AlertRule : AdaptiveHUDRule {
        override val name = "Alerts"
        override val priority = RulePriority.CRITICAL

        override fun evaluate(context: HUDContext): List<HUDAction> {
            return if (context.alertLevel != AlertLevel.NONE) {
                listOf(
                    HUDAction.SetVisibility(HUDComponentRegistry.ALERTS, true),
                    HUDAction.ChangeState(HUDComponentRegistry.ALERTS, HUDComponentState.EXPANDED)
                )
            } else {
                listOf(HUDAction.SetVisibility(HUDComponentRegistry.ALERTS, false))
            }
        }
    }
}
