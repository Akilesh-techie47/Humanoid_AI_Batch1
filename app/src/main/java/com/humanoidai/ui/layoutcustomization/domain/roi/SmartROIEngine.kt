package com.humanoidai.ui.layoutcustomization.domain.roi

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Intelligent selector for Primary and Secondary ROIs.
 * Implements hysteresis and stability logic.
 */
class SmartROIEngine(
    private val scorer: ROIScorer = ROIScorer()
) {
    companion object {
        private const val TAG = "SmartROI"
        private const val HYSTERESIS_THRESHOLD = 0.15f
        private const val MAX_SECONDARY_ROIS = 3
        private const val TARGET_LOSS_TIMEOUT_MS = 1500L
    }

    private val _decisions = MutableStateFlow<List<ROIDecision>>(emptyList())
    val decisions: StateFlow<List<ROIDecision>> = _decisions.asStateFlow()

    private var currentPrimaryId: String? = null

    /**
     * Processes new detections and updates ROI decisions.
     */
    fun processDetections(candidates: List<ROICandidate>) {
        val scoredCandidates = candidates.map { it to scorer.calculateScore(it) }
            .sortedByDescending { it.second }

        if (scoredCandidates.isEmpty()) {
            _decisions.value = emptyList()
            currentPrimaryId = null
            return
        }

        val newPrimary = selectPrimary(scoredCandidates)
        val secondary = scoredCandidates.filter { it.first.id != newPrimary.id }
            .take(MAX_SECONDARY_ROIS)
            .map { (candidate, score) ->
                ROIDecision(candidate.id, false, score, ROIState.TRACKING, candidate)
            }

        _decisions.value = listOf(newPrimary) + secondary
        currentPrimaryId = newPrimary.id
    }

    private fun selectPrimary(scored: List<Pair<ROICandidate, Float>>): ROIDecision {
        val top = scored.first()
        val currentPrimary = scored.find { it.first.id == currentPrimaryId }

        // Hysteresis: Only switch if the new top is significantly better than current primary
        val selected = if (currentPrimary != null && (top.second - currentPrimary.second) < HYSTERESIS_THRESHOLD) {
            currentPrimary
        } else {
            top
        }

        return ROIDecision(
            id = selected.first.id,
            isPrimary = true,
            score = selected.second,
            state = ROIState.FOCUSED,
            candidate = selected.first
        )
    }
}
