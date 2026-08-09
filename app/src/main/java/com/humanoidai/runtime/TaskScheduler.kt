package com.humanoidai.runtime

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.PriorityQueue

class TaskScheduler(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val taskQueue = PriorityQueue<AITask> { t1, t2 ->
        t2.priority.weight.compareTo(t1.priority.weight)
    }
    
    private val queueMutex = Mutex()
    private val activeTasks = mutableMapOf<String, Job>()
    
    private var isRunning = false

    fun start() {
        if (isRunning) return
        isRunning = true
        processQueue()
    }

    fun stop() {
        isRunning = false
        activeTasks.values.forEach { it.cancel() }
        activeTasks.clear()
    }

    suspend fun submit(task: AITask) {
        queueMutex.withLock {
            // Merge logic: if a task with the same name and category exists, replace it
            val existing = taskQueue.find { it.name == task.name && it.category == task.category }
            if (existing != null) {
                taskQueue.remove(existing)
            }
            taskQueue.add(task)
        }
    }

    private fun processQueue() {
        scope.launch {
            while (isRunning) {
                val nextTask = queueMutex.withLock {
                    if (taskQueue.isNotEmpty()) taskQueue.poll() else null
                }

                if (nextTask != null) {
                    executeTask(nextTask)
                } else {
                    delay(10) // Small delay to prevent tight loop when empty
                }
            }
        }
    }

    private fun executeTask(task: AITask) {
        val job = scope.launch {
            try {
                task.status = TaskStatus.RUNNING
                task.startTime = System.currentTimeMillis()
                task.execution()
                task.status = TaskStatus.COMPLETED
            } catch (e: CancellationException) {
                task.status = TaskStatus.CANCELLED
            } catch (e: Exception) {
                task.status = TaskStatus.FAILED
            } finally {
                task.endTime = System.currentTimeMillis()
                queueMutex.withLock {
                    activeTasks.remove(task.id)
                }
            }
        }
        
        scope.launch {
            queueMutex.withLock {
                activeTasks[task.id] = job
            }
        }
    }

    suspend fun getQueueSize(): Int = queueMutex.withLock { taskQueue.size }
    
    suspend fun cancelTask(taskId: String) {
        queueMutex.withLock {
            activeTasks[taskId]?.cancel()
            activeTasks.remove(taskId)
            taskQueue.removeAll { it.id == taskId }
        }
    }
}
