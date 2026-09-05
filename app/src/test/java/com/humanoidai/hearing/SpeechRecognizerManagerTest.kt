package com.humanoidai.hearing

import android.content.Context
import android.media.AudioManager
import android.os.Handler
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

class SpeechRecognizerManagerTest {

    @Mock
    lateinit var mockContext: Context
    @Mock
    lateinit var mockAudioManager: AudioManager
    @Mock
    lateinit var mockHandler: Handler

    private lateinit var manager: SpeechRecognizerManager

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        whenever(mockContext.getSystemService(Context.AUDIO_SERVICE)).thenReturn(mockAudioManager)
        whenever(mockContext.packageName).thenReturn("com.humanoidai")
        
        // We pass a mock handler to avoid Looper issues in unit tests
        manager = SpeechRecognizerManager(mockContext, mockHandler)
    }

    @Test
    fun `initial state is IDLE`() {
        assertEquals(MicState.IDLE, manager.state.value)
    }

    @Test
    fun `startListening transitions state to STARTING`() {
        // Trigger startListening which will post to the handler
        manager.startListening()
        
        // The startInternal method posts a runnable to mainHandler
        val captor = argumentCaptor<Runnable>()
        verify(mockHandler, atLeastOnce()).post(captor.capture())
        
        // Run the captured runnable to simulate execution on main thread
        captor.lastValue.run()
        
        assertEquals(MicState.STARTING, manager.state.value)
    }

    @Test
    fun `stopListening transitions state to STOPPING and then IDLE`() {
        // Simulate being in a starting state first
        manager.startListening()
        val captorStart = argumentCaptor<Runnable>()
        verify(mockHandler, atLeastOnce()).post(captorStart.capture())
        captorStart.lastValue.run()
        
        // Now request stop
        manager.stopListening()
        assertEquals(MicState.STOPPING, manager.state.value)
        
        // The stop logic posts to the handler to finalize IDLE state
        val captorStop = argumentCaptor<Runnable>()
        verify(mockHandler, atLeastOnce()).post(captorStop.capture())
        captorStop.lastValue.run()
        
        assertEquals(MicState.IDLE, manager.state.value)
    }
}
