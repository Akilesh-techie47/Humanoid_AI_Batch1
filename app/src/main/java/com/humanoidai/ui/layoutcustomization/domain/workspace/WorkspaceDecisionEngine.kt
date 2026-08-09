package com.humanoidai.ui.layoutcustomization.domain.workspace

import android.util.Log
import com.humanoidai.ui.layoutcustomization.domain.adaptive.*
import com.humanoidai.ui.layoutcustomization.domain.hud.HUDComponentRegistry
import com.humanoidai.ui.layoutcustomization.domain.hud.HUDComponentState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Selects the most appropriate workspace based on environmental context.
 * Part of Phase 3C.
 */
class WorkspaceDecisionEngine {

    companion object {
        private const val TAG = "WorkspaceEngine"
        private const val HYSTERESIS_DELAY_MS = 2000L
    }

    private val profiles = mapOf(
        WorkspaceType.IDLE to WorkspaceProfile(
            type = WorkspaceType.IDLE,
            name = "Idle Monitoring",
            priority = WorkspacePriority.IDLE,
            preferredStates = mapOf(
                HUDComponentRegistry.ASSISTANT to HUDComponentState.COLLAPSED,
                HUDComponentRegistry.ALERTS to HUDComponentState.HIDDEN,
                HUDComponentRegistry.PRIMARY_ROI to HUDComponentState.HIDDEN
            ),
            preferredDensity = "Minimal"
        ),
        WorkspaceType.DETECTION to WorkspaceProfile(
            type = WorkspaceType.DETECTION,
            name = "Object Analysis",
            priority = WorkspacePriority.DETECTION,
            preferredStates = mapOf(
                HUDComponentRegistry.PRIMARY_ROI to HUDComponentState.FOCUSED,
                HUDComponentRegistry.SECONDARY_ROI to HUDComponentState.COMPACT,
                HUDComponentRegistry.ASSISTANT to HUDComponentState.COLLAPSED
            ),
            preferredDensity = "Balanced"
        ),
        WorkspaceType.CONVERSATION to WorkspaceProfile(
            type = WorkspaceType.CONVERSATION,
            name = "Human Conversation",
            priority = WorkspacePriority.CONVERSATION,
            preferredStates = mapOf(
                HUDComponentRegistry.ASSISTANT to HUDComponentState.EXPANDED,
                HUDComponentRegistry.PRIMARY_ROI to HUDComponentState.COMPACT,
                HUDComponentRegistry.ALERTS to HUDComponentState.COMPACT
            ),
            preferredDensity = "Balanced"
        ),
        WorkspaceType.SECURITY to WorkspaceProfile(
            type = WorkspaceType.SECURITY,
            name = "Security Monitoring",
            priority = WorkspacePriority.SECURITY,
            preferredStates = mapOf(
                HUDComponentRegistry.ALERTS to HUDComponentState.EXPANDED,
                HUDComponentRegistry.PRIMARY_ROI to HUDComponentState.FOCUSED,
                HUDComponentRegistry.SECONDARY_ROI to HUDComponentState.COMPACT
            ),
            preferredDensity = "Expanded"
        )
    )

    private val _currentWorkspace = MutableStateFlow(profiles[WorkspaceType.IDLE]!!)
    val currentWorkspace: StateFlow<WorkspaceProfile> = _currentWorkspace.asStateFlow()

    private var lastSwitchTime = 0L

    /**
     * Evaluates context and potentially switches workspace.
     */
    fun evaluate(context: HUDContext): List<HUDAction> {
        val candidate = selectProfile(context)
        
        if (candidate.type != _currentWorkspace.value.type) {
            val now = System.currentTimeMillis()
            if (now - lastSwitchTime > HYSTERESIS_DELAY_MS) {
                Log.i(TAG, "Switching Workspace: ${_currentWorkspace.value.name} -> ${candidate.name}")
                _currentWorkspace.value = candidate
                lastSwitchTime = now
                return generateWorkspaceActions(candidate)
            }
        }
        
        return emptyList()
    }

    private fun selectProfile(context: HUDContext): WorkspaceProfile {
        return when {
            context.alertLevel == AlertLevel.CRITICAL || context.alertLevel == AlertLevel.HIGH -> profiles[WorkspaceType.SECURITY]!!
            context.assistant == AssistantState.SPEAKING || context.assistant == AssistantState.LISTENING -> profiles[WorkspaceType.CONVERSATION]!!
            context.detection != DetectionState.NONE -> profiles[WorkspaceType.DETECTION]!!
            else -> profiles[WorkspaceType.IDLE]!!
        }
    }

    private fun generateWorkspaceActions(profile: WorkspaceProfile): List<HUDAction> {
        return profile.preferredStates.map { (id, state) ->
            HUDAction.ChangeState(id, state)
        }
    }
}
