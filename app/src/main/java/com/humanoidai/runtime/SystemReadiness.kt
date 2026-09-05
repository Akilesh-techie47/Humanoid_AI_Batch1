package com.humanoidai.runtime

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SystemReadiness {
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val initializedComponents = mutableSetOf<String>()
    private val requiredComponents = setOf("TRUST", "RUNTIME", "OPENCV", "SQLITE")

    fun markReady(component: String) {
        synchronized(this) {
            initializedComponents.add(component)
            if (initializedComponents.containsAll(requiredComponents)) {
                _isReady.value = true
            }
        }
    }
}
