package com.humanoidai.ui.layoutcustomization.domain.perception

import androidx.compose.runtime.Immutable
import com.humanoidai.ui.layoutcustomization.domain.roi.ROIDecision

/**
 * Qualitative confidence levels for perception signals.
 */
enum class PerceptionConfidence {
    UNKNOWN,
    LOW,
    MEDIUM,
    HIGH,
    VERY_HIGH
}

/**
 * Health status of a perception collector.
 */
enum class CollectorStatus {
    ACTIVE,
    INACTIVE,
    DEGRADED,
    ERROR
}

/**
 * Unified model of the AI's current environment.
 */
@Immutable
data class PerceptionState(
    val visual: VisualPerception = VisualPerception(),
    val audio: AudioPerception = AudioPerception(),
    val environment: EnvironmentalPerception = EnvironmentalPerception(),
    val interaction: InteractionPerception = InteractionPerception(),
    val timestamp: Long = System.currentTimeMillis()
)

@Immutable
data class VisualPerception(
    val primaryROI: ROIDecision? = null,
    val secondaryROIs: List<ROIDecision> = emptyList(),
    val confidence: PerceptionConfidence = PerceptionConfidence.UNKNOWN
)

@Immutable
data class AudioPerception(
    val voiceActive: Boolean = false,
    val intensity: Float = 0f,
    val confidence: PerceptionConfidence = PerceptionConfidence.UNKNOWN
)

@Immutable
data class EnvironmentalPerception(
    val orientation: String = "PORTRAIT",
    val batteryLevel: Int = 100,
    val motionLevel: Float = 0f,
    val confidence: PerceptionConfidence = PerceptionConfidence.HIGH
)

@Immutable
data class InteractionPerception(
    val touchActive: Boolean = false,
    val activeWidgetId: String? = null,
    val confidence: PerceptionConfidence = PerceptionConfidence.HIGH
)
