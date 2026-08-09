package com.humanoidai.ui.layoutcustomization.domain.workspace

import androidx.compose.runtime.Immutable
import com.humanoidai.ui.layoutcustomization.domain.hud.HUDComponentState

/**
 * Types of specialized workspaces.
 */
enum class WorkspaceType {
    IDLE,
    DETECTION,
    CONVERSATION,
    SECURITY,
    NAVIGATION,
    DEVELOPER
}

/**
 * Definition of a workspace's intended HUD configuration.
 */
@Immutable
data class WorkspaceProfile(
    val type: WorkspaceType,
    val name: String,
    val priority: Int,
    val preferredStates: Map<String, HUDComponentState>,
    val preferredDensity: String? = null,
    val motionProfile: String? = null
)

/**
 * Priorities for workspace selection.
 */
object WorkspacePriority {
    const val SECURITY = 100
    const val ALERT = 90
    const val CONVERSATION = 80
    const val DETECTION = 60
    const val NAVIGATION = 40
    const val IDLE = 10
}
