package com.humanoidai.ui.customization

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * CEA v1.5 Master Appearance Specification.
 * Controls every visual aspect of the AI Operating System.
 */
data class AppearanceSettings(
    // ── Layout ───────────────────────────────────────────────────────────────
    val layoutPreset: String = "classic", // classic, minimal, split, radial, cards, security, widget, focus, zones, chat_first
    val uiScale: UIScale = UIScale.MEDIUM,
    
    // ── Theme ────────────────────────────────────────────────────────────────
    val themeId: String = "t1", // t1..t10 — the 10 camera-home visual personalities
    val colorTheme: String = "core_monolith", // legacy, superseded by themeId
    val accentColor: Long = 0xFFFFB300,
    val backgroundOpacity: Float = 0.95f,
    val cardTransparency: Float = 0.5f,
    val glassBlurStrength: Dp = 12.dp,
    val cornerRadius: Dp = 20.dp,
    val hudGlowIntensity: Float = 0.8f,
    val glowEnabled: Boolean = true,
    val maxRoi: Int = 3,
    
    // ── Structural Geometry (New v1.6) ──────────────────────────────────────
    val hudStructure: HUDStructure = HUDStructure.TOP_APERTURE, // TOP_APERTURE, SIDE_SPLIT, CORNER_TACTICAL, BOTTOM_FOCUS, CENTRAL_HUB, MINIMAL_FLOATING, DATA_STACK, SYMMETRIC_GRID
    
    // ── HUD Components ───────────────────────────────────────────────────────
    val showGrid: Boolean = true,
    val showRadar: Boolean = true,
    val showHeatmap: Boolean = false,
    val showRoiBox: Boolean = true,
    val showConfidence: Boolean = true,
    val showBrackets: Boolean = true,
    val showSensorStatus: Boolean = true,
    val showFpsCounter: Boolean = false,
    val showAiStatus: Boolean = true,
    val showAlertBanner: Boolean = true,
    val showLiveContext: Boolean = true,
    val showMiniAssistant: Boolean = false,
    
    // ── Animations ───────────────────────────────────────────────────────────
    val animationProfile: AnimationProfile = AnimationProfile.BALANCED,
    val cameraScanAnim: Boolean = true,
    val radarSweepEnabled: Boolean = true
)

enum class UIScale(val factor: Float) {
    SMALL(0.85f), MEDIUM(1.0f), LARGE(1.2f)
}

enum class AnimationProfile(val speedMult: Float) {
    MINIMAL(0.5f), BALANCED(1.0f), HIGH(1.5f)
}

enum class HUDStructure {
    TOP_APERTURE,       // Camera Top, Grid Center (Original)
    SIDE_SPLIT,         // Camera Left, Data Right
    CORNER_TACTICAL,    // Camera Small Corner, Data Dominant
    BOTTOM_FOCUS,       // Camera Bottom, Grid Top
    CENTRAL_HUB,        // Everything Centralized
    MINIMAL_FLOATING,   // Tiny Camera Hub anywhere
    DATA_STACK,         // Camera background, Vertical Data Strip
    SYMMETRIC_GRID      // Multi-zone balanced grid
}
