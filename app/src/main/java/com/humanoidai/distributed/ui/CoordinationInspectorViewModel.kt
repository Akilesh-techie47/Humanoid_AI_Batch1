package com.humanoidai.distributed.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.humanoidai.distributed.AIInstance
import com.humanoidai.distributed.CoordinationManager
import com.humanoidai.distributed.SyncPolicy
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class CoordinationInspectorViewModel(private val manager: CoordinationManager) : ViewModel() {

    val instances: StateFlow<Map<String, AIInstance>> = manager.registry.instances
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val syncPolicy: StateFlow<SyncPolicy> = manager.syncPolicy
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SyncPolicy.LOCAL_ONLY)

    val localInstanceId: String = manager.localInstanceId

    fun setSyncPolicy(policy: SyncPolicy) {
        manager.setSyncPolicy(policy)
    }

    fun addMockInstance(instance: AIInstance) {
        manager.registry.registerInstance(instance)
    }

    fun removeInstance(instanceId: String) {
        manager.registry.unregisterInstance(instanceId)
    }

    class Factory(private val manager: CoordinationManager) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CoordinationInspectorViewModel(manager) as T
        }
    }
}
