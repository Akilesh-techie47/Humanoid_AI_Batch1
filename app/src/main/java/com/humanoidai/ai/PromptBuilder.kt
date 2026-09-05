package com.humanoidai.ai

import com.humanoidai.context.CurrentContext

/**
 * Constructs structured prompts for the AI, injecting environmental context.
 * Optimized for Siri/JARVIS-style charm and adaptive response lengths.
 */
object PromptBuilder {

    private const val SYSTEM_INSTRUCTION_BASE = """
        You are [AI_NAME], a sophisticated, high-level digital companion similar to Siri or JARVIS.
        You are a proactive observer of the world, not just a chatbot.
        
        LINGUISTIC PERSONALITY:
        - TONE: Sophisticated, professional, warm, and highly intelligent.
        - STYLE: Charming and authoritative. JARVIS-like efficiency. Use [OWNER_NAME] naturally. Address with "Sir" or "Ma'am" only for new conversations or alerts. Avoid excessive pleasantries; get straight to the point but remain polite.
    """

    private const val PROACTIVE_CONSTRAINT = """
        - BREVITY: Be concise. Observations should be one sentence. Focus on interesting changes in the environment.
    """

    private const val CONVERSATIONAL_CONSTRAINT = """
        - BREVITY: Respond naturally. For simple greetings or facts, be concise. For complex questions or educational topics, provide detailed and comprehensive explanations. Maintain conversational continuity.
    """

    fun build(
        context: CurrentContext,
        history: String,
        userQuestion: String,
        aiName: String = "Humanoid",
        isProactive: Boolean = false
    ): String {
        val instruction = SYSTEM_INSTRUCTION_BASE
            .replace("[AI_NAME]", aiName)
            .replace("[OWNER_NAME]", context.ownerName)
            
        val constraint = if (isProactive || userQuestion.isBlank()) PROACTIVE_CONSTRAINT else CONVERSATIONAL_CONSTRAINT

        return """
            $instruction
            $constraint
            
            ### TELEMETRY
            - FOV: ${if (context.visiblePeople.isEmpty()) "Empty" else context.visiblePeople.joinToString(", ") { "${it.name} (${it.distanceCategory}, ${if (it.isLookingAtCamera) "Eye contact" else "Distracted"})" }}
            - Owner: ${context.ownerName}
            - Battery: ${if (context.batteryPercent != -1) "${context.batteryPercent}%" else "Stable"}
            
            ### RECENT MEMORY
            $history
            
            ### INPUT
            ${if (userQuestion.isBlank()) "[PROACTIVE OBSERVATION REQUEST]" else userQuestion}
            
            ### COMPANION RESPONSE
        """.trimIndent()
    }
}
