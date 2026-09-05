package com.humanoidai.voice

import android.content.Context
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

class VoiceEngineTest {

    @Mock
    lateinit var mockContext: Context

    @Mock
    lateinit var mockOutputEngine: SpeechOutputEngine

    private lateinit var voiceEngine: VoiceEngine

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        voiceEngine = VoiceEngine(mockContext, mockOutputEngine)
    }

    @Test
    fun `speak splits long text into chunks and speaks them sequentially`() {
        // A sentence with more than 15 words and a conjunction will be split
        val longText = "This is a very long sentence, and it is designed to test the chunking logic of the voice engine to ensure it splits correctly."
        
        voiceEngine.speak(longText)

        // Capture the onComplete callback for the first chunk
        val callbackCaptor = argumentCaptor<() -> Unit>()
        
        // Verify first chunk is spoken
        verify(mockOutputEngine).speak(any(), any(), callbackCaptor.capture())
        
        // Simulate completion of first chunk
        callbackCaptor.firstValue.invoke()
        
        // Verify second chunk is spoken
        verify(mockOutputEngine, times(2)).speak(any(), any(), any())
    }

    @Test
    fun `stop cancels ongoing speech in output engine`() {
        voiceEngine.stop()
        verify(mockOutputEngine).stop()
    }
    
    @Test
    fun `shutdown releases resources in output engine`() {
        voiceEngine.shutdown()
        verify(mockOutputEngine).shutdown()
    }
}
