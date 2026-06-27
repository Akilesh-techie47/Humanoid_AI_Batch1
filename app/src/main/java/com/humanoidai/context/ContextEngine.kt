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

    fun updateFromVision(visiblePeople: List<com.humanoidai.vision.DetectedPerson>) {
        val now = Date()
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())

        val personContexts = visiblePeople.map { person ->
            PersonContext(
                name = person.name,
                label = person.label,
                confidence = person.confidence,
                isPrimary = person.isPrimary
            )
        }

        _currentContext.value = _currentContext.value.copy(
            visiblePeople = personContexts,
            primarySubject = personContexts.find { it.isPrimary },
            unknownCount = visiblePeople.count { it.name == "UNKNOWN" },
            currentTime = timeFormat.format(now),
            currentDate = dateFormat.format(now),
            batteryPercent = getBatteryLevel(),
            noiseLevel = if (visiblePeople.size > 2) 75f else 30f // Simulating noise in dB
        )
    }

    fun updateScreen(screenName: String) {
        // Placeholder for screen tracking in CEA context
    }

    fun updateAlerts(alertCount: Int) {
        // Placeholder for recent alerts list
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
