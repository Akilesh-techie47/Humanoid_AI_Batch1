package com.humanoidai.ai

import android.content.Context
import android.util.Log
import com.humanoidai.communication.CommunicationIntelligenceEngine
import com.humanoidai.context.ContextEngine
import com.humanoidai.context.CurrentContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

import com.humanoidai.memory.ConversationMemory
import com.humanoidai.memory.LongTermMemory
import com.humanoidai.ml.OwnerEnrollmentManager
import com.humanoidai.security.TrustFramework
import com.humanoidai.voice.SpeechTone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The central brain that manages AI providers, context, and conversations.
 */
class AIManager(
    private val context: Context, 
    apiKey: String, 
    private val contextEngine: ContextEngine,
    private val conversationMemory: ConversationMemory,
    private val voiceEngine: com.humanoidai.voice.VoiceEngine,
    private val behaviorEngine: com.humanoidai.behavior.BehaviorEngine? = null,
    private val commIntelEngine: CommunicationIntelligenceEngine? = null,
    initialProviders: Map<String, AIProvider>? = null,
    private val trustFramework: TrustFramework = TrustFramework.getInstance(context),
    private val ownerEnrollmentManager: OwnerEnrollmentManager = OwnerEnrollmentManager(context)
) {

    private val providers = mutableMapOf<String, AIProvider>()
    
    private val _activeProviderId = MutableStateFlow("MockAI-1.0")

    private val _aiState = MutableStateFlow<AIState>(AIState.Idle)
    val aiState: StateFlow<AIState> = _aiState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Context Cache for High-Speed Reasoning
    private var cachedOwnerName: String = ""
    private var cachedAiName: String = "Humanoid"
    private var cachedLanguage: String = "en"

    var onSpeechStarted: (() -> Unit)? = null

    init {
        if (initialProviders != null) {
            providers.putAll(initialProviders)
            if (initialProviders.isNotEmpty()) {
                _activeProviderId.value = initialProviders.keys.first()
            }
        } else {
            // Register default providers
            val mock = MockProvider()
            val gemini = GeminiProvider(apiKey)
            
            providers[mock.id] = mock
            providers[gemini.id] = gemini
            
            // Default to Gemini (Phase 7)
            _activeProviderId.value = gemini.id
        }
    }

    private var currentRequestId = 0L

    suspend fun ask(
        userQuestion: String,
        ownerName: String,
        currentScreen: String,
        onResponseComplete: () -> Unit = {}
    ): AIResponse {
        if (!trustFramework.sessionManager.isSessionActive()) {
            val response = AIResponse(
                text = "Session is inactive. Please re-authenticate.",
                provider = "SYSTEM",
                error = "SESSION_INACTIVE"
            )
            _aiState.value = AIState.Error(response.text)
            return response
        }

        val requestId = ++currentRequestId
        val requestStartTime = System.currentTimeMillis()
        Log.i("AIManager", "[VOICE] GEMINI_REQUEST_STARTED id=$requestId prompt=\"$userQuestion\"")
        _aiState.value = AIState.Loading
        
        // Refresh Context
        refreshContext(ownerName, currentScreen)
        val currentContext = contextEngine.currentContext.value

        // 1. Memory Integration (Rule 4-8)
        conversationMemory.addMessage(ChatMessage(text = userQuestion, isUser = true))
        val history = conversationMemory.getHistorySnippet()

        // 2. Check for Local Intents first (Interaction 2.0 Optimization)
        val localIntent = LocalIntentClassifier.classify(userQuestion)
        if (localIntent != LocalIntentClassifier.LocalIntent.UNKNOWN) {
            Log.i("AIManager", "[LOCAL_INTENT_DETECTED] $localIntent")
            val localResponse = handleLocalIntent(localIntent)
            val response = AIResponse(text = localResponse, provider = "LOCAL_SYSTEM")
            handleAiResponse(response, ownerName, currentContext, onResponseComplete)
            return response
        }

        // 3. Generate Response (Streaming for Interaction 2.0)
        val provider = providers[_activeProviderId.value] ?: providers.values.first()
        val prompt = PromptBuilder.build(currentContext, history, userQuestion, cachedAiName, isProactive = false)
        val request = AIRequest(prompt)
        
        val fullTextBuilder = StringBuilder()
        val phraseChannel = Channel<String>(Channel.UNLIMITED)
        val phraseFlow = phraseChannel.receiveAsFlow()
        var firstChunkReceived = false

        scope.launch {
            try {
                val currentPhrase = StringBuilder()
                provider.stream(request).collect { chunk ->
                    if (requestId != currentRequestId) {
                        Log.w("AIManager", "Discarding stale stream chunk for request $requestId")
                        return@collect
                    }

                    if (!firstChunkReceived) {
                        firstChunkReceived = true
                        onSpeechStarted?.invoke()
                        Log.d("AIManager", "[AI_STREAM_START] first chunk for $requestId at ${System.currentTimeMillis() - requestStartTime}ms")
                    }
                    
                    fullTextBuilder.append(chunk)
                    currentPhrase.append(chunk)

                    // Rule 17-18: Sentence-based streaming
                    if (chunk.contains(".") || chunk.contains("!") || chunk.contains("?") || chunk.contains("\n")) {
                        val phrase = currentPhrase.toString().trim()
                        if (phrase.length > 3) {
                            phraseChannel.send(phrase)
                            currentPhrase.setLength(0)
                        }
                    }
                }
                // Emit final remaining content
                val finalPhrase = currentPhrase.toString().trim()
                if (finalPhrase.isNotEmpty()) phraseChannel.send(finalPhrase)
                
            } catch (e: Exception) {
                Log.e("AIManager", "Streaming error $requestId: ${e.message}")
            } finally {
                phraseChannel.close()
            }
        }

        // 4. Vocalize while streaming
        voiceEngine.speakStream(phraseFlow, tone = SpeechTone.NEUTRAL)

        val finalResponseText = fullTextBuilder.toString().ifBlank { "I'm sorry, I couldn't process that request." }
        val response = AIResponse(
            text = finalResponseText,
            provider = provider.id,
            generationTimeMs = System.currentTimeMillis() - requestStartTime
        )
        
        Log.i("AIManager", "[VOICE] GEMINI_RESPONSE_RECEIVED id=$requestId duration=${response.generationTimeMs}ms")

        // 5. Finalize History (No speech here, already done by stream)
        finalizeAiResponse(response, ownerName, currentContext, onResponseComplete)
        return response
    }

    private suspend fun finalizeAiResponse(
        response: AIResponse, 
        ownerName: String, 
        currentContext: CurrentContext,
        onResponseComplete: () -> Unit
    ) {
        val messageText = response.text
        conversationMemory.addMessage(ChatMessage(text = messageText, isUser = false))
        
        try {
            LongTermMemory.getInstance(context).logInteraction(
                eventType = "AI_RESPONSE",
                userId = ownerName,
                aiResponse = messageText,
                context = mapOf(
                    "battery" to currentContext.batteryPercent,
                    "noise" to currentContext.noiseLevel,
                    "peopleCount" to currentContext.visiblePeople.size
                ),
                wasOwnerPresent = true
            )
        } catch (e: Exception) {
            Log.e("AIManager", "Memory Log Error: ${e.message}")
        }

        _aiState.value = AIState.Success(response)
        onResponseComplete()
    }

    private suspend fun handleLocalIntent(intent: LocalIntentClassifier.LocalIntent): String {
        return when (intent) {
            LocalIntentClassifier.LocalIntent.WHAT_TIME -> {
                val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                "The current time is ${sdf.format(Date())}."
            }
            LocalIntentClassifier.LocalIntent.GO_HOME -> "Returning to the home screen."
            LocalIntentClassifier.LocalIntent.OPEN_SETTINGS -> "Opening your system settings."
            LocalIntentClassifier.LocalIntent.TURN_ON_CAMERA -> "Activating the visual sensors."
            LocalIntentClassifier.LocalIntent.WHAT_DID_I_MISS -> {
                commIntelEngine?.getWhatDidIMissSummary() ?: "I'm checking your recent communications now."
            }
            LocalIntentClassifier.LocalIntent.WHO_CALLED_ME -> "Checking your missed call log."
            LocalIntentClassifier.LocalIntent.WHO_ARE_YOU -> "I am $cachedAiName, your digital companion. I observe, listen, and assist you with context-aware intelligence."
            else -> "I'm not sure how to handle that local request."
        }
    }



    /**
     * CEA v1.4 Step 5: Proactive Thinking.
     * Generates a thought or greeting based on current context without user input.
     */
    suspend fun thinkProactively(): String? {
        val currentContext = contextEngine.currentContext.value
        val history = conversationMemory.getHistorySnippet(limit = 3)
        
        val prompt = PromptBuilder.build(
            context = currentContext,
            history = history,
            userQuestion = "",
            aiName = cachedAiName,
            isProactive = true
        )

        val provider = providers[_activeProviderId.value] ?: providers.values.first()
        val response = provider.generate(AIRequest(prompt))
        
        return if (response.error == null && response.text != "NO_THOUGHT") {
            response.text
        } else null
    }

    private fun refreshContext(ownerName: String, currentScreen: String) {
        if (cachedOwnerName.isEmpty() || (ownerName != cachedOwnerName)) {
            cachedOwnerName = ownerName
            cachedAiName = ownerEnrollmentManager.getAiName()
            cachedLanguage = ownerEnrollmentManager.getPreferredLanguage()
            contextEngine.updateOwner(ownerName)
            contextEngine.updateLanguage(cachedLanguage)
        }
        contextEngine.updateScreen(currentScreen)
    }

    private suspend fun handleAiResponse(
        response: AIResponse, 
        ownerName: String, 
        currentContext: com.humanoidai.context.CurrentContext,
        onResponseComplete: () -> Unit
    ) {
        val messageText = if (response.error?.contains("invalid authentication credentials") == true) {
            "Sir, there seems to be an issue with my neural link configuration. Please verify my A.I. credentials in the settings."
        } else {
            response.text
        }

        conversationMemory.addMessage(ChatMessage(text = messageText, isUser = false))
        
        // Persist to Long Term Memory
        try {
            LongTermMemory.getInstance(context).logInteraction(
                eventType = "AI_RESPONSE",
                userId = ownerName,
                aiResponse = messageText,
                wasOwnerPresent = true
            )
        } catch (e: Exception) {
            Log.e("AIManager", "Memory Log Error: ${e.message}")
        }

        _aiState.value = AIState.Success(response)
        
        val tone = when {
            currentContext.recentAlerts.isNotEmpty() -> com.humanoidai.voice.SpeechTone.ALERT
            currentContext.batteryPercent in 1..15 -> com.humanoidai.voice.SpeechTone.CONCERNED
            else -> com.humanoidai.voice.SpeechTone.NEUTRAL
        }
        
        if (behaviorEngine != null) {
            behaviorEngine.deliverResponse(
                com.humanoidai.behavior.InteractionResponse(
                    text = messageText,
                    priority = if (tone != com.humanoidai.voice.SpeechTone.NEUTRAL) 
                        com.humanoidai.behavior.InteractionPriority.HIGH 
                        else com.humanoidai.behavior.InteractionPriority.NORMAL
                )
            )
        } else {
            voiceEngine.speak(messageText, tone = tone)
        }
        onResponseComplete()
    }

    fun getMessages(): StateFlow<List<ChatMessage>> = conversationMemory.messages
}
