package com.humanoidai.behavior

import com.humanoidai.ui.layoutcustomization.domain.reasoning.Decision

/**
 * Transforms internal states into user-facing multimodal responses.
 */
class ResponseComposer {

    fun composeFromDecision(
        decision: Decision,
        level: CommunicationLevel,
        personality: PersonalityProfile
    ): InteractionResponse {
        val text = when (level) {
            CommunicationLevel.MINIMAL -> "Acting on ${decision.id}."
            CommunicationLevel.BALANCED -> decision.explanation
            CommunicationLevel.DETAILED -> {
                "${decision.explanation} Evidence: ${decision.evidence.joinToString(", ")}. Confidence: ${decision.confidence}."
            }
        }

        val spokenText = applyPersonality(text, personality)

        return InteractionResponse(
            text = text,
            spokenText = spokenText,
            priority = if (decision.priority > 90) InteractionPriority.HIGH else InteractionPriority.NORMAL
        )
    }

    fun composeProgressUpdate(
        goalTitle: String,
        progress: Float,
        personality: PersonalityProfile
    ): InteractionResponse {
        val percentage = (progress * 100).toInt()
        val text = "Progress on $goalTitle: $percentage%"
        
        return InteractionResponse(
            text = text,
            spokenText = if (percentage % 25 == 0) text else "", // Only speak at milestones
            channels = if (percentage % 25 == 0) setOf(InteractionChannel.TEXT, InteractionChannel.VOICE) else setOf(InteractionChannel.TEXT)
        )
    }

    private fun applyPersonality(text: String, profile: PersonalityProfile): String {
        return when (profile) {
            PersonalityProfile.FRIENDLY -> "Sure thing! $text"
            PersonalityProfile.CONCISE -> text.take(100)
            PersonalityProfile.EDUCATIONAL -> "As part of our learning process, $text"
            PersonalityProfile.PROFESSIONAL -> "Understood. $text"
            PersonalityProfile.DEVELOPER -> "[DEV] $text"
        }
    }
}
