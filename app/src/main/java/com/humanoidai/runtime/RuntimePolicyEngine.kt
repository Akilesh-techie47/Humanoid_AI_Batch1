package com.humanoidai.runtime

import android.os.PowerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RuntimePolicyEngine {

    private val _currentProfile = MutableStateFlow(RuntimeProfile.BALANCED)
    val currentProfile: StateFlow<RuntimeProfile> = _currentProfile.asStateFlow()

    private var userSelectedProfile: RuntimeProfile? = null

    fun setUserProfile(profile: RuntimeProfile) {
        userSelectedProfile = profile
        _currentProfile.value = profile
    }

    fun evaluate(metrics: PerformanceMetrics) {
        // If user explicitly set a profile, we generally respect it unless there's thermal emergency
        val baseProfile = userSelectedProfile ?: determineBaseProfile(metrics)

        _currentProfile.value = when {
            metrics.thermal.status >= PowerManager.THERMAL_STATUS_EMERGENCY -> RuntimeProfile.BATTERY_SAVER
            metrics.thermal.status >= PowerManager.THERMAL_STATUS_MODERATE && baseProfile == RuntimeProfile.PERFORMANCE -> RuntimeProfile.BALANCED
            metrics.battery.percentage < 15 && !metrics.battery.isCharging -> RuntimeProfile.BATTERY_SAVER
            else -> baseProfile
        }
    }

    private fun determineBaseProfile(metrics: PerformanceMetrics): RuntimeProfile {
        return when {
            metrics.battery.isCharging -> RuntimeProfile.PERFORMANCE
            metrics.battery.percentage < 30 -> RuntimeProfile.BATTERY_SAVER
            else -> RuntimeProfile.BALANCED
        }
    }
    
    fun getHealthState(metrics: PerformanceMetrics): HealthState {
        return when {
            metrics.thermal.status >= PowerManager.THERMAL_STATUS_CRITICAL -> HealthState.DEGRADED
            metrics.cpu.utilizationPercent > 90 || metrics.ai.tasksQueued > 10 -> HealthState.BUSY
            else -> HealthState.HEALTHY
        }
    }
}

enum class HealthState {
    HEALTHY,
    BUSY,
    DEGRADED,
    RECOVERING,
    FAILED
}
