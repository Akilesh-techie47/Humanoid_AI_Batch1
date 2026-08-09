package com.humanoidai.planning

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Manages the sequential execution of plan steps.
 */
class ExecutionCoordinator(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    companion object {
        private const val TAG = "ExecutionCoordinator"
    }

    private val activeJobs = mutableMapOf<String, Job>()
    private val mutex = Mutex()

    fun execute(goal: Goal, onProgress: (Goal) -> Unit) {
        scope.launch {
            mutex.withLock {
                if (activeJobs.containsKey(goal.id)) {
                    Log.w(TAG, "Goal ${goal.id} already executing. Skipping.")
                    return@launch
                }
            }

            val job = launch {
                runGoalSteps(goal, onProgress)
            }

            mutex.withLock {
                activeJobs[goal.id] = job
            }

            job.join()

            mutex.withLock {
                activeJobs.remove(goal.id)
            }
        }
    }

    private suspend fun runGoalSteps(goal: Goal, onProgress: (Goal) -> Unit) {
        goal.status = GoalStatus.EXECUTING
        goal.startTime = System.currentTimeMillis()
        onProgress(goal)

        for (step in goal.steps) {
            if (goal.status != GoalStatus.EXECUTING) break

            step.status = StepStatus.RUNNING
            onProgress(goal)

            val result = try {
                step.action()
            } catch (e: Exception) {
                Log.e(TAG, "Step ${step.name} failed with exception", e)
                Result.failure(e)
            }

            if (result.isSuccess) {
                step.status = StepStatus.COMPLETED
            } else {
                step.status = StepStatus.FAILED
                goal.status = GoalStatus.FAILED
                Log.e(TAG, "Goal ${goal.id} failed at step ${step.name}")
                break
            }
            
            goal.updateProgress()
            onProgress(goal)
        }

        if (goal.status == GoalStatus.EXECUTING) {
            goal.status = GoalStatus.COMPLETED
            goal.endTime = System.currentTimeMillis()
        }
        
        onProgress(goal)
    }

    fun cancel(goalId: String) {
        scope.launch {
            mutex.withLock {
                activeJobs[goalId]?.cancel()
                activeJobs.remove(goalId)
            }
        }
    }
}
