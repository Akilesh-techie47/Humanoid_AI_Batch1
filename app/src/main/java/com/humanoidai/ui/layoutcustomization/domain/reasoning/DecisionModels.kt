package com.humanoidai.ui.layoutcustomization.domain.reasoning

import androidx.compose.runtime.Immutable
import com.humanoidai.ui.layoutcustomization.domain.adaptive.HUDAction

/**
 * Qualitative confidence levels for decisions.
 */
enum class DecisionConfidence {
    LOW,
    MEDIUM,
    HIGH,
    VERY_HIGH
}

/**
 * Immutable atomic unit of a decision.
 */
@Immutable
data class Decision(
    val id: String,
    val category: String, // UI, Assistant, Camera, System
    val priority: Int,
    val confidence: DecisionConfidence,
    val actions: List<HUDAction>,
    val evidence: List<String> = emptyList(),
    val explanation: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val expiration: Long = -1L
)

/**
 * State of the decision engine.
 */
@Immutable
data class DecisionState(
    val activeDecisions: Map<String, Decision> = emptyMap(),
    val queue: List<Decision> = emptyList(),
    val history: List<Decision> = emptyList()
)
