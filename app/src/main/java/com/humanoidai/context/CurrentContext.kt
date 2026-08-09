package com.humanoidai.context

import com.humanoidai.companion.CompanionState
import com.humanoidai.ai.ChatMessage

/**
 * The single source of truth for the entire Companion Engine.
 * Every sensor writes to it. Gemini reads from it.
 */
data class CurrentContext(
    val ownerName: String = "Unknown",
    val ownerPresent: Boolean = false,
    val ownerLastSeenAt: Long = 0L,
    val visiblePeople: List<PersonContext> = emptyList(),
    val primarySubject: PersonContext? = null,
    val unknownCount: Int = 0,
    val conversationHistory: List<ChatMessage> = emptyList(),
    val lastSpokenAt: Long = 0L,
    val batteryPercent: Int = -1,
    val isCharging: Boolean = false,
    val ambientLight: Float = -1f,
    val noiseLevel: Float = -1f,
    val currentTime: String = "",
    val currentDate: String = "",
    val upcomingEvents: List<String> = emptyList(), // Placeholder for Calendar Events
    val recentAlerts: List<String> = emptyList(),   // Placeholder for Alert Items
    val companionState: CompanionState = CompanionState.SLEEPING,
    val lastWakeWordAt: Long = 0L,
    val activeConversation: Boolean = false,
    val preferredLanguage: String = "auto" // "en", "ta", or "auto"
)
