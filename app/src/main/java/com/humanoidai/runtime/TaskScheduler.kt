package com.humanoidai.runtime

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
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
    private val taskSignal = Channel<Unit>(Channel.CONFLATED)
    
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
        taskSignal.close()
    }

    suspend fun submit(task: AITask) {
        queueMutex.withLock {
            val existing = taskQueue.find { it.name == task.name && it.category == task.category }
            if (existing != null) {
                taskQueue.remove(existing)
            }
            taskQueue.add(task)
        }
        taskSignal.trySend(Unit)
    }

    private fun processQueue() {
        scope.launch {
            for (signal in taskSignal) {
                if (!isRunning) break
                
                while (true) {
                    val nextTask = queueMutex.withLock {
                        if (taskQueue.isNotEmpty()) taskQueue.poll() else null
                    }

                    if (nextTask != null) {
                        executeTask(nextTask)
                    } else {
                        break
                    }
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
