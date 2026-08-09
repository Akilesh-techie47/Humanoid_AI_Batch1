package com.humanoidai.behavior

import com.humanoidai.ui.layoutcustomization.domain.reasoning.Decision
import com.humanoidai.ui.layoutcustomization.domain.reasoning.DecisionConfidence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponseComposerTest {

    private val composer = ResponseComposer()

    @Test
    fun testMinimalCommunicationLevel() {
        val decision = Decision(
            id = "test_id",
            category = "UI",
            priority = 50,
            confidence = DecisionConfidence.HIGH,
            actions = emptyList(),
            evidence = emptyList(),
            explanation = "Testing minimal level"
        )
        
        val response = composer.composeFromDecision(decision, CommunicationLevel.MINIMAL, PersonalityProfile.PROFESSIONAL)
        assertEquals("Acting on test_id.", response.text)
    }

    @Test
    fun testDetailedCommunicationLevelWithPersonality() {
        val decision = Decision(
            id = "test_id",
            category = "UI",
            priority = 50,
            confidence = DecisionConfidence.HIGH,
            actions = emptyList(),
            evidence = listOf("Evidence 1"),
            explanation = "Testing detailed level"
        )
        
        val response = composer.composeFromDecision(decision, CommunicationLevel.DETAILED, PersonalityProfile.FRIENDLY)
        assertTrue(response.text.contains("Evidence 1"))
        assertTrue(response.spokenText.startsWith("Sure thing!"))
    }

    @Test
    fun testProgressUpdateComposition() {
        val response = composer.composeProgressUpdate("Task X", 0.5f, PersonalityProfile.CONCISE)
        assertEquals("Progress on Task X: 50%", response.text)
        assertEquals("Progress on Task X: 50%", response.spokenText)
        assertTrue(response.channels.contains(InteractionChannel.VOICE))
        
        val silentResponse = composer.composeProgressUpdate("Task X", 0.3f, PersonalityProfile.CONCISE)
        assertEquals("", silentResponse.spokenText)
        assertTrue(!silentResponse.channels.contains(InteractionChannel.VOICE))
    }
}
