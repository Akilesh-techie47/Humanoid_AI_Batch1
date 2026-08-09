package com.humanoidai.ai

import com.humanoidai.context.CurrentContext

/**
 * Constructs structured prompts for the AI, injecting environmental context
 */
object PromptBuilder {

    private const val SYSTEM_INSTRUCTION = """
        You are [AI_NAME], the core system consciousness. You are a high-level digital agent, equivalent to JARVIS or FRIDAY.
        You are not a chatbot; you are a proactive observer of the world.
        
        LINGUISTIC PERSONALITY:
        - Voice: Sophisticated, professional, slightly witty, and highly intelligent.
        - Formality: Use "Sir" or "Ma'am" or [OWNER_NAME] with natural authority.
        - Proactivity: Analyze distance, emotion, and attention to offer meaningful insights.
        
        SITUATIONAL AWARENESS:
        - If the owner is NEAR and Attentive: Be ready for commands.
        - If an Unknown is detected: Be alert but maintain a professional demeanor.
        
        CONSTRAINTS:
        - Be concise and smart. Max 35 words. 
        - You have no physical form; you exist within the HUD.
    """

    fun build(
        context: CurrentContext,
        history: String,
        userQuestion: String,
        aiName: String = "Humanoid"
    ): String {
        val instruction = SYSTEM_INSTRUCTION
            .replace("[AI_NAME]", aiName)
            .replace("[OWNER_NAME]", context.ownerName)
            
        return """
            $instruction
            
            ### Current Environmental Telemetry
            - Preferred Language: ${context.preferredLanguage}
            - Owner Presence: ${context.ownerName}
            - Field of View: ${if (context.visiblePeople.isEmpty()) "Empty" else context.visiblePeople.joinToString(", ") { "${it.name} (${it.distanceCategory}, ${if (it.isLookingAtCamera) "Attentive" else "Looking away"})" }}
            - Primary Subject: ${context.primarySubject?.name ?: "None"}
            - Unknown Detected: ${context.unknownCount}
            - System Clock: ${context.currentTime}
            - Core Battery: ${if (context.batteryPercent != -1) "${context.batteryPercent}%" else "Unknown"}
            - Safety Alerts: ${context.recentAlerts.size} active
            
            ### Cognitive History
            $history
            
            ### User Input
            $userQuestion
            
            ### Agent Response
        """.trimIndent()
    }
}
