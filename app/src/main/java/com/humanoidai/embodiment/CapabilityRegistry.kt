package com.humanoidai.embodiment

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Registry of available capabilities across all connected embodiments.
 */
class CapabilityRegistry {

    private val _capabilities = MutableStateFlow<Set<Capability>>(emptySet())
    val capabilities: StateFlow<Set<Capability>> = _capabilities.asStateFlow()

    fun updateCapabilities(newCapabilities: Set<Capability>) {
        _capabilities.value = newCapabilities
    }

    fun hasCapability(capability: Capability): Boolean {
        return _capabilities.value.contains(capability)
    }

    fun getMissingCapabilities(required: Set<Capability>): Set<Capability> {
        return required.filter { !hasCapability(it) }.toSet()
    }
}
