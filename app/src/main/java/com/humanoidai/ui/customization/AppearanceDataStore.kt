package com.humanoidai.ui.customization

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.appearanceDataStore: DataStore<Preferences> by preferencesDataStore(name = "appearance_settings")

class AppearanceDataStore(private val context: Context) {

    private object Keys {
        val LAYOUT_PRESET = stringPreferencesKey("layout_preset")
        val UI_SCALE = stringPreferencesKey("ui_scale")
        val COLOR_THEME = stringPreferencesKey("color_theme")
        val THEME_ID = stringPreferencesKey("theme_id")
        val ACCENT_COLOR = longPreferencesKey("accent_color")
        val BG_OPACITY = floatPreferencesKey("bg_opacity")
        val CARD_TRANSPARENCY = floatPreferencesKey("card_transparency")
        val GLOW_INTENSITY = floatPreferencesKey("glow_intensity")
        val SHOW_RADAR = booleanPreferencesKey("show_radar")
        val SHOW_GRID = booleanPreferencesKey("show_grid")
        val SHOW_ROI_BOX = booleanPreferencesKey("show_roi_box")
        val SHOW_CONFIDENCE = booleanPreferencesKey("show_confidence")
        val SHOW_ALERT_BANNER = booleanPreferencesKey("show_alert_banner")
        val SHOW_LIVE_CONTEXT = booleanPreferencesKey("show_live_context")
        val SHOW_SENSOR_STATUS = booleanPreferencesKey("show_sensor_status")
        val SHOW_FPS_COUNTER = booleanPreferencesKey("show_fps_counter")
        val SHOW_AI_STATUS = booleanPreferencesKey("show_ai_status")
        val HUD_STRUCTURE = stringPreferencesKey("hud_structure")
        val ANIMATION_PROFILE = stringPreferencesKey("animation_profile")
        val MAX_ROI = intPreferencesKey("max_roi")
        val GLOW_ENABLED = booleanPreferencesKey("glow_enabled")
        
        // New Persistence Keys for Phase 8/9
        val GLASS_BLUR = floatPreferencesKey("glass_blur")
        val CORNER_RADIUS = floatPreferencesKey("corner_radius")
        val SHOW_HEATMAP = booleanPreferencesKey("show_heatmap")
        val SHOW_BRACKETS = booleanPreferencesKey("show_brackets")
        val SHOW_MINI_ASSISTANT = booleanPreferencesKey("show_mini_assistant")
        val CAMERA_SCAN_ANIM = booleanPreferencesKey("camera_scan_anim")
        val RADAR_SWEEP_ENABLED = booleanPreferencesKey("radar_sweep_enabled")

        // Theme Engine Keys
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val MASTER_PASSWORD = stringPreferencesKey("master_password")

        // AI Engine Keys
        val AI_MODE = stringPreferencesKey("ai_mode")
        val OLLAMA_ENDPOINT = stringPreferencesKey("ollama_endpoint")
        val OLLAMA_MODEL = stringPreferencesKey("ollama_model")
        val GROQ_MODEL = stringPreferencesKey("groq_model")
        val OPENROUTER_MODEL = stringPreferencesKey("openrouter_model")
    }

    val settings: Flow<AppearanceSettings> = context.appearanceDataStore.data.map { prefs ->
        try {
            AppearanceSettings(
                layoutPreset = prefs[Keys.LAYOUT_PRESET] ?: "classic",
                uiScale = UIScale.valueOf(prefs[Keys.UI_SCALE] ?: "MEDIUM"),
                hudStructure = HUDStructure.valueOf(prefs[Keys.HUD_STRUCTURE] ?: "SYMMETRIC_GRID"),
                themeId = prefs[Keys.THEME_ID] ?: "t8",
                colorTheme = prefs[Keys.COLOR_THEME] ?: "monochrome",
                accentColor = prefs[Keys.ACCENT_COLOR] ?: 0xFFFFFFFF,
                backgroundOpacity = prefs[Keys.BG_OPACITY] ?: 0.98f,
                cardTransparency = prefs[Keys.CARD_TRANSPARENCY] ?: 0.3f,
                hudGlowIntensity = prefs[Keys.GLOW_INTENSITY] ?: 0.4f,
                showRadar = prefs[Keys.SHOW_RADAR] ?: true,
                showGrid = prefs[Keys.SHOW_GRID] ?: false,
                showRoiBox = prefs[Keys.SHOW_ROI_BOX] ?: true,
                showConfidence = prefs[Keys.SHOW_CONFIDENCE] ?: true,
                showAlertBanner = prefs[Keys.SHOW_ALERT_BANNER] ?: true,
                showLiveContext = prefs[Keys.SHOW_LIVE_CONTEXT] ?: true,
                showSensorStatus = prefs[Keys.SHOW_SENSOR_STATUS] ?: true,
                showFpsCounter = prefs[Keys.SHOW_FPS_COUNTER] ?: false,
                showAiStatus = prefs[Keys.SHOW_AI_STATUS] ?: true,
                animationProfile = AnimationProfile.valueOf(prefs[Keys.ANIMATION_PROFILE] ?: "BALANCED"),
                maxRoi = prefs[Keys.MAX_ROI] ?: 4,
                glowEnabled = prefs[Keys.GLOW_ENABLED] ?: false,
                
                // Restoration logic
                glassBlurStrength = (prefs[Keys.GLASS_BLUR] ?: 16f).dp,
                cornerRadius = (prefs[Keys.CORNER_RADIUS] ?: 8f).dp,
                showHeatmap = prefs[Keys.SHOW_HEATMAP] ?: false,
                showBrackets = prefs[Keys.SHOW_BRACKETS] ?: true,
                showMiniAssistant = prefs[Keys.SHOW_MINI_ASSISTANT] ?: false,
                cameraScanAnim = prefs[Keys.CAMERA_SCAN_ANIM] ?: true,
                radarSweepEnabled = prefs[Keys.RADAR_SWEEP_ENABLED] ?: true,

                isDarkMode = prefs[Keys.IS_DARK_MODE] ?: true,
                masterPassword = prefs[Keys.MASTER_PASSWORD],

                aiMode = AIMode.valueOf(prefs[Keys.AI_MODE] ?: "AUTOMATIC"),
                ollamaEndpoint = prefs[Keys.OLLAMA_ENDPOINT] ?: "http://192.168.43.217:11434",
                ollamaModel = prefs[Keys.OLLAMA_MODEL] ?: "phi:latest",
                groqModel = prefs[Keys.GROQ_MODEL] ?: "openai/gpt-oss-20b",
                openrouterModel = prefs[Keys.OPENROUTER_MODEL] ?: "google/gemini-flash-1.5"
            )
        } catch (e: Exception) {
            android.util.Log.e("AppearanceDataStore", "Error parsing settings: ${e.message}")
            AppearanceSettings() // Fallback to defaults
        }
    }


