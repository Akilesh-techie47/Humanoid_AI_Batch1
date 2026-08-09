package com.humanoidai.runtime

data class PerformanceMetrics(
    val cpu: CpuMetrics = CpuMetrics(),
    val gpu: GpuMetrics = GpuMetrics(),
    val memory: MemoryMetrics = MemoryMetrics(),
    val battery: BatteryMetrics = BatteryMetrics(),
    val thermal: ThermalMetrics = ThermalMetrics(),
    val ai: AiRuntimeMetrics = AiRuntimeMetrics(),
    val timestamp: Long = System.currentTimeMillis()
)

data class CpuMetrics(
    val utilizationPercent: Int = 0,
    val activeThreads: Int = 0
)

data class GpuMetrics(
    val loadPercent: Int = 0,
    val frameTimeMs: Float = 0f
)

data class MemoryMetrics(
    val heapUsedMb: Long = 0,
    val heapMaxMb: Long = 0,
    val nativeUsedMb: Long = 0
)

data class BatteryMetrics(
    val percentage: Int = 0,
    val isCharging: Boolean = false,
    val currentNowMa: Int = 0
)

data class ThermalMetrics(
    val status: Int = 0, // Maps to PowerManager.THERMAL_STATUS_*
    val isThrottling: Boolean = false
)

data class AiRuntimeMetrics(
    val averageInferenceTimeMs: Long = 0,
    val tasksQueued: Int = 0,
    val tasksCompleted: Int = 0,
    val tasksFailed: Int = 0
)
