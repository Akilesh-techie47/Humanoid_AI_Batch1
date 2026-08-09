package com.humanoidai.embodiment.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.humanoidai.embodiment.Capability
import com.humanoidai.embodiment.Embodiment
import com.humanoidai.embodiment.EmbodimentManager
import com.humanoidai.embodiment.WorldState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class EmbodimentInspectorViewModel(private val manager: EmbodimentManager) : ViewModel() {

    val activeEmbodiment: StateFlow<Embodiment?> = manager.activeEmbodiment
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val worldState: StateFlow<WorldState?> = manager.worldState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val capabilities: StateFlow<Set<Capability>> = manager.capabilityRegistry.capabilities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
}
