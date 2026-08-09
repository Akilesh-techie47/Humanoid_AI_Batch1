package com.humanoidai.ui.layoutcustomization.domain.intent

import android.util.Log
import com.humanoidai.ui.layoutcustomization.domain.adaptive.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Predicts user intent based on fused signals.
 * Part of Phase 3D.
 */
class PredictiveIntentEngine {

    companion object {
        private const val TAG = "PredictiveEngine"
        private const val PREDICTION_COOLDOWN_MS = 1500L
    }

    private val _state = MutableStateFlow(IntentState())
    val state: StateFlow<IntentState> = _state.asStateFlow()

    /**
     * Fuses context signals to estimate user intent.
     */
    fun evaluate(context: HUDContext) {
        val now = System.currentTimeMillis()
        if (now - _state.value.lastPredictionTime < PREDICTION_COOLDOWN_MS) return

        val candidates = mutableListOf<IntentPrediction>()

        // Rule: Conversation Likely
        if (context.assistant == AssistantState.LISTENING || context.detection != DetectionState.NONE) {
            candidates.add(
                IntentPrediction(
                    category = IntentCategory.CONVERSATION,
                    confidence = if (context.assistant == AssistantState.LISTENING) IntentConfidence.HIGH else IntentConfidence.MEDIUM,
                    reason = "Human presence and assistant activity detected."
                )
            )
        }

        // Rule: Monitoring Likely
        if (context.detection == DetectionState.MULTIPLE_OBJECTS || context.alertLevel != AlertLevel.NONE) {
            candidates.add(
                IntentPrediction(
                    category = IntentCategory.MONITORING,
                    confidence = if (context.alertLevel != AlertLevel.NONE) IntentConfidence.VERY_HIGH else IntentConfidence.MEDIUM,
                    reason = "Complex environment or active alerts detected."
                )
            )
        }

        // Rule: Scanning Likely
        if (context.camera == CameraState.ACTIVE && context.detection == DetectionState.SINGLE_OBJECT) {
            candidates.add(
                IntentPrediction(
                    category = IntentCategory.SCANNING,
                    confidence = IntentConfidence.MEDIUM,
                    reason = "Stable single object tracking in active camera."
                )
            )
        }

        val best = candidates.sortedByDescending { it.confidence }.firstOrNull() 
            ?: IntentPrediction(IntentCategory.IDLE, IntentConfidence.LOW)

        _state.update { 
            it.copy(
                activeIntent = best.copy(status = PredictionStatus.VALIDATED),
                candidates = candidates,
                lastPredictionTime = now
            )
        }
        
        if (best.category != IntentCategory.IDLE) {
            Log.d(TAG, "New Prediction: ${best.category} (${best.confidence}) - ${best.reason}")
        }
    }
}
