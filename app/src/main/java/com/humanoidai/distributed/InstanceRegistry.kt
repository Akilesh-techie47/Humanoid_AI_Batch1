package com.humanoidai.distributed

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Tracks all active and trusted AI instances in the cluster.
 */
class InstanceRegistry {

    private val _instances = MutableStateFlow<Map<String, AIInstance>>(emptyMap())
    val instances: StateFlow<Map<String, AIInstance>> = _instances.asStateFlow()

    fun registerInstance(instance: AIInstance) {
        _instances.update { it + (instance.id to instance) }
    }

    fun unregisterInstance(instanceId: String) {
        _instances.update { it - instanceId }
    }

    fun updateStatus(instanceId: String, update: (AIInstance) -> AIInstance) {
        _instances.update { current ->
            current[instanceId]?.let {
                current + (instanceId to update(it))
            } ?: current
        }
    }

    fun findInstanceByCapability(required: com.humanoidai.embodiment.Capability): List<AIInstance> {
        return _instances.value.values.filter { it.capabilities.contains(required) }
    }
}
