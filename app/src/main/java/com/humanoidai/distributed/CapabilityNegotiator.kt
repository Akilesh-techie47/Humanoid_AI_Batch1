package com.humanoidai.distributed

import com.humanoidai.embodiment.Capability

/**
 * Logic for selecting the best instance to execute a task.
 */
class CapabilityNegotiator(private val registry: InstanceRegistry) {

    /**
     * Finds the most suitable instance ID for the required capability.
     */
    fun negotiate(required: Capability, localInstanceId: String): String {
        val candidates = registry.findInstanceByCapability(required)
        
        if (candidates.isEmpty()) return localInstanceId

        // 1. Prefer local if available and not overloaded
        val local = candidates.find { it.id == localInstanceId }
        if (local != null && local.workloadPercent < 80) {
            return localInstanceId
        }

        // 2. Select remote candidate with lowest workload and highest battery
        return candidates
            .filter { it.status == com.humanoidai.embodiment.HardwareStatus.HEALTHY }
            .sortedWith(compareBy<AIInstance> { it.workloadPercent }.thenByDescending { it.batteryLevel })
            .firstOrNull()?.id ?: localInstanceId
    }
}
