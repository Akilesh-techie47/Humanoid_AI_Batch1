package com.humanoidai.ui.layoutcustomization.domain.hud

import androidx.compose.runtime.Immutable

/**
 * Supported states for any HUD component.
 * Part of Phase 2B modularization.
 */
@Immutable
enum class HUDComponentState {
    HIDDEN,
    COLLAPSED,
    COMPACT,
    EXPANDED,
    FOCUSED,
    DISABLED
}

/**
 * Categories to manage lifecycle and visibility rules.
 */
@Immutable
enum class HUDComponentCategory {
    PERMANENT, // Always visible (Camera, Bottom Dock)
    CONTEXT,   // Only when needed (Assistant, Alerts)
    DETECTION, // Appear on AI detection (ROI, Object Cards)
    DEVELOPER  // Debug only (FPS)
}

/**
 * Functional behaviors of a component.
 */
@Immutable
data class HUDComponentBehavior(
    val canMove: Boolean = false,
    val canResize: Boolean = false,
    val canCollapse: Boolean = false,
    val canHide: Boolean = true,
    val canStack: Boolean = false,
    val canFloat: Boolean = true,
    val canAnimate: Boolean = true
)
