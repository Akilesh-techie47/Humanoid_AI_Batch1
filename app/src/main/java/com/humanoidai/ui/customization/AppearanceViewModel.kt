package com.humanoidai.ui.customization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.content.Context
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.toArgb
import com.humanoidai.ui.layouts.HomeLayoutPreset

class AppearanceViewModel(context: Context) : ViewModel() {
    
    private val dataStore = AppearanceDataStore(context.applicationContext)
    
    val settings: StateFlow<AppearanceSettings> = dataStore.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppearanceSettings()
    )

    fun setLayout(presetId: String) {
        viewModelScope.launch { 
            dataStore.updateLayout(presetId)
            val preset = HomeLayoutPreset.fromId(presetId)
            val colorLong = preset.primaryColor.toArgb().toLong() and 0xFFFFFFFFL
            dataStore.updateTheme(presetId, colorLong)
            
            // Map Layout to Theme personality (t1..t10)
            val themeId = when(presetId) {
                "classic" -> "t1"
                "minimal" -> "t2"
                "split" -> "t3"
                "radial" -> "t4"
                "cards" -> "t5"
                "security" -> "t6"
                "widget" -> "t7"
                "focus" -> "t8"
                "zones" -> "t9"
                "chat_first" -> "t10"
                else -> "t1"
            }
            dataStore.updateThemeId(themeId)
        }
    }

    fun setStructure(structure: HUDStructure) {
        viewModelScope.launch { dataStore.updateStructure(structure) }
    }

    fun setTheme(theme: String, accent: Long) {
        viewModelScope.launch { dataStore.updateTheme(theme, accent) }
    }

    fun setHudTheme(themeId: String) {
        viewModelScope.launch {
            dataStore.updateThemeId(themeId)
            // keep accentColor in sync for legacy consumers
            val hudTheme = com.humanoidai.ui.theme.HudThemes.forId(themeId)
            dataStore.updateTheme(themeId, hudTheme.accent.toArgb().toLong() and 0xFFFFFFFFL)
        }
    }

    fun toggleComponent(key: String, enabled: Boolean) {
        viewModelScope.launch { dataStore.updateComponent(key, enabled) }
    }

    fun setMaxRoi(count: Int) {
        viewModelScope.launch { dataStore.updateMaxRoi(count) }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AppearanceViewModel(context) as T
        }
    }
}
