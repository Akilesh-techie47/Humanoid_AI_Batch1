package com.humanoidai.ui.layoutcustomization.domain.perception

import android.content.Context
import android.util.Log
import com.humanoidai.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The central hub for sensor fusion and multi-modal perception.
 * Part of Phase 4C.
 */
class PerceptionEngine(
    private val context: Context? = null,
    private val embodimentManager: com.humanoidai.embodiment.EmbodimentManager? = null
) {

    companion object {
        private const val TAG = "PerceptionEngine"
    }

    private val scope = CoroutineScope(Dispatchers.Default)
    private val _state = MutableStateFlow(PerceptionState())
    val state: StateFlow<PerceptionState> = _state.asStateFlow()

    init {
        scope.launch {
            _state.collect { newState ->
                embodimentManager?.updateWorldState { it.copy(perception = newState) }
            }
        }
    }

    private fun getRuntimeManager(): AIRuntimeManager? {
        return context?.let { AIRuntimeManager.getInstance(it) }
    }

    /**
     * Updates the visual modality of the perception model.
     */
    fun updateVisual(visual: VisualPerception) {
        val manager = getRuntimeManager()
        if (manager != null) {
            scope.launch {
                manager.submitTask(
                    name = "UpdateVisual",
                    priority = TaskPriority.CRITICAL,
                    category = TaskCategory.VISION
                ) {
                    _state.update { it.copy(visual = visual, timestamp = System.currentTimeMillis()) }
                }
            }
        } else {
            _state.update { it.copy(visual = visual, timestamp = System.currentTimeMillis()) }
        }
    }

    /**
     * Updates the audio modality of the perception model.
     */
    fun updateAudio(audio: AudioPerception) {
        val manager = getRuntimeManager()
        if (manager != null) {
            scope.launch {
                manager.submitTask(
                    name = "UpdateAudio",
                    priority = TaskPriority.HIGH,
                    category = TaskCategory.AUDIO
                ) {
                    _state.update { it.copy(audio = audio, timestamp = System.currentTimeMillis()) }
                }
            }
        } else {
            _state.update { it.copy(audio = audio, timestamp = System.currentTimeMillis()) }
        }
    }

    /**
     * Updates the environmental modality of the perception model.
     */
    fun updateEnvironment(env: EnvironmentalPerception) {
        val manager = getRuntimeManager()
        if (manager != null) {
            scope.launch {
                manager.submitTask(
                    name = "UpdateEnvironment",
                    priority = TaskPriority.MEDIUM,
                    category = TaskCategory.SYSTEM
                ) {
                    _state.update { it.copy(environment = env, timestamp = System.currentTimeMillis()) }
                }
            }
        } else {
            _state.update { it.copy(environment = env, timestamp = System.currentTimeMillis()) }
        }
    }

    /**
     * Updates the interaction modality of the perception model.
     */
    fun updateInteraction(interaction: InteractionPerception) {
        val manager = getRuntimeManager()
        if (manager != null) {
            scope.launch {
                manager.submitTask(
                    name = "UpdateInteraction",
                    priority = TaskPriority.MEDIUM,
                    category = TaskCategory.SYSTEM
                ) {
                    _state.update { it.copy(interaction = interaction, timestamp = System.currentTimeMillis()) }
                }
            }
        } else {
            _state.update { it.copy(interaction = interaction, timestamp = System.currentTimeMillis()) }
        }
    }
}
