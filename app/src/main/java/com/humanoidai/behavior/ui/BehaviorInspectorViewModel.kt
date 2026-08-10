package com.humanoidai.behavior.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.humanoidai.behavior.BehaviorEngine
import com.humanoidai.behavior.CommunicationLevel
import com.humanoidai.behavior.ConversationTurn
import com.humanoidai.behavior.InteractionTurnState
import com.humanoidai.behavior.PersonalityProfile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class BehaviorInspectorViewModel(private val engine: BehaviorEngine) : ViewModel() {

    val level: StateFlow<CommunicationLevel> = engine.communicationLevel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CommunicationLevel.BALANCED)

    val personality: StateFlow<PersonalityProfile> = engine.personality
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PersonalityProfile.FRIENDLY)

    val turnState: StateFlow<InteractionTurnState> = engine.turnManager.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InteractionTurnState.IDLE)

    val history: StateFlow<List<ConversationTurn>> = engine.conversationManager.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setLevel(level: CommunicationLevel) {
        engine.setCommunicationLevel(level)
    }

    fun setPersonality(profile: PersonalityProfile) {
        engine.setPersonality(profile)
    }

    fun clearHistory() {
        engine.conversationManager.clearHistory()
    }

    class Factory(private val engine: BehaviorEngine) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BehaviorInspectorViewModel(engine) as T
        }
    }
}
