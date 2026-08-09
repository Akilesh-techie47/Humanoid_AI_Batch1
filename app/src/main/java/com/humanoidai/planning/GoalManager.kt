package com.humanoidai.planning

import android.util.Log
import com.humanoidai.ui.layoutcustomization.domain.skill.SkillManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Orchestrates high-level system goals.
 */
class GoalManager(
    private val skillManager: SkillManager,
    private val behaviorEngine: com.humanoidai.behavior.BehaviorEngine? = null
) {

    companion object {
        private const val TAG = "GoalManager"
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val planner = Planner(skillManager)
    private val coordinator = ExecutionCoordinator(scope)

    private val _activeGoals = MutableStateFlow<Map<String, Goal>>(emptyMap())
    val activeGoals: StateFlow<Map<String, Goal>> = _activeGoals.asStateFlow()

    fun submitGoal(goal: Goal) {
        Log.i(TAG, "Submitting goal: ${goal.title}")
        
        // Arbitration: If a goal with the same title exists, cancel it first
        val existing = _activeGoals.value.values.find { it.title == goal.title && it.status != GoalStatus.COMPLETED }
        if (existing != null) {
            cancelGoal(existing.id)
        }

        _activeGoals.update { it + (goal.id to goal) }

        scope.launch {
            planner.createPlan(goal)
            if (goal.status == GoalStatus.READY) {
                coordinator.execute(goal) { updatedGoal ->
                    _activeGoals.update { it + (updatedGoal.id to updatedGoal.copy()) }
                    
                    // Notify Behavior Engine of progress milestones (Phase 5D)
                    behaviorEngine?.let { behavior ->
                        val response = behavior.responseComposer.composeProgressUpdate(
                            updatedGoal.title,
                            updatedGoal.progress,
                            behavior.personality.value
                        )
                        if (response.spokenText.isNotEmpty() || response.text.isNotEmpty()) {
                            behavior.deliverResponse(response)
                        }
                    }
                }
            }
        }
    }

    fun cancelGoal(goalId: String) {
        val goal = _activeGoals.value[goalId] ?: return
        goal.status = GoalStatus.CANCELLED
        coordinator.cancel(goalId)
        _activeGoals.update { it + (goalId to goal) }
    }

    fun clearCompleted() {
        _activeGoals.update { current ->
            current.filter { it.value.status != GoalStatus.COMPLETED && it.value.status != GoalStatus.CANCELLED }
        }
    }
}
