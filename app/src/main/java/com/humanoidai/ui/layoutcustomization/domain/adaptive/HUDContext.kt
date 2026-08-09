package com.humanoidai.ui.layoutcustomization.domain.adaptive

import androidx.compose.runtime.Immutable

/**
 * Capture of the current HUD environment.
 * The single source of truth for adaptive decisions.
 */
@Immutable
data class HUDContext(
    val camera: CameraState = CameraState.IDLE,
    val detection: DetectionState = DetectionState.NONE,
    val assistant: AssistantState = AssistantState.IDLE,
    val alertLevel: AlertLevel = AlertLevel.NONE,
    val userInteraction: InteractionState = InteractionState.IDLE,
    val orientation: String = "PORTRAIT",
    val timestamp: Long = System.currentTimeMillis()
)

enum class CameraState { IDLE, ACTIVE, PAUSED, RECORDING }
enum class DetectionState { NONE, SINGLE_OBJECT, MULTIPLE_OBJECTS }
enum class AssistantState { IDLE, LISTENING, THINKING, SPEAKING }
enum class AlertLevel { NONE, LOW, MEDIUM, HIGH, CRITICAL }
enum class InteractionState { IDLE, TOUCHING, DRAGGING, CUSTOMIZING }
