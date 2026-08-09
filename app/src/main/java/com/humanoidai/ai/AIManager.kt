package com.humanoidai.ai

import android.content.Context
import com.humanoidai.context.ContextEngine
import com.humanoidai.context.CurrentContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import com.humanoidai.memory.ConversationMemory
import com.humanoidai.ai.ChatMessage
import com.humanoidai.ml.OwnerEnrollmentManager

/**
 * The central brain that manages AI providers, context, and conversations.
 */
class AIManager(
    private val context: Context, 
    apiKey: String, 
    private val contextEngine: ContextEngine,
    private val conversationMemory: ConversationMemory,
    private val voiceEngine: com.humanoidai.voice.VoiceEngine,
    private val behaviorEngine: com.humanoidai.behavior.BehaviorEngine? = null
) {

    private val providers = mutableMapOf<String, AIProvider>()
    
    private val _activeProviderId = MutableStateFlow("MockAI-1.0")
    val activeProviderId: StateFlow<String> = _activeProviderId.asStateFlow()

    private val _aiState = MutableStateFlow<AIState>(AIState.Idle)
    val aiState: StateFlow<AIState> = _aiState.asStateFlow()

    // Context Cache for High-Speed Reasoning
    private var cachedOwnerName: String = ""
    private var cachedAiName: String = "Humanoid"
    private var cachedLanguage: String = "en"
    private val ownerEnrollmentManager by lazy { OwnerEnrollmentManager(context) }

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
        currentScreen: String,
        onResponseComplete: () -> Unit = {}
    ): AIResponse {
        val trust = com.humanoidai.security.TrustFramework.getInstance(context)
        if (!trust.sessionManager.isSessionActive()) {
            val response = AIResponse(
                text = "Session is not active or locked. Please re-authenticate.",
                provider = "SYSTEM",
                error = "SESSION_INACTIVE"
            )
            _aiState.value = AIState.Error(response.text)
            return response
        }

        _aiState.value = AIState.Loading
        
        // 1. Refresh Context (Optimized with Cache)
        if (cachedOwnerName.isEmpty() || ownerName != cachedOwnerName) {
            cachedOwnerName = ownerName
            cachedAiName = ownerEnrollmentManager.getAiName()
            cachedLanguage = ownerEnrollmentManager.getPreferredLanguage()
            contextEngine.updateOwner(ownerName)
            contextEngine.updateLanguage(cachedLanguage)
        }
        
        contextEngine.updateScreen(currentScreen)
        val currentContext = contextEngine.currentContext.value

        // 2. Build Prompt
        val history = conversationMemory.getHistorySnippet()
        val prompt = PromptBuilder.build(currentContext, history, userQuestion, cachedAiName)

        // 3. Generate Response (Flash 2.5 Path)
        val provider = providers[_activeProviderId.value] ?: providers.values.first()
        val request = AIRequest(prompt)
        
        val response = provider.generate(request)

        // 4. Update History
        conversationMemory.addMessage(ChatMessage(text = userQuestion, isUser = true))
        if (response.error == null) {
            conversationMemory.addMessage(ChatMessage(text = response.text, isUser = false))
            _aiState.value = AIState.Success(response)
            
            // Determine Tone based on context
            val tone = when {
                currentContext.recentAlerts.isNotEmpty() -> com.humanoidai.voice.SpeechTone.ALERT
                currentContext.batteryPercent in 1..15 -> com.humanoidai.voice.SpeechTone.CONCERNED
                else -> com.humanoidai.voice.SpeechTone.NEUTRAL
            }
            
            // Deliver via Behavior Engine if available (Phase 5D)
            if (behaviorEngine != null) {
                behaviorEngine.deliverResponse(
                    com.humanoidai.behavior.InteractionResponse(
                        text = response.text,
                        priority = if (tone != com.humanoidai.voice.SpeechTone.NEUTRAL) 
                            com.humanoidai.behavior.InteractionPriority.HIGH 
                            else com.humanoidai.behavior.InteractionPriority.NORMAL
                    )
                )
                onResponseComplete()
            } else {
                // Proactively speak the AI's response (Legacy)
                voiceEngine.speak(response.text, tone = tone) {
                    onResponseComplete()
                }
            }
        } else {
            _aiState.value = AIState.Error(response.error)
            onResponseComplete()
        }

        return response
    }

    fun getMessages(): StateFlow<List<ChatMessage>> = conversationMemory.messages

    fun clearConversation() {
        conversationMemory.clear()
    }
}
