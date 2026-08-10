package com.humanoidai.planning.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.humanoidai.planning.Goal
import com.humanoidai.planning.GoalManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class GoalInspectorViewModel(private val manager: GoalManager) : ViewModel() {

    val activeGoals: StateFlow<Map<String, Goal>> = manager.activeGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun cancelGoal(goalId: String) {
        manager.cancelGoal(goalId)
    }

    fun clearCompleted() {
        manager.clearCompleted()
    }

    class Factory(private val manager: GoalManager) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GoalInspectorViewModel(manager) as T
        }
    }
}
