package com.humanoidai.runtime

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AIRuntimeManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    val monitor = PerformanceMonitor(context)
    val scheduler = TaskScheduler(scope)
    val policyEngine = RuntimePolicyEngine()
    val capabilityDetector = DeviceCapabilityDetector(context)
    
    private val _healthState = MutableStateFlow(HealthState.HEALTHY)
    val healthState: StateFlow<HealthState> = _healthState.asStateFlow()

    private var monitoringJob: Job? = null
    
    var deviceProfile: DeviceCapabilityProfile? = null
        private set

    fun initialize() {
        deviceProfile = capabilityDetector.detect()
        scheduler.start()
        startMonitoring()
    }

    private fun startMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = scope.launch {
            while (isActive) {
                monitor.updateMetrics()
                val metrics = monitor.metrics.value
                
                // Update AI metrics from scheduler
                val updatedMetrics = metrics.copy(
                    ai = metrics.ai.copy(
                        tasksQueued = scheduler.getQueueSize()
                    )
                )
                
                policyEngine.evaluate(updatedMetrics)
                _healthState.value = policyEngine.getHealthState(updatedMetrics)
                
                delay(1000) // Monitor refresh rate
            }
        }
    }

    suspend fun submitTask(
        name: String,
        priority: TaskPriority,
        category: TaskCategory,
        execution: suspend () -> Unit
    ) {
        val task = AITask(
            name = name,
            priority = priority,
            category = category,
            execution = execution
        )
        scheduler.submit(task)
    }

    fun shutdown() {
        monitoringJob?.cancel()
        scheduler.stop()
        scope.cancel()
    }

    companion object {
        @Volatile
        private var instance: AIRuntimeManager? = null

        fun getInstance(context: Context): AIRuntimeManager {
            return instance ?: synchronized(this) {
                instance ?: AIRuntimeManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
