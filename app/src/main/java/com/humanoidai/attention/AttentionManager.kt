package com.humanoidai.attention

import com.humanoidai.context.CurrentContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Decides what deserves the AI's attention right now.
 * Calculates focus based on environmental triggers.
 */
class AttentionManager {

    private val _currentFocus = MutableStateFlow(AttentionFocus.ENVIRONMENT)
    val currentFocus: StateFlow<AttentionFocus> = _currentFocus.asStateFlow()

    fun evaluate(context: CurrentContext) {
        val newFocus = when {
            context.recentAlerts.isNotEmpty() -> AttentionFocus.ALERT
            context.unknownCount > 0 -> AttentionFocus.UNKNOWN_PERSON
            context.visiblePeople.isNotEmpty() -> AttentionFocus.SOCIAL
            else -> AttentionFocus.ENVIRONMENT
        }
        _currentFocus.value = newFocus
    }
}

enum class AttentionFocus {
    ENVIRONMENT,
    SOCIAL,
    UNKNOWN_PERSON,
    ALERT,
    CONVERSATION
}
