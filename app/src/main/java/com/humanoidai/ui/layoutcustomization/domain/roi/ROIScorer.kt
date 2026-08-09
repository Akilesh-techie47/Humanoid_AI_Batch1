package com.humanoidai.ui.layoutcustomization.domain.roi

import android.util.Log

/**
 * Logic for calculating the importance of an ROI candidate.
 */
class ROIScorer {
    
    companion object {
        private const val WEIGHT_CONFIDENCE = 0.4f
        private const val WEIGHT_STABILITY = 0.3f
        private const val WEIGHT_PERSISTENCE = 0.2f
        private const val WEIGHT_DISTANCE = 0.1f
    }

    fun calculateScore(candidate: ROICandidate): Float {
        val persistence = (System.currentTimeMillis() - candidate.firstSeen).coerceAtMost(5000L) / 5000f
        
        // Simplified scoring for Phase 3B
        val score = (candidate.confidence * WEIGHT_CONFIDENCE) +
                    (candidate.trackingConfidence * WEIGHT_STABILITY) +
                    (persistence * WEIGHT_PERSISTENCE)
        
        return score
    }
}
