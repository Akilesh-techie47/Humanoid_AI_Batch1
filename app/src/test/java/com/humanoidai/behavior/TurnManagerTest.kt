package com.humanoidai.behavior

import com.humanoidai.voice.VoiceEngine
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class TurnManagerTest {

    private val voiceEngine = mock(VoiceEngine::class.java)
    private val turnManager = TurnManager(voiceEngine)

    @Test
    fun testInterruptionHandling() {
        // Start responding
        turnManager.onAiStartedResponding()
        assertEquals(InteractionTurnState.RESPONDING, turnManager.state.value)
        
        // User starts speaking -> should stop voice and go to listening
        turnManager.onUserStartedSpeaking()
        verify(voiceEngine).stop()
        assertEquals(InteractionTurnState.LISTENING, turnManager.state.value)
    }

    @Test
    fun testNormalTurnFlow() {
        assertEquals(InteractionTurnState.IDLE, turnManager.state.value)
        
        turnManager.onUserStartedSpeaking()
        assertEquals(InteractionTurnState.LISTENING, turnManager.state.value)
        
        turnManager.onUserFinishedSpeaking()
        assertEquals(InteractionTurnState.THINKING, turnManager.state.value)
        
        turnManager.onAiStartedResponding()
        assertEquals(InteractionTurnState.RESPONDING, turnManager.state.value)
        
        turnManager.onAiFinishedResponding()
        assertEquals(InteractionTurnState.IDLE, turnManager.state.value)
    }
}
