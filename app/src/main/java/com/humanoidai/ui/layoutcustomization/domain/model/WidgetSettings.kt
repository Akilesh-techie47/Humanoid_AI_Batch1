package com.humanoidai.ui.layoutcustomization.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class WidgetSettings(
    val density: String = "Balanced", // Minimal, Compact, Balanced, Expanded, Command Center
    val arrangement: String = "Balanced", // Compact, Balanced, Spacious
    val visibilityMap: Map<String, Boolean> = mapOf(
        "Primary ROI" to true,
        "Secondary ROI" to true,
        "AI Assistant" to true,
        "AI Status" to true,
        "Notification Stack" to true,
        "Alert Panel" to true,
        "Object Details" to true,
        "Emotion Indicator" to true,
        "Distance Indicator" to true
    ),
    val transparencyOverride: Float = 1.0f, // reserved
    val floatingEnabled: Boolean = false, // reserved
    val snapToGrid: Boolean = true, // reserved
    val dockBehavior: String = "fixed" // reserved
)
