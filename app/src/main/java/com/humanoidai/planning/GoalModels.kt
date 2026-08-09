package com.humanoidai.planning

import java.util.UUID

/**
 * High-level status for a Goal.
 */
enum class GoalStatus {
    CREATED,
    ACCEPTED,
    PLANNING,
    READY,
    EXECUTING,
    COMPLETED,
    BLOCKED,
    CANCELLED,
    FAILED,
    SUSPENDED
}

/**
 * Type of goal to help with planning strategy.
 */
enum class GoalType {
    USER_REQUEST,
    SYSTEM_OPTIMIZATION,
    SAFETY_ADAPTATION,
    KNOWLEDGE_ACQUISITION
}

/**
 * A single executable step within a plan.
 */
data class PlanStep(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    var status: StepStatus = StepStatus.PENDING,
    val action: suspend () -> Result<Unit>
)

enum class StepStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    SKIPPED
}

/**
 * Represents a goal-driven objective for the AI.
 */
data class Goal(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val type: GoalType,
    val priority: Int,
    val origin: String = "SYSTEM",
    var status: GoalStatus = GoalStatus.CREATED,
    var progress: Float = 0f,
    val steps: MutableList<PlanStep> = mutableListOf(),
    val dependencies: List<String> = emptyList(),
    val creationTime: Long = System.currentTimeMillis()
) {
    var startTime: Long = 0
    var endTime: Long = 0

    fun updateProgress() {
        if (steps.isEmpty()) return
        val completed = steps.count { it.status == StepStatus.COMPLETED }
        progress = completed.toFloat() / steps.size
    }
}
