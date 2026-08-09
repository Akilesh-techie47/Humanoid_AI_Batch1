package com.humanoidai.context.model

import com.humanoidai.context.PersonContext

/**
 * Shared vocabulary for the Context Engine Upgrade (CEA v1.4).
 */

enum class EmotionType { JOY, NEUTRAL, STRESS, SADNESS, EXCITEMENT, CONFUSED }
enum class EmotionSource { FACE, VOICE, FUSED }
enum class ContextEventType { 
    FACE_RECOGNIZED, 
    LAUGHTER, 
    SILENCE_PERIOD, 
    MOOD_SHIFT, 
    LOUD_NOISE, 
    PROACTIVE_ENGAGEMENT,
    OWNER_ENTERED,
    OWNER_LEFT
}
enum class TimeOfDayBucket { MORNING, AFTERNOON, EVENING, NIGHT }

data class EmotionSignal(
    val type: EmotionType,
    val confidence: Float,
    val source: EmotionSource
)

data class AudioContext(
    val isSpeechDetected: Boolean = false,
    val isLaughterDetected: Boolean = false,
    val voiceEnergyLevel: Float = 0f,
    val transcript: String? = null
)

data class ContextEvent(
    val timestamp: Long = System.currentTimeMillis(),
    val type: ContextEventType,
    val description: String,
    val relatedPersonId: String? = null
)

data class EnvironmentContext(
    val lightLevel: Float = 0f,
    val motionDetected: Boolean = false,
    val timeOfDay: TimeOfDayBucket = TimeOfDayBucket.MORNING
)

data class ContextState(
    val timestamp: Long = System.currentTimeMillis(),
    val presentPeople: List<PersonContext> = emptyList(),
    val primaryFocus: PersonContext? = null,
    val ownerEmotion: EmotionSignal? = null,
    val ambientAudio: AudioContext? = null,
    val environment: EnvironmentContext = EnvironmentContext(),
    val recentEvents: List<ContextEvent> = emptyList()
)
