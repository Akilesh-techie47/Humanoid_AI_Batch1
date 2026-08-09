package com.humanoidai.ui.customization

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.appearanceDataStore: DataStore<Preferences> by preferencesDataStore(name = "appearance_settings")

class AppearanceDataStore(private val context: Context) {

    private object Keys {
        val LAYOUT_PRESET = stringPreferencesKey("layout_preset")
        val UI_SCALE = stringPreferencesKey("ui_scale")
        val COLOR_THEME = stringPreferencesKey("color_theme")
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
    }

    val settings: Flow<AppearanceSettings> = context.appearanceDataStore.data.map { prefs ->
        try {
            AppearanceSettings(
                layoutPreset = prefs[Keys.LAYOUT_PRESET] ?: "classic",
                uiScale = UIScale.valueOf(prefs[Keys.UI_SCALE] ?: "MEDIUM"),
                hudStructure = HUDStructure.valueOf(prefs[Keys.HUD_STRUCTURE] ?: "TOP_APERTURE"),
                colorTheme = prefs[Keys.COLOR_THEME] ?: "cyan_jarvis",
                accentColor = prefs[Keys.ACCENT_COLOR] ?: 0xFF00D4FF,
                backgroundOpacity = prefs[Keys.BG_OPACITY] ?: 0.95f,
                cardTransparency = prefs[Keys.CARD_TRANSPARENCY] ?: 0.4f,
                hudGlowIntensity = prefs[Keys.GLOW_INTENSITY] ?: 0.6f,
                showRadar = prefs[Keys.SHOW_RADAR] ?: true,
                showGrid = prefs[Keys.SHOW_GRID] ?: true,
                showRoiBox = prefs[Keys.SHOW_ROI_BOX] ?: true,
                showConfidence = prefs[Keys.SHOW_CONFIDENCE] ?: true,
                showAlertBanner = prefs[Keys.SHOW_ALERT_BANNER] ?: true,
                showLiveContext = prefs[Keys.SHOW_LIVE_CONTEXT] ?: true,
                showSensorStatus = prefs[Keys.SHOW_SENSOR_STATUS] ?: true,
                showFpsCounter = prefs[Keys.SHOW_FPS_COUNTER] ?: false,
                showAiStatus = prefs[Keys.SHOW_AI_STATUS] ?: true,
                animationProfile = AnimationProfile.valueOf(prefs[Keys.ANIMATION_PROFILE] ?: "BALANCED"),
                maxRoi = prefs[Keys.MAX_ROI] ?: 3,
                glowEnabled = prefs[Keys.GLOW_ENABLED] ?: true
            )
        } catch (e: Exception) {
            android.util.Log.e("AppearanceDataStore", "Error parsing settings: ${e.message}")
            AppearanceSettings() // Fallback to defaults
        }
    }

    suspend fun updateLayout(preset: String) {
        context.appearanceDataStore.edit { it[Keys.LAYOUT_PRESET] = preset }
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
}
