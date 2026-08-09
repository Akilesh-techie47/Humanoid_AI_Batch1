package com.humanoidai.ui.layoutcustomization.domain.reasoning

import androidx.compose.runtime.Immutable
import com.humanoidai.ui.layoutcustomization.domain.adaptive.HUDContext
import com.humanoidai.ui.layoutcustomization.domain.intent.IntentState
import com.humanoidai.ui.layoutcustomization.domain.memory.MemoryState
import com.humanoidai.ui.layoutcustomization.domain.roi.ROIDecision
import com.humanoidai.ui.layoutcustomization.domain.workspace.WorkspaceProfile

/**
 * High-level categories for reasoning.
 */
@Immutable
data class VisualSituation(
    val primaryROI: ROIDecision? = null,
    val secondaryROIs: List<ROIDecision> = emptyList(),
    val isMotionDetected: Boolean = false
)

@Immutable
data class SituationState(
    val visual: VisualSituation = VisualSituation(),
    val context: HUDContext = HUDContext(),
    val workspace: WorkspaceProfile? = null,
    val intent: IntentState = IntentState(),
    val memory: MemoryState = MemoryState(),
    val timestamp: Long = System.currentTimeMillis()
)
