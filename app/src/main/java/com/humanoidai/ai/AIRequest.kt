package com.humanoidai.ai

/**
 * Encapsulates a request to an AI provider
 */
data class AIRequest(
    val prompt: String,
    val requestId: Long = System.currentTimeMillis(),
    val temperature: Float = 0.7f,
    val maxTokens: Int = 512,
    val stopSequences: List<String> = emptyList(),
    val systemInstructions: String? = null,
    val history: List<ChatMessage> = emptyList(),
    val visionContext: VisionContext? = null,
    val type: AIRequestType = AIRequestType.GENERAL_CONVERSATION,
    val conversationId: String? = null,
    val attachments: List<Any> = emptyList(),
    val imageData: ByteArray? = null,
    val audioData: ByteArray? = null,
    val videoData: ByteArray? = null,
    val locationData: Map<String, Double>? = null,
    val requiresVision: Boolean = false,
    val requiresAudio: Boolean = false,
    val requiresVideo: Boolean = false,
    val requiresReasoning: Boolean = false,
    val requiresCoding: Boolean = false,
    val requiresLongContext: Boolean = false,
    val requiresRealtime: Boolean = false,
    val priority: Int = 0,
    val preferredLatency: Long = 2000L,
    val offlineAllowed: Boolean = true
)

/**
 * Categorization of AI requests for intelligent routing.
 */
enum class AIRequestType {
    GENERAL_CONVERSATION,
    QUICK_COMMAND,
    REASONING,
    VISION,
    PRIVATE_DATA,
    CURRENT_INFORMATION,
    CODING,
    LONG_CONTEXT
}

/**
 * Metadata for visual context in AI requests
 */
data class VisionContext(
    val description: String,
    val objectsDetected: List<String> = emptyList(),
    val isOwnerPresent: Boolean = false
)
