package com.humanoidai.behavior

/**
 * Defines how much information the AI should provide.
 */
enum class CommunicationLevel {
    /** Only essential alerts and direct answers. */
    MINIMAL,
    /** Brief explanations plus actions. */
    BALANCED,
    /** Detailed reasoning, alternatives, and recommendations. */
    DETAILED
}

/**
 * Configurable interaction profile for presentation style.
 */
enum class PersonalityProfile {
    PROFESSIONAL,
    FRIENDLY,
    CONCISE,
    EDUCATIONAL,
    DEVELOPER
}

/**
 * Current state of the interaction turn.
 */
enum class InteractionTurnState {
    IDLE,
    LISTENING,
    THINKING,
    RESPONDING,
    WAITING_FOR_CLARIFICATION
}

/**
 * Represetns a multimodal response to be delivered.
 */
data class InteractionResponse(
    val text: String,
    val spokenText: String = text,
    val channels: Set<InteractionChannel> = setOf(InteractionChannel.TEXT, InteractionChannel.VOICE),
    val priority: InteractionPriority = InteractionPriority.NORMAL,
    val metadata: Map<String, Any> = emptyMap()
)

enum class InteractionChannel {
    TEXT,   // Assistant panel
    VOICE,  // TTS
    HUD,    // Visual overlays/animations
    NOTIFICATION // System notification
}

enum class InteractionPriority {
    LOW,
    NORMAL,
    HIGH,
    CRITICAL
}