    suspend fun updateLayout(preset: String) {
        context.appearanceDataStore.edit { it[Keys.LAYOUT_PRESET] = preset }
    }

    suspend fun updateScale(scale: UIScale) {
        context.appearanceDataStore.edit { it[Keys.UI_SCALE] = scale.name }
    }

    suspend fun updateStructure(structure: HUDStructure) {
        context.appearanceDataStore.edit { it[Keys.HUD_STRUCTURE] = structure.name }
    }

    suspend fun updateTheme(theme: String, accent: Long) {
        context.appearanceDataStore.edit {
            it[Keys.COLOR_THEME] = theme
            it[Keys.ACCENT_COLOR] = accent
        }
    }

    suspend fun updateThemeId(themeId: String) {
        context.appearanceDataStore.edit { it[Keys.THEME_ID] = themeId }
    }

    suspend fun updateComponent(key: String, enabled: Boolean) {
        context.appearanceDataStore.edit {
            when(key) {
                "radar" -> it[Keys.SHOW_RADAR] = enabled
                "grid" -> it[Keys.SHOW_GRID] = enabled
                "roi" -> it[Keys.SHOW_ROI_BOX] = enabled
                "confidence" -> it[Keys.SHOW_CONFIDENCE] = enabled
                "alerts" -> it[Keys.SHOW_ALERT_BANNER] = enabled
                "context" -> it[Keys.SHOW_LIVE_CONTEXT] = enabled
                "sensors" -> it[Keys.SHOW_SENSOR_STATUS] = enabled
                "fps" -> it[Keys.SHOW_FPS_COUNTER] = enabled
                "ai_status" -> it[Keys.SHOW_AI_STATUS] = enabled
                "glow" -> it[Keys.GLOW_ENABLED] = enabled
            }
        }
    }

    suspend fun updateMaxRoi(count: Int) {
        context.appearanceDataStore.edit { it[Keys.MAX_ROI] = count }
    }

    suspend fun updateGlassBlur(strength: Float) {
        context.appearanceDataStore.edit { it[Keys.GLASS_BLUR] = strength }
    }

    suspend fun updateCornerRadius(radius: Float) {
        context.appearanceDataStore.edit { it[Keys.CORNER_RADIUS] = radius }
    }

    suspend fun updateHeatmap(enabled: Boolean) {
        context.appearanceDataStore.edit { it[Keys.SHOW_HEATMAP] = enabled }
    }

    suspend fun updateBrackets(enabled: Boolean) {
        context.appearanceDataStore.edit { it[Keys.SHOW_BRACKETS] = enabled }
    }

    suspend fun updateMiniAssistant(enabled: Boolean) {
        context.appearanceDataStore.edit { it[Keys.SHOW_MINI_ASSISTANT] = enabled }
    }

    suspend fun updateCameraScanAnim(enabled: Boolean) {
        context.appearanceDataStore.edit { it[Keys.CAMERA_SCAN_ANIM] = enabled }
    }

    suspend fun updateRadarSweep(enabled: Boolean) {
        context.appearanceDataStore.edit { it[Keys.RADAR_SWEEP_ENABLED] = enabled }
    }

    suspend fun updateAiMode(mode: AIMode) {
        context.appearanceDataStore.edit { it[Keys.AI_MODE] = mode.name }
    }

    suspend fun updateOllamaEndpoint(endpoint: String) {
        context.appearanceDataStore.edit { it[Keys.OLLAMA_ENDPOINT] = endpoint }
    }

    suspend fun updateOllamaModel(model: String) {
        context.appearanceDataStore.edit { it[Keys.OLLAMA_MODEL] = model }
    }

    suspend fun updateGroqModel(model: String) {
        context.appearanceDataStore.edit { it[Keys.GROQ_MODEL] = model }
    }

    suspend fun updateOpenRouterModel(model: String) {
        context.appearanceDataStore.edit { it[Keys.OPENROUTER_MODEL] = model }
    }

    suspend fun updateDarkMode(enabled: Boolean) {
        context.appearanceDataStore.edit { it[Keys.IS_DARK_MODE] = enabled }
    }

    suspend fun updateMasterPassword(password: String) {
        context.appearanceDataStore.edit { it[Keys.MASTER_PASSWORD] = password }
    }
}

