package com.humanoidai.embodiment

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Interface representing a physical body.
 */
interface Embodiment {
    val profile: EmbodimentProfile
    val vision: VisionSensor?
    val audio: AudioSensor?
    val motion: MotionSensor?
    val display: DisplayActuator?
    val speaker: SpeakerActuator?
    val physical: PhysicalActuator?
    
    fun initialize()
    fun shutdown()
}

/**
 * Orchestrates physical embodiments and standardizes I/O.
 */
class EmbodimentManager {

    companion object {
        private const val TAG = "EmbodimentManager"
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    val capabilityRegistry = CapabilityRegistry()
    val safetyController = SafetyController(capabilityRegistry)
    
    private val _activeEmbodiment = MutableStateFlow<Embodiment?>(null)
    val activeEmbodiment: StateFlow<Embodiment?> = _activeEmbodiment.asStateFlow()

    private val _worldState = MutableStateFlow<WorldState?>(null)
    val worldState: StateFlow<WorldState?> = _worldState.asStateFlow()

    fun setEmbodiment(embodiment: Embodiment) {
        _activeEmbodiment.value?.shutdown()
        
        Log.i(TAG, "Setting active embodiment: ${embodiment.profile.id} (${embodiment.profile.type})")
        _activeEmbodiment.value = embodiment
        embodiment.initialize()
        
        capabilityRegistry.updateCapabilities(embodiment.profile.capabilities)
        
        _worldState.value = WorldState(
            activeEmbodimentId = embodiment.profile.id,
            connectedEmbodimentIds = setOf(embodiment.profile.id),
            availableCapabilities = embodiment.profile.capabilities,
        )
    }

    fun executeAction(request: ActionRequest) {
        if (!safetyController.validateAction(request)) return

        val embodiment = _activeEmbodiment.value ?: run {
            Log.e(TAG, "No active embodiment to execute action")
            return
        }

        when (request) {
            is ActionRequest.Speak -> {
                embodiment.speaker?.speak(request.text, request.priority) {
                    Log.d(TAG, "Speech finished: ${request.text.take(20)}...")
                }
            }
            is ActionRequest.Vibrate -> {
                // To be implemented via a HapticsActuator interface if added
            }
            is ActionRequest.DisplayOverlay -> {
                if (request.visible) {
                    embodiment.display?.showOverlay(request.componentId, null)
                } else {
                    embodiment.display?.hideOverlay(request.componentId)
                }
            }
            is ActionRequest.Move -> {
                embodiment.physical?.move(request.dx, request.dy, request.dz)
            }
            is ActionRequest.Rotate -> {
                embodiment.physical?.rotate(request.roll, request.pitch, request.yaw)
            }
            is ActionRequest.Flash -> {
                // To be implemented
            }
        }
    }

    fun updateWorldState(update: (WorldState) -> WorldState) {
        _worldState.update { current ->
            current?.let { update(it) } ?: current
        }
    }

    fun shutdown() {
        _activeEmbodiment.value?.shutdown()
        scope.cancel()
    }
}
