package com.humanoidai.ai

import android.content.Context
import com.humanoidai.context.ContextEngine
import com.humanoidai.context.CurrentContext
import com.humanoidai.memory.ConversationMemory
import com.humanoidai.ml.OwnerEnrollmentManager
import com.humanoidai.security.SessionManager
import com.humanoidai.security.TrustFramework
import com.humanoidai.voice.VoiceEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class AIManagerTest {

    @Mock
    lateinit var mockContext: Context
    @Mock
    lateinit var mockContextEngine: ContextEngine
    @Mock
    lateinit var mockConversationMemory: ConversationMemory
    @Mock
    lateinit var mockVoiceEngine: VoiceEngine
    @Mock
    lateinit var mockAIProvider: AIProvider
    @Mock
    lateinit var mockTrustFramework: TrustFramework
    @Mock
    lateinit var mockSessionManager: SessionManager
    @Mock
    lateinit var mockOwnerEnrollmentManager: OwnerEnrollmentManager

    private lateinit var aiManager: AIManager

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        whenever(mockAIProvider.id).thenReturn("MockProvider")
        whenever(mockContextEngine.currentContext).thenReturn(MutableStateFlow(CurrentContext()))
        whenever(mockTrustFramework.sessionManager).thenReturn(mockSessionManager)
        whenever(mockSessionManager.isSessionActive()).thenReturn(true)
        whenever(mockConversationMemory.messages).thenReturn(MutableStateFlow(emptyList()))
        whenever(mockConversationMemory.getHistorySnippet(any())).thenReturn("History context")
        whenever(mockOwnerEnrollmentManager.getAiName()).thenReturn("Humanoid")
        whenever(mockOwnerEnrollmentManager.getPreferredLanguage()).thenReturn("en")
        
        val providers = mapOf("MockProvider" to mockAIProvider)
        aiManager = AIManager(
            mockContext,
            "fake_key",
            mockContextEngine,
            mockConversationMemory,
            mockVoiceEngine,
            initialProviders = providers,
            trustFramework = mockTrustFramework,
            ownerEnrollmentManager = mockOwnerEnrollmentManager
        )
    }

    @Test
    fun `ask flow triggers streaming and vocalization`() = runTest {
        val question = "Hello AI"
        val chunks = flowOf("Hello", " there", " owner.")
        whenever(mockAIProvider.stream(any())).thenReturn(chunks)
        
        aiManager.ask(question, "Owner", "HomeScreen")
        
        // Verify provider was called to stream the response
        verify(mockAIProvider).stream(any())
        
        // Verify voice engine was called to speak the stream
        verify(mockVoiceEngine).speakStream(any(), any())
        
        // Verify interaction was added to conversation memory
        verify(mockConversationMemory, atLeastOnce()).addMessage(any())
    }
    
    @Test
    fun `ask returns error response when session is inactive`() = runTest {
        whenever(mockSessionManager.isSessionActive()).thenReturn(false)
        
        val response = aiManager.ask("Help", "Owner", "HomeScreen")
        
        assert(response.error == "SESSION_INACTIVE")
        verify(mockAIProvider, never()).stream(any())
    }
}
