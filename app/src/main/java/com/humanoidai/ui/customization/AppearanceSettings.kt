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
    val themeId: String = "t8", // Changed to t8 (Pure Modern) as default
    val colorTheme: String = "monochrome",
    val accentColor: Long = 0xFFFFFFFF,
    val backgroundOpacity: Float = 0.98f,
    val cardTransparency: Float = 0.3f,
    val glassBlurStrength: Dp = 16.dp,
    val cornerRadius: Dp = 8.dp, // Sharper, cleaner corners as requested
    val hudGlowIntensity: Float = 0.4f,
    val glowEnabled: Boolean = false, // Less glow for premium feel
    val maxRoi: Int = 4,
    
    // ── Structural Geometry (New v1.6) ──────────────────────────────────────
    val hudStructure: HUDStructure = HUDStructure.SYMMETRIC_GRID,
    
    // ── HUD Components ───────────────────────────────────────────────────────
    val showGrid: Boolean = false,
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
    val radarSweepEnabled: Boolean = true,

    // ── Theme Engine (v1.7) ────────────────────────────────────────────────
    val isDarkMode: Boolean = true,
    val masterPassword: String? = null,

    // ── AI Engine (Phase 2 & 3) ──────────────────────────────────────────────
    val aiMode: AIMode = AIMode.AUTOMATIC,
    val ollamaEndpoint: String = "http://192.168.43.217:11434",
    val ollamaModel: String = "phi:latest",
    val groqModel: String = "openai/gpt-oss-20b",
    val openrouterModel: String = "google/gemini-flash-1.5",
    val cloudPriority: List<String> = listOf("ollama-local", "groq-cloud", "gemini-1.5-flash")
)

enum class AIMode {
    AUTOMATIC, LOCAL_ONLY, CLOUD_ONLY
}

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
