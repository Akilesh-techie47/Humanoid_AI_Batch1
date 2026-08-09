package com.humanoidai.ui.customization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.content.Context
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppearanceViewModel(context: Context) : ViewModel() {
    
    private val dataStore = AppearanceDataStore(context.applicationContext)
    
    val settings: StateFlow<AppearanceSettings> = dataStore.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppearanceSettings()
    )

    fun setLayout(preset: String) {
        viewModelScope.launch { dataStore.updateLayout(preset) }
    }

    fun setStructure(structure: HUDStructure) {
        viewModelScope.launch { dataStore.updateStructure(structure) }
    }

    fun setTheme(theme: String, accent: Long) {
        viewModelScope.launch { dataStore.updateTheme(theme, accent) }
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
