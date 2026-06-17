package com.humanoidai.context

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.humanoidai.vision.DetectedPerson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

/**
 * The central brain memory. Every module writes here.
 * Nothing talks directly to Gemini; everything updates Context.
 */
class ContextEngine(private val context: Context) {

    private val _currentContext = MutableStateFlow(CurrentContext())
    val currentContext: StateFlow<CurrentContext> = _currentContext.asStateFlow()

    fun updateFromVision(visiblePeople: List<DetectedPerson>) {
        val now = Date()
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())

        _currentContext.value = _currentContext.value.copy(
            visiblePeople = visiblePeople.map { it.name },
            primaryPerson = visiblePeople.find { it.isPrimary }?.name,
            unknownCount = visiblePeople.count { it.name == "UNKNOWN" },
            time = timeFormat.format(now),
            date = dateFormat.format(now),
            cameraState = if (visiblePeople.isNotEmpty()) "Active - Subjects Detected" else "Active - Scanning",
            batteryLevel = getBatteryLevel(),
            // Mocking for now, could be linked to actual sensors
            noiseLevel = if (visiblePeople.size > 2) "Loud" else "Quiet",
            brightness = "Normal"
        )
    }

    fun updateScreen(screenName: String) {
        _currentContext.value = _currentContext.value.copy(currentScreen = screenName)
    }

    fun updateAlerts(alertCount: Int) {
        _currentContext.value = _currentContext.value.copy(currentAlerts = alertCount)
    }

    fun updateOwner(name: String) {
        _currentContext.value = _currentContext.value.copy(ownerName = name)
    }

    private fun getBatteryLevel(): Int {
        val batteryStatus: Intent? = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return batteryStatus?.let { intent ->
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            (level * 100 / scale.toFloat()).toInt()
        } ?: -1
    }
}
