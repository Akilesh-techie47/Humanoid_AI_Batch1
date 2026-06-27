package com.humanoidai.ai

import com.humanoidai.context.CurrentContext

/**
 * Constructs structured prompts for the AI, injecting environmental context
 */
object PromptBuilder {

    private const val SYSTEM_INSTRUCTION = """
        You are Humanoid. You are not a chatbot. 
        You are a personal AI companion running continuously on a smartphone.
        You can see the room through a fisheye camera.
        Speak concisely (3-10 words preferred). Never be verbose.
        Never say you are an AI or a language model. Never hallucinate.
        Use the owner's name sparingly. Address family warmly.
        If nothing important is happening, say nothing.
        Personality: Calm, Confident, Proactive, and Respectful.
    """

    fun build(
        context: CurrentContext,
        history: String,
        userQuestion: String
    ): String {
        return """
            $SYSTEM_INSTRUCTION
            
            ### Current Context
            - Owner: ${context.ownerName}
            - Visible People: ${if (context.visiblePeople.isEmpty()) "None" else context.visiblePeople.joinToString(", ") { it.name }}
            - Primary Subject: ${context.primarySubject?.name ?: "None"}
            - Unknown Persons: ${context.unknownCount}
            - System Time: ${context.currentTime}
            - System Date: ${context.currentDate}
            - Battery Level: ${if (context.batteryPercent != -1) "${context.batteryPercent}%" else "Unknown"}
            - Security Alerts: ${context.recentAlerts.size}
            
            ### Conversation History
            $history
            
            ### User Question
            $userQuestion
            
            ### Response
        """.trimIndent()
    }
}
