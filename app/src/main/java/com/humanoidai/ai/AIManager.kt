package com.humanoidai.ai

import android.content.Context
import com.humanoidai.context.ContextEngine
import com.humanoidai.context.CurrentContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import com.humanoidai.memory.ConversationMemory
import com.humanoidai.ai.ChatMessage

/**
 * The central brain that manages AI providers, context, and conversations.
 */
class AIManager(
    private val context: Context, 
    apiKey: String, 
    private val contextEngine: ContextEngine,
    private val conversationMemory: ConversationMemory,
    private val voiceEngine: com.humanoidai.voice.VoiceEngine
) {

    private val providers = mutableMapOf<String, AIProvider>()
    
    private val _activeProviderId = MutableStateFlow("MockAI-1.0")
    val activeProviderId: StateFlow<String> = _activeProviderId.asStateFlow()

    private val _aiState = MutableStateFlow<AIState>(AIState.Idle)
    val aiState: StateFlow<AIState> = _aiState.asStateFlow()

    init {
        // Register default providers
        val mock = MockProvider()
        val gemini = GeminiProvider(apiKey)
        
        providers[mock.id] = mock
        providers[gemini.id] = gemini
        
        // Default to Gemini (Phase 7)
        _activeProviderId.value = gemini.id
    }

    fun updateEnvironment(
        detections: List<com.humanoidai.vision.DetectedPerson>,
        alertCount: Int
    ) {
        contextEngine.updateFromVision(detections)
        contextEngine.updateAlerts(alertCount)
    }

    fun setProvider(providerId: String) {
        if (providers.containsKey(providerId)) {
            _activeProviderId.value = providerId
        }
    }

    suspend fun ask(
        userQuestion: String,
        ownerName: String,
        currentScreen: String
    ): AIResponse {
        _aiState.value = AIState.Loading
        
        // 1. Get Current Context from Engine
        contextEngine.updateScreen(currentScreen)
        contextEngine.updateOwner(ownerName)
        val currentContext = contextEngine.currentContext.value

        // 2. Build Prompt
        val history = conversationMemory.getHistorySnippet()
        val prompt = PromptBuilder.build(currentContext, history, userQuestion)

        // 3. Generate Response
        val provider = providers[_activeProviderId.value] ?: providers.values.first()
        val request = AIRequest(prompt)
        
        val response = provider.generate(request)

        // 4. Update History
        conversationMemory.addMessage(ChatMessage(text = userQuestion, isUser = true))
        if (response.error == null) {
            conversationMemory.addMessage(ChatMessage(text = response.text, isUser = false))
            _aiState.value = AIState.Success(response)
            
            // Proactively speak the AI's response
            voiceEngine.speak(response.text)
        } else {
            _aiState.value = AIState.Error(response.error)
        }

        return response
    }

    fun getMessages(): StateFlow<List<ChatMessage>> = conversationMemory.messages

    fun clearConversation() {
        conversationMemory.clear()
    }
}
