package com.humanoidai.ui.layoutcustomization.domain.roi

import androidx.compose.runtime.Immutable
import android.graphics.RectF

/**
 * Lifecycle states for an ROI.
 */
enum class ROIState {
    APPEARING,
    TRACKING,
    FOCUSED,
    WARNING,
    LOST,
    HIDDEN
}

/**
 * Metadata for a detected candidate that could become an ROI.
 */
@Immutable
data class ROICandidate(
    val id: String,
    val type: String,
    val confidence: Float,
    val trackingConfidence: Float = 1.0f,
    val bounds: RectF,
    val lastSeen: Long = System.currentTimeMillis(),
    val firstSeen: Long = System.currentTimeMillis(),
    val stabilityScore: Float = 1.0f,
    val distanceEstimate: Float = 0f
)


/**
 * The final decision for an ROI component.
 */
@Immutable
data class ROIDecision(
    val id: String,
    val isPrimary: Boolean,
    val score: Float,
    val state: ROIState,
    val candidate: ROICandidate
)
