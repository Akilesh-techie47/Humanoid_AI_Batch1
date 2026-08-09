package com.humanoidai.runtime.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.humanoidai.runtime.AIRuntimeManager
import com.humanoidai.runtime.HealthState
import com.humanoidai.runtime.PerformanceMetrics
import com.humanoidai.runtime.RuntimeProfile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class RuntimeInspectorViewModel(private val manager: AIRuntimeManager) : ViewModel() {

    val metrics: StateFlow<PerformanceMetrics> = manager.monitor.metrics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PerformanceMetrics())

    val profile: StateFlow<RuntimeProfile> = manager.policyEngine.currentProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RuntimeProfile.BALANCED)

    val health: StateFlow<HealthState> = manager.healthState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HealthState.HEALTHY)

    fun setProfile(profile: RuntimeProfile) {
        manager.policyEngine.setUserProfile(profile)
    }
}
