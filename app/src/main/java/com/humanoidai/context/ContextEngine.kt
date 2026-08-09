package com.humanoidai.context

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.humanoidai.context.model.*
import com.humanoidai.memory.database.HumanoidDatabase
import com.humanoidai.memory.entities.ContextLogEntity
import com.humanoidai.vision.DetectedPerson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * CEA v1.4 Context Engine Upgrade.
 * Replaces scattered signals with a single shared fused memory and event ring buffer.
 */
class ContextEngine(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {

    private val db = HumanoidDatabase.getInstance(context)

    // ── CEA v1.4 State ───────────────────────────────────────────────────────
    
    private val _state = MutableStateFlow(ContextState())
    val state: StateFlow<ContextState> = _state.asStateFlow()

    private val eventBuffer = mutableListOf<ContextEvent>()
    private val MAX_EVENTS = 50
    private val BUFFER_TIME_MS = 30_000L

    // ── Legacy Compatibility ─────────────────────────────────────────────────
    // Keep CurrentContext flow for existing UI components until fully migrated.
    private val _currentContext = MutableStateFlow(CurrentContext())
    val currentContext: StateFlow<CurrentContext> = _currentContext.asStateFlow()

    // ── Context Methods ──────────────────────────────────────────────────────

    /**
     * Records a new context event into the working memory ring buffer and DB.
     */
    fun recordEvent(type: ContextEventType, description: String, personId: String? = null) {
        val now = System.currentTimeMillis()
        val event = ContextEvent(now, type, description, personId)
        
        synchronized(eventBuffer) {
            eventBuffer.add(event)
            // Retention: Last 30s or last 50 events
            eventBuffer.removeAll { now - it.timestamp > BUFFER_TIME_MS }
            if (eventBuffer.size > MAX_EVENTS) {
                eventBuffer.removeAt(0)
            }
            
            _state.value = _state.value.copy(
                timestamp = now,
                recentEvents = eventBuffer.toList()
            )
        }

        // Persistence for Gap Detection & Recovery
        scope.launch {
            db.contextLogDao().insert(
                ContextLogEntity(
                    timestamp = now,
                    eventType = type.name,
                    environmentData = description
                )
            )
        }
    }

    fun updateFromVision(visiblePeople: List<DetectedPerson>) {
        val now = Date()
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())

        val personContexts = visiblePeople.map { person ->
            PersonContext(
                name = person.name,
                label = person.label,
                confidence = person.confidence,
                isPrimary = person.isPrimary,
                distanceCategory = person.distanceCategory,
                isLookingAtCamera = person.isLookingAtCamera
            )
        }

        // 1. Update Legacy State
        _currentContext.value = _currentContext.value.copy(
            visiblePeople = personContexts,
            primarySubject = personContexts.find { it.isPrimary },
            unknownCount = visiblePeople.count { it.name == "UNKNOWN" },
            currentTime = timeFormat.format(now),
            currentDate = dateFormat.format(now),
            batteryPercent = getBatteryLevel(),
            noiseLevel = if (visiblePeople.size > 2) 75f else 30f
        )

        // 2. Update CEA v1.4 Fused State
        _state.value = _state.value.copy(
            timestamp = now.time,
            presentPeople = personContexts,
            primaryFocus = personContexts.find { it.isPrimary }
        )

        // 3. Persistence Heartbeat
        scope.launch {
            db.contextLogDao().insert(
                ContextLogEntity(
                    timestamp = now.time,
                    eventType = "VISION_HEARTBEAT",
                    environmentData = "People count: ${personContexts.size}"
                )
            )
        }

        // 4. Automated Event Recording (Owner Entered/Left)
        val ownerName = _currentContext.value.ownerName
        if (ownerName.isNotEmpty()) {
            val isOwnerPresent = personContexts.any { it.name == ownerName }
            val previouslyPresent = _state.value.presentPeople.any { it.name == ownerName }

            if (isOwnerPresent && !previouslyPresent) {
                recordEvent(ContextEventType.OWNER_ENTERED, "$ownerName entered the field of view.")
            } else if (!isOwnerPresent && previouslyPresent) {
                recordEvent(ContextEventType.OWNER_LEFT, "$ownerName left the field of view.")
            }
        }
    }

    fun updateEmotion(type: EmotionType, confidence: Float, source: EmotionSource) {
        _state.value = _state.value.copy(
            ownerEmotion = EmotionSignal(type, confidence, source)
        )
        if (confidence > 0.7f) {
            recordEvent(ContextEventType.MOOD_SHIFT, "Owner appears to be feeling ${type.name}")
        }
    }

    fun updateAudio(audio: AudioContext) {
        _state.value = _state.value.copy(ambientAudio = audio)
        if (audio.isLaughterDetected) {
            recordEvent(ContextEventType.LAUGHTER, "Laughter detected in environment")
        }
    }

    fun updateEnvironment(lightLevel: Float, motion: Boolean) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val bucket = when {
            hour < 12 -> TimeOfDayBucket.MORNING
            hour < 17 -> TimeOfDayBucket.AFTERNOON
            hour < 21 -> TimeOfDayBucket.EVENING
            else -> TimeOfDayBucket.NIGHT
        }

        _state.value = _state.value.copy(
            environment = EnvironmentContext(
                lightLevel = lightLevel,
                motionDetected = motion,
                timeOfDay = bucket
            )
        )
    }

    fun updateScreen(screenName: String) {
        // Placeholder
    }

    fun updateAlerts(alertCount: Int) {
        // Placeholder
    }

    fun updateLanguage(lang: String) {
        _currentContext.value = _currentContext.value.copy(preferredLanguage = lang)
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
