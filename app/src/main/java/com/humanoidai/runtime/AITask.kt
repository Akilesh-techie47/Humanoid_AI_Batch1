package com.humanoidai.runtime

import java.util.UUID

/**
 * Represents a unit of AI work that can be scheduled and monitored.
 */
data class AITask(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val priority: TaskPriority,
    val category: TaskCategory,
    val isCancellable: Boolean = true,
    val execution: suspend () -> Unit
) {
    var status: TaskStatus = TaskStatus.PENDING
    var submissionTime: Long = System.currentTimeMillis()
    var startTime: Long = 0
    var endTime: Long = 0

    val duration: Long
        get() = if (endTime > 0) endTime - startTime else 0
}

enum class TaskStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    CANCELLED,
    FAILED
}
