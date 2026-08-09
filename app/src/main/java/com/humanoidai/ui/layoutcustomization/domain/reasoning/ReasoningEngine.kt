package com.humanoidai.ui.layoutcustomization.domain.reasoning

import android.util.Log
import com.humanoidai.planning.Goal
import com.humanoidai.planning.GoalType
import com.humanoidai.ui.layoutcustomization.domain.adaptive.AlertLevel
import com.humanoidai.ui.layoutcustomization.domain.adaptive.AssistantState
import com.humanoidai.ui.layoutcustomization.domain.adaptive.DetectionState
import com.humanoidai.ui.layoutcustomization.domain.adaptive.HUDAction
import com.humanoidai.ui.layoutcustomization.domain.hud.HUDComponentRegistry
import com.humanoidai.ui.layoutcustomization.domain.hud.HUDComponentState
import com.humanoidai.ui.layoutcustomization.domain.intent.IntentCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The core intelligence hub for situational understanding and decision making.
 * Part of Phase 4B & 5C.
 */
class ReasoningEngine {

    companion object {
        private const val TAG = "ReasoningEngine"
    }

    private val _decisionState = MutableStateFlow(DecisionState())
    val decisionState: StateFlow<DecisionState> = _decisionState.asStateFlow()

    /**
     * Evaluates the current situation to generate decisions and goals.
     */
    fun evaluate(situation: SituationState): List<Decision> {
        val newDecisions = mutableListOf<Decision>()

        // 1. Rule: Focus Reinforcement
        situation.visual.primaryROI?.let { primary ->
            if (primary.score > 0.8f) {
                newDecisions.add(
                    Decision(
                        id = "highlight_primary_${primary.id}",
                        category = "UI",
                        priority = 70,
                        confidence = DecisionConfidence.HIGH,
                        actions = listOf(HUDAction.Highlight(HUDComponentRegistry.PRIMARY_ROI, 1.0f)),
                        evidence = listOf("Stable high-confidence target detected."),
                        explanation = "Highlighting the primary object of interest to confirm AI focus."
                    )
                )
            }
        }

        // 2. Rule: Conversation Continuity
        if (situation.intent.activeIntent.category == IntentCategory.CONVERSATION) {
            newDecisions.add(
                Decision(
                    id = "prepare_conversation_workspace",
                    category = "UI",
                    priority = 80,
                    confidence = DecisionConfidence.VERY_HIGH,
                    actions = listOf(
                        HUDAction.ChangeState(HUDComponentRegistry.ASSISTANT, HUDComponentState.COMPACT),
                        HUDAction.Dim(HUDComponentRegistry.BOTTOM_DOCK, 0.3f)
                    ),
                    evidence = listOf("Conversation intent predicted with human presence."),
                    explanation = "Preparing the assistant interface for an expected interaction."
                )
            )
        }

        // 3. Rule: Critical Safety Priority
        if (situation.context.alertLevel == AlertLevel.CRITICAL) {
            newDecisions.add(
                Decision(
                    id = "emergency_alert_workspace",
                    category = "System",
                    priority = 100,
                    confidence = DecisionConfidence.VERY_HIGH,
                    actions = listOf(
                        HUDAction.ChangeState(HUDComponentRegistry.ALERTS, HUDComponentState.EXPANDED),
                        HUDAction.Dim(HUDComponentRegistry.ASSISTANT, 0.2f),
                        HUDAction.Dim(HUDComponentRegistry.PRIMARY_ROI, 0.2f)
                    ),
                    evidence = listOf("System-critical alert active."),
                    explanation = "Suppressing secondary information to prioritize emergency alerts."
                )
            )
        }

        // Conflict Resolution (Highest priority per category/component wins)
        val resolved = resolveConflicts(newDecisions)
        
        updateDecisionState(resolved)
        
        return resolved
    }

    /**
     * Proposes high-level autonomous goals based on the current situation.
     * Part of Phase 5C.
     */
    fun proposeGoals(situation: SituationState): List<Goal> {
        val goals = mutableListOf<Goal>()

        // Autonomous Visual Analysis for unknown high-confidence targets
        situation.visual.primaryROI?.let { primary ->
            if (primary.score > 0.9f && situation.memory.activeFocusId != primary.id) {
                goals.add(Goal(
                    title = "Visual Analysis",
                    type = GoalType.KNOWLEDGE_ACQUISITION,
                    priority = 60,
                    origin = "AUTONOMOUS"
                ))
            }
        }

        // Proactive System Optimization if resources are tight (informed by Runtime Manager)
        // Note: Real implementation would check health status from Runtime Manager
        
        return goals
    }

    private fun resolveConflicts(decisions: List<Decision>): List<Decision> {
        // Simple conflict resolution for Phase 4B: keep highest priority overall
        // In a real system, we'd check for specific component action conflicts.
        return decisions.sortedByDescending { it.priority }
    }

    private fun updateDecisionState(newDecisions: List<Decision>) {
        _decisionState.update { current ->
            val updatedHistory = (newDecisions + current.history).take(20)
            current.copy(
                activeDecisions = newDecisions.associateBy { it.id },
                history = updatedHistory
            )
        }
        
        newDecisions.forEach { 
            Log.i(TAG, "New Decision: ${it.id} - ${it.explanation}")
        }
    }
}
