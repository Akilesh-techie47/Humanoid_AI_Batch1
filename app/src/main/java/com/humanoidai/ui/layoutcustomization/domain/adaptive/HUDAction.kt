package com.humanoidai.ui.layoutcustomization.domain.adaptive

import com.humanoidai.ui.layoutcustomization.domain.hud.HUDComponentState

/**
 * Atomic operations the Adaptive Engine can perform on HUD components.
 */
sealed class HUDAction {
    data class ChangeState(val componentId: String, val state: HUDComponentState) : HUDAction()
    data class SetVisibility(val componentId: String, val isVisible: Boolean) : HUDAction()
    data class Highlight(val componentId: String, val intensity: Float) : HUDAction()
    data class Dim(val componentId: String, val alpha: Float) : HUDAction()
    
    // Future expansion: Move, Resize, etc.
}
