package com.humanoidai.security.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.humanoidai.security.AiPermission
import com.humanoidai.security.AiSession
import com.humanoidai.security.SecurityEvent
import com.humanoidai.security.SecurityPolicyProfile
import com.humanoidai.security.TrustFramework
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class PrivacyDashboardViewModel(private val trust: TrustFramework) : ViewModel() {

    val currentSession: StateFlow<AiSession?> = trust.sessionManager.currentSession
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val permissions: StateFlow<Map<AiPermission, Boolean>> = trust.permissionEngine.permissions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val policy: StateFlow<SecurityPolicyProfile> = trust.privacyManager.currentPolicy
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SecurityPolicyProfile.BALANCED)

    val auditLogs: StateFlow<List<SecurityEvent>> = trust.auditLogger.events
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setPolicy(profile: SecurityPolicyProfile) {
        trust.privacyManager.setPolicy(profile)
    }

    fun refreshPermissions() {
        trust.permissionEngine.refreshPermissions()
    }

    class Factory(private val trust: TrustFramework) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PrivacyDashboardViewModel(trust) as T
        }
    }
}
