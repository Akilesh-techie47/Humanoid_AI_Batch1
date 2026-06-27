package com.humanoidai.companion

import kotlinx.coroutines.flow.StateFlow

/**
 * Facade class that exposes the Companion Engine to the UI layer cleanly.
 */
class CompanionManager(private val engine: CompanionEngine) {
    
    val state: StateFlow<CompanionState> = engine.state
    val context = engine.contextFlow

    fun wake() = engine.wake()
    fun sleep() = engine.sleep()
}
