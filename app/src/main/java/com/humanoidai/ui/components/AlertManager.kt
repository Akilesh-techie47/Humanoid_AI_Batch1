package com.humanoidai.ui.components

import androidx.compose.runtime.mutableStateListOf
import java.text.SimpleDateFormat
import java.util.*

data class AppAlert(
    val id: String = UUID.randomUUID().toString(),
    val message: String,
    val timestamp: String,
    val type: AlertType = AlertType.UNKNOWN_FACE
)

enum class AlertType {
    UNKNOWN_FACE, MOTION, SYSTEM
}

object AlertManager {
    val recentAlerts = mutableStateListOf<AppAlert>()
    
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun addAlert(message: String, type: AlertType = AlertType.UNKNOWN_FACE) {
        val newAlert = AppAlert(
            message = message,
            timestamp = timeFormat.format(Date()),
            type = type
        )
        // Add to front
        recentAlerts.add(0, newAlert)
        
        // Keep only last 10
        if (recentAlerts.size > 10) {
            recentAlerts.removeAt(recentAlerts.size - 1)
        }
    }
}
