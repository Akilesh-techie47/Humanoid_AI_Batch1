package com.humanoidai.alerts

import android.graphics.Bitmap
import com.humanoidai.vision.DetectedPerson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// -----------------------------------------------------------------
// AlertEngine
// -----------------------------------------------------------------
// Consumes DetectedPerson list from FaceAnalyzer and generates
// real AlertItem entries when:
//   - An UNKNOWN person is detected (HIGH priority)
//   - Owner re-appears after being absent (MEDIUM priority)
//   - Multiple unknowns detected simultaneously (CRITICAL)
//
// Includes:
//   - 30s per-alert cooldown (prevents alert spam)
//   - Owner absence tracking
//   - Alert queue with unread count
// -----------------------------------------------------------------

enum class AlertPriority { CRITICAL, HIGH, MEDIUM, LOW }
enum class AlertType {
    UNKNOWN_PERSON,
    MULTIPLE_UNKNOWNS,
    OWNER_RETURNED,
    KNOWN_PERSON_ARRIVED
}

data class AlertItem(
    val id: String,
    val title: String,
    val description: String,
    val timestamp: Long,
    val priority: AlertPriority,
    val type: AlertType,
    val faceBitmap: Bitmap? = null,
    val personName: String = "",
    val personLabel: String = "",
    var isRead: Boolean = false,
)

class AlertEngine(
    private val ownerName: String,
    private val onNewAlert: (AlertItem) -> Unit
) {
    companion object {
        private const val UNKNOWN_ALERT_COOLDOWN_MS = 120_000L  // Increased to 2m to reduce agent-level annoyance
        private const val OWNER_ABSENT_THRESHOLD_MS = 10_000L   // 10s before marking owner absent
        private const val MAX_ALERTS = 100
    }

    // Alert state
    private val _alerts = MutableStateFlow<List<AlertItem>>(emptyList())
    val alerts: StateFlow<List<AlertItem>> = _alerts.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _latestAlert = MutableStateFlow<AlertItem?>(null)
    val latestAlert: StateFlow<AlertItem?> = _latestAlert.asStateFlow()


    // Cooldown tracking — key: AlertType, value: last trigger time
    private val lastAlertTime = mutableMapOf<AlertType, Long>()

    // Owner tracking
    private var ownerLastSeenAt = 0L
    private var ownerWasAbsent = false
    private var ownerAbsentAlertSent = false

    // -----------------------------------------------------------------
    // Main entry point — called every frame from EnvironmentScreen
    // -----------------------------------------------------------------
    fun processDetections(persons: List<DetectedPerson>) {
        val now = System.currentTimeMillis()
        val unknowns = persons.filter { it.name == "UNKNOWN" }
        val knowns   = persons.filter { it.name != "UNKNOWN" }
        val ownerPresent = knowns.any { it.name == ownerName }

        // 1. Owner tracking
        if (ownerPresent) {
            val wasAbsent = ownerWasAbsent
            ownerLastSeenAt = now
            ownerWasAbsent = false
            ownerAbsentAlertSent = false

            if (wasAbsent) {
                // Owner just came back
                triggerAlert(
                    type = AlertType.OWNER_RETURNED,
                    title = "Owner Returned",
                    description = "$ownerName is back in camera range.",
                    priority = AlertPriority.MEDIUM,
                    faceBitmap = persons.find { it.name == ownerName }?.faceBitmap,
                    personName = ownerName,
                    cooldownMs = 60_000L
                )
            }
        } else if (ownerLastSeenAt > 0 && ((now - ownerLastSeenAt) > OWNER_ABSENT_THRESHOLD_MS)) {
            ownerWasAbsent = true
        }

        // 2. Unknown person alerts
        when {
            unknowns.size > 2 -> {
                triggerAlert(
                    type = AlertType.MULTIPLE_UNKNOWNS,
                    title = "Multiple Unknown Persons",
                    description = "${unknowns.size} unrecognized persons detected simultaneously.",
                    priority = AlertPriority.CRITICAL,
                    faceBitmap = unknowns.firstOrNull()?.faceBitmap
                )
            }
            unknowns.size == 1 -> {
                triggerAlert(
                    type = AlertType.UNKNOWN_PERSON,
                    title = "Unknown Person Detected",
                    description = "An unrecognized person has entered the camera field of view.",
                    priority = AlertPriority.HIGH,
                    faceBitmap = unknowns[0].faceBitmap
                )
            }
        }

        // 3. Known person arrived (not owner)
        knowns.filter { it.name != ownerName }.forEach { person ->
            triggerAlert(
                type = AlertType.KNOWN_PERSON_ARRIVED,
                title = "${person.name} Detected",
                description = "${person.label} — ${person.name} is in camera range.",
                priority = AlertPriority.LOW,
                faceBitmap = person.faceBitmap,
                personName = person.name,
                personLabel = person.label,
                cooldownMs = 120_000L  // 2 min cooldown for known person arrivals
            )
        }
    }

    // -----------------------------------------------------------------
    // Alert trigger with cooldown check
    // -----------------------------------------------------------------
    private fun triggerAlert(
        type: AlertType,
        title: String,
        description: String,
        priority: AlertPriority,
        faceBitmap: Bitmap? = null,
        personName: String = "",
        personLabel: String = "",
        cooldownMs: Long = UNKNOWN_ALERT_COOLDOWN_MS
    ) {
        val now = System.currentTimeMillis()
        val lastTime = lastAlertTime[type] ?: 0L

        if (now - lastTime < cooldownMs) return  // Still in cooldown

        lastAlertTime[type] = now

        val alert = AlertItem(
            id          = java.util.UUID.randomUUID().toString(),
            title       = title,
            description = description,
            timestamp   = now,
            priority    = priority,
            type        = type,
            faceBitmap  = faceBitmap,
            personName  = personName,
            personLabel = personLabel,
            isRead      = false
        )

        val updated = (_alerts.value + alert)
            .asSequence()
            .sortedByDescending { it.timestamp }
            .take(MAX_ALERTS)
            .toList()

        _alerts.value = updated
        _unreadCount.value = updated.count { !it.isRead }
        _latestAlert.value = alert

        onNewAlert(alert)
    }


    // -----------------------------------------------------------------
    // UI helpers
    // -----------------------------------------------------------------
    fun markAllRead() {
        _alerts.value = _alerts.value.map { it.copy(isRead = true) }
        _unreadCount.value = 0
    }

    fun markRead(alertId: String) {
        _alerts.value = _alerts.value.map {
            if (it.id == alertId) it.copy(isRead = true) else it
        }
        _unreadCount.value = _alerts.value.count { !it.isRead }
    }

    fun clearAll() {
        _alerts.value = emptyList()
        _unreadCount.value = 0
    }

    fun getFormattedTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        return when {
            diff < 60_000 -> "Just now"
            diff < 3_600_000 -> "${diff / 60_000} min ago"
            diff < 86_400_000 -> "${diff / 3_600_000} hr ago"
            else -> "${diff / 86_400_000} day ago"
        }
    }
}
