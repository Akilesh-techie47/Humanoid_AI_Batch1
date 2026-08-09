package com.humanoidai.ui.layoutcustomization.domain.intent

import androidx.compose.runtime.Immutable

/**
 * Categories of probable user intent.
 */
enum class IntentCategory {
    OBSERVATION,
    CONVERSATION,
    SCANNING,
    MONITORING,
    NAVIGATION,
    IDLE,
    UNKNOWN
}

/**
 * Qualitative confidence levels for predictions.
 */
enum class IntentConfidence {
    LOW,
    MEDIUM,
    HIGH,
    VERY_HIGH
}

/**
 * The lifecycle state of a prediction.
 */
enum class PredictionStatus {
    CREATED,
    VALIDATED,
    PREPARED,
    ACTIVATED,
    EXPIRED
}

/**
 * Immutable snapshot of a predicted user intent.
 */
@Immutable
data class IntentPrediction(
    val category: IntentCategory,
    val confidence: IntentConfidence,
    val status: PredictionStatus = PredictionStatus.CREATED,
    val reason: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val expirationTime: Long = System.currentTimeMillis() + 10000L // Default 10s
)

/**
 * Aggregated state of the predictive engine.
 */
@Immutable
data class IntentState(
    val activeIntent: IntentPrediction = IntentPrediction(IntentCategory.IDLE, IntentConfidence.LOW),
    val candidates: List<IntentPrediction> = emptyList(),
    val lastPredictionTime: Long = 0L
)
