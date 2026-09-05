package com.humanoidai.ui.layoutcustomization.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.humanoidai.ui.layoutcustomization.domain.model.*
import com.humanoidai.ui.layoutcustomization.presentation.state.LayoutCustomizationState
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.layoutCustomizationDataStore: DataStore<Preferences> by preferencesDataStore(name = "layout_customization_v2")

/**
 * Persists and retrieves the hierarchical LayoutCustomizationState.
 * CEA v2.0 - Phase 1B implementation.
 */
class LayoutCustomizationRepository(private val context: Context) {

    companion object {
        private const val TAG = "LayoutCustomization"
    }

    private object Keys {
        val SELECTED_LAYOUT = stringPreferencesKey("core.selected_layout")
        val SELECTED_PRESET = stringPreferencesKey("core.selected_preset")
        
        // Camera Category
        val CAMERA_SCALE = floatPreferencesKey("camera.scale")
        val CAMERA_OFFSET_X = floatPreferencesKey("camera.offset_x")
        val CAMERA_OFFSET_Y = floatPreferencesKey("camera.offset_y")
        
        // ROI Category
        val ROI_PRIMARY_AUTO = booleanPreferencesKey("roi.primary_auto")
        val ROI_PRIMARY_X = floatPreferencesKey("roi.primary_x")
        val ROI_PRIMARY_Y = floatPreferencesKey("roi.primary_y")
        val ROI_SECONDARY_AUTO = booleanPreferencesKey("roi.secondary_auto")
        val ROI_SECONDARY_X = floatPreferencesKey("roi.secondary_x")
        val ROI_SECONDARY_Y = floatPreferencesKey("roi.secondary_y")
        val ROI_AUTO_RESIZE = booleanPreferencesKey("roi.auto_resize")
        
        // Widget Category
        val WIDGET_DENSITY = stringPreferencesKey("widget.density")
        val WIDGET_ARRANGEMENT = stringPreferencesKey("widget.arrangement")
        
        // Motion Category
        val MOTION_PROFILE = stringPreferencesKey("motion.profile")
        
        // Adaptive Category
        val ADAPTIVE_ENABLED = booleanPreferencesKey("adaptive.enabled")
        
        // Visibility Map (JSON)
        val WIDGET_VISIBILITY = stringPreferencesKey("widget.visibility_map")
    }

    private val gson = Gson()
    private val mapType = object : TypeToken<Map<String, Boolean>>() {}.type

    val state: Flow<LayoutCustomizationState> = context.layoutCustomizationDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading Layout DataStore", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            LayoutCustomizationState(
                selectedLayout = prefs[Keys.SELECTED_LAYOUT] ?: "top_aperture",
                selectedPreset = prefs[Keys.SELECTED_PRESET] ?: "jarvis",
                camera = CameraSettings(
                    scale = prefs[Keys.CAMERA_SCALE] ?: 1.0f,
                    offsetX = prefs[Keys.CAMERA_OFFSET_X] ?: 0f,
                    offsetY = prefs[Keys.CAMERA_OFFSET_Y] ?: 0f
                ),
                roi = ROISettings(
                    primaryAutoPosition = prefs[Keys.ROI_PRIMARY_AUTO] ?: true,
                    primaryPositionX = prefs[Keys.ROI_PRIMARY_X] ?: 0f,
                    primaryPositionY = prefs[Keys.ROI_PRIMARY_Y] ?: 0f,
                    secondaryAutoPosition = prefs[Keys.ROI_SECONDARY_AUTO] ?: true,
                    secondaryPositionX = prefs[Keys.ROI_SECONDARY_X] ?: 0f,
                    secondaryPositionY = prefs[Keys.ROI_SECONDARY_Y] ?: 0f,
                    autoResize = prefs[Keys.ROI_AUTO_RESIZE] ?: true
                ),
                widget = WidgetSettings(
                    density = prefs[Keys.WIDGET_DENSITY] ?: "Balanced",
                    arrangement = prefs[Keys.WIDGET_ARRANGEMENT] ?: "Balanced",
                    visibilityMap = try {
                        val json = prefs[Keys.WIDGET_VISIBILITY]
                        if (json != null) gson.fromJson(json, mapType) else emptyMap()
                    } catch (_: Exception) { emptyMap() }
                ),
                motion = MotionSettings(
                    profile = prefs[Keys.MOTION_PROFILE] ?: "professional"
                ),
                adaptive = AdaptiveHUDSettings(
                    isEnabled = prefs[Keys.ADAPTIVE_ENABLED] ?: false
                )
            )
        }

    suspend fun saveState(state: LayoutCustomizationState) {
        context.layoutCustomizationDataStore.edit { prefs ->
            prefs[Keys.SELECTED_LAYOUT] = state.selectedLayout
            prefs[Keys.SELECTED_PRESET] = state.selectedPreset
            prefs[Keys.CAMERA_SCALE] = state.camera.scale
            prefs[Keys.CAMERA_OFFSET_X] = state.camera.offsetX
            prefs[Keys.CAMERA_OFFSET_Y] = state.camera.offsetY
            prefs[Keys.ROI_PRIMARY_AUTO] = state.roi.primaryAutoPosition
            prefs[Keys.ROI_PRIMARY_X] = state.roi.primaryPositionX
            prefs[Keys.ROI_PRIMARY_Y] = state.roi.primaryPositionY
            prefs[Keys.ROI_SECONDARY_AUTO] = state.roi.secondaryAutoPosition
            prefs[Keys.ROI_SECONDARY_X] = state.roi.secondaryPositionX
            prefs[Keys.ROI_SECONDARY_Y] = state.roi.secondaryPositionY
            prefs[Keys.ROI_AUTO_RESIZE] = state.roi.autoResize
            prefs[Keys.WIDGET_DENSITY] = state.widget.density
            prefs[Keys.WIDGET_ARRANGEMENT] = state.widget.arrangement
            prefs[Keys.WIDGET_VISIBILITY] = gson.toJson(state.widget.visibilityMap)
            prefs[Keys.MOTION_PROFILE] = state.motion.profile
            prefs[Keys.ADAPTIVE_ENABLED] = state.adaptive.isEnabled
        }
    }
}
