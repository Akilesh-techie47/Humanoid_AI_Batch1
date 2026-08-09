package com.humanoidai.ui.layoutcustomization.domain.memory

import com.humanoidai.ui.layoutcustomization.domain.roi.ROIDecision

/**
 * Resolves natural language or intent-based references to physical objects.
 * Part of Phase 4A.
 */
class ReferenceResolver(
    private val memoryEngine: MemoryEngine
) {
    /**
     * Resolves a generic "that" or "it" reference to the current focus or most likely ROI.
     */
    fun resolveDeicticReference(roiDecisions: List<ROIDecision>): ROIDecision? {
        val memoryState = memoryEngine.state.value
        
        // 1. Check if the current cognitive focus maps to an ROI
        memoryState.activeFocusId?.let { focusId ->
            val match = roiDecisions.find { it.id == focusId }
            if (match != null) return match
        }
        
        // 2. Fallback: Return the primary ROI if it exists
        return roiDecisions.find { it.isPrimary }
    }

    /**
     * Resolves ordinal references like "the second one" or "the other person".
     */
    fun resolveOrdinalReference(index: Int, roiDecisions: List<ROIDecision>): ROIDecision? {
        val sorted = roiDecisions.sortedByDescending { it.score }
        return sorted.getOrNull(index)
    }
}
