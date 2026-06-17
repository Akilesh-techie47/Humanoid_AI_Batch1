package com.humanoidai.ai

import com.humanoidai.context.CurrentContext

/**
 * Constructs structured prompts for the AI, injecting environmental context
 */
object PromptBuilder {

    private const val SYSTEM_INSTRUCTION = """
        You are Humanoid. You are not a chatbot. You are a personal AI companion.
        You continuously observe the environment and your personality is:
        - Helpful, Friendly, Professional, Calm, Respectful, Observant, and Proactive.
        - Only speak when necessary. Always be concise.
        - Never hallucinate (invent information).
        - Always consider the current environmental context provided below.
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
            - Visible People: ${if (context.visiblePeople.isEmpty()) "None" else context.visiblePeople.joinToString(", ")}
            - Primary Subject: ${context.primaryPerson ?: "None"}
            - Unknown Persons: ${context.unknownCount}
            - System Time: ${context.time}
            - System Date: ${context.date}
            - Battery Level: ${if (context.batteryLevel != -1) "${context.batteryLevel}%" else "Unknown"}
            - Security Alerts: ${context.currentAlerts}
            - Active Screen: ${context.currentScreen}
            
            ### Conversation History
            $history
            
            ### User Question
            $userQuestion
            
            ### Response
        """.trimIndent()
    }
}
