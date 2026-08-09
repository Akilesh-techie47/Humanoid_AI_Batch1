package com.humanoidai.ui.layoutcustomization.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class AdaptiveHUDSettings(
    val isEnabled: Boolean = false,
    val smartPositioning: Boolean = true,
    val smartVisibility: Boolean = true,
    val smartPriority: Boolean = true,
    val smartCameraOffset: Boolean = true,
    val smartAssistantExpansion: Boolean = true,
    val smartNotificationPlacement: Boolean = true,
    val contextAwareness: Boolean = true, // reserved
    val aiFocusMode: String = "auto", // reserved
    val sceneAnalysis: Boolean = true, // reserved
    val multiPersonIntelligence: Boolean = true, // reserved
    val objectPriorityEngine: Boolean = true // reserved
)
