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
import com.humanoidai.ui.customization.AppearanceSettings
import com.humanoidai.voice.SpeechTone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.asStateFlow
import com.humanoidai.voice.TranscriptValidator
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
    apiKeys: Map<String, String>,
    private val contextEngine: ContextEngine,
    private val conversationMemory: ConversationMemory,
    private val voiceEngine: com.humanoidai.voice.VoiceEngine,
    private val behaviorEngine: com.humanoidai.behavior.BehaviorEngine? = null,
    private val commIntelEngine: CommunicationIntelligenceEngine? = null,
    private val settings: StateFlow<AppearanceSettings>,
    initialProviders: Map<String, AIProvider>? = null,
    private val trustFramework: TrustFramework = TrustFramework.getInstance(context),
    private val ownerEnrollmentManager: OwnerEnrollmentManager = OwnerEnrollmentManager(context)
) {

    private val registry = AIProviderRegistry()
    private val router = AIRouter(registry)
    private val orchestrator = AIOrchestrator(router)
    
    private val _aiState = MutableStateFlow<AIState>(AIState.Idle)
    val aiState: StateFlow<AIState> = _aiState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Context Cache for High-Speed Reasoning
    private var cachedOwnerName: String = ""
    private var cachedAiName: String = "Aura 360°"
    private var cachedLanguage: String = "en"

    var onSpeechStarted: (() -> Unit)? = null

    init {
        if (initialProviders != null) {
            initialProviders.values.forEach { registry.register(it) }
        } else {
            // Register default providers
            registry.register(MockProvider())
            registry.register(OfflineAIProvider())
            
            val geminiKey = apiKeys["gemini"] ?: ""
            val groqKey = apiKeys["groq"] ?: ""
            val openrouterKey = apiKeys["openrouter"] ?: ""
            val cerebrasKey = apiKeys["cerebras"] ?: ""
            val mistralKey = apiKeys["mistral"] ?: ""
            val nvidiaKey = apiKeys["nvidia"] ?: ""

            registry.register(GeminiProvider(geminiKey))
            registry.register(GroqProvider(groqKey))
            registry.register(OpenRouterProvider(openrouterKey))
            registry.register(CerebrasProvider(cerebrasKey))
            registry.register(MistralProvider(mistralKey))
            registry.register(NvidiaProvider(nvidiaKey))
            registry.register(OllamaProvider())
        }
        
        Log.i("AIManager", "[AI_ORCHESTRATOR] Registered providers: ${registry.getAll().joinToString { it.id }}")

        // Initialize active providers
        scope.launch {
            registry.getAll().forEach { it.initialize() }
        }
    }

    fun getProviderHealth(): Map<String, AIRouter.ProviderStatus> {
        return registry.getAll().associate { provider ->
            provider.id to (router.getProviderStatus(provider.id))
        }
    }

    suspend fun testProvider(providerId: String): Boolean {
        val provider = registry.get(providerId) ?: return false
        val testRequest = AIRequest(
            prompt = "Say 'LINK_STABLE' in one word.",
            requestId = -1L,
            type = AIRequestType.QUICK_COMMAND
        )
        return try {
            val response = provider.generate(testRequest)
            val success = response.error == null && response.text.contains("LINK_STABLE", ignoreCase = true)
            if (!success) {
                router.reportFailure(providerId, response.errorCategory ?: AIError.UNKNOWN_ERROR, response.metadata)
            }
            success
        } catch (e: Exception) {
            router.reportFailure(providerId, AIError.NETWORK_ERROR)
            false
        }
    }

    private var currentRequestId = 0L

    suspend fun ask(
        userQuestion: String,
        ownerName: String,
        currentScreen: String,
        onResponseComplete: () -> Unit = {}
    ): AIResponse {
        val requestId = ++currentRequestId
        val requestStartTime = System.currentTimeMillis()
        Log.i("AIManager", "[VOICE_PIPELINE] AI_REQUEST id=$requestId prompt=\"$userQuestion\"")

        if (!trustFramework.sessionManager.isSessionActive()) {
            Log.w("AIManager", "[VOICE_PIPELINE] ERROR: SESSION_INACTIVE id=$requestId")
            val response = AIResponse(
                text = "Session is inactive. Please re-authenticate.",
                provider = "SYSTEM",
                error = "SESSION_INACTIVE"
            )
            _aiState.value = AIState.Error(response.text)
            return response
        }

        val normalizedQuestion = TranscriptValidator.normalize(userQuestion)
        
        if (!TranscriptValidator.isValid(normalizedQuestion)) {
            Log.w("AIManager", "[VOICE_PIPELINE] ERROR: INVALID_INPUT id=$requestId prompt=\"$userQuestion\"")
            val response = AIResponse(
                text = "I didn't catch that. Could you repeat it?",
                provider = "SYSTEM",
                error = "INVALID_TRANSCRIPT"
            )
            _aiState.value = AIState.Error(response.text)
            return response
        }

        _aiState.value = AIState.Loading
        
        // Refresh Context
        refreshContext(ownerName, currentScreen)
        val currentContext = contextEngine.currentContext.value
        Log.d("AIManager", "[VOICE_PIPELINE] CONTEXT_ATTACHED id=$requestId context=\"${currentContext.ownerName} @ $currentScreen\"")

        // 1. Memory Integration (Rule 4-8)
        conversationMemory.addMessage(ChatMessage(text = normalizedQuestion, isUser = true))
        
        // Intelligent Context Management: Get a subset of recent history (e.g. last 6 turns)
        val historyList = conversationMemory.messages.value.takeLast(7).dropLast(1) // Exclude current question

        // 2. Check for Local Intents first (Interaction 2.0 Optimization)
        val localIntent = LocalIntentClassifier.classify(normalizedQuestion)
        if (localIntent != LocalIntentClassifier.LocalIntent.UNKNOWN) {
            Log.i("AIManager", "[VOICE_PIPELINE] LOCAL_INTENT_DETECTED id=$requestId intent=$localIntent")
            val localResponse = handleLocalIntent(localIntent)
            val response = AIResponse(text = localResponse, provider = "LOCAL_SYSTEM")
            handleAiResponse(response, ownerName, currentContext, onResponseComplete)
            return response
        }

        // 3. Generate Response (Streaming for Interaction 2.0)
        val hasVision = currentContext.visiblePeople.isNotEmpty()
        val prompt = PromptBuilder.build(currentContext, "", normalizedQuestion, cachedAiName, isProactive = false)
        val request = AIRequest(
            prompt = prompt,
            requestId = requestId,
            history = historyList,
            requiresVision = hasVision,
            type = if (hasVision) AIRequestType.VISION else AIRequestType.GENERAL_CONVERSATION
        )
        
        val fullTextBuilder = StringBuilder()
        val phraseChannel = Channel<String>(Channel.UNLIMITED)
        val phraseFlow = phraseChannel.receiveAsFlow()
        var firstChunkReceived = false
        var streamError: String? = null

        scope.launch {
            try {
                val currentPhrase = StringBuilder()
                orchestrator.stream(request, settings.value).collect { chunk ->
                    if (requestId != currentRequestId) {
                        Log.w("AIManager", "Discarding stale stream chunk for request $requestId")
                        return@collect
                    }

                    if (!firstChunkReceived) {
                        firstChunkReceived = true
                        onSpeechStarted?.invoke()
                        // Initial AI message placeholder
                        withContext(Dispatchers.Main) {
                            conversationMemory.addMessage(ChatMessage(text = "", isUser = false))
                        }
                    }
                    
                    fullTextBuilder.append(chunk)
                    currentPhrase.append(chunk)
                    
                    // Update UI history in real-time
                    withContext(Dispatchers.Main) {
                        conversationMemory.updateLastAiMessage(fullTextBuilder.toString())
                    }

                    // Sentence-based streaming for TTS
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
                streamError = e.localizedMessage
                Log.e("AIManager", "[VOICE_PIPELINE] ERROR: AI_STREAM_ERROR id=$requestId message=${e.message}")
                withContext(Dispatchers.Main) {
                    val errorMsg = "I encountered an error: ${e.localizedMessage ?: "Connection failure"}"
                    if (firstChunkReceived) {
                        conversationMemory.updateLastAiMessage(fullTextBuilder.toString() + "\n[ERROR: $errorMsg]")
                    } else {
                        conversationMemory.addMessage(ChatMessage(text = errorMsg, isUser = false))
                    }
                }
            } finally {
                phraseChannel.close()
            }
        }

        // 4. Vocalize while streaming
        voiceEngine.speakStream(phraseFlow, tone = SpeechTone.NEUTRAL)

        val finalText = fullTextBuilder.toString().trim()
        val finalResponseText = when {
            finalText.isNotEmpty() -> finalText
            streamError != null -> "I'm having trouble connecting to my neural network. Error: $streamError"
            else -> "I heard you, but I couldn't formulate a response. Please try again."
        }
        
        val response = AIResponse(
            text = finalResponseText,
            provider = "ROUTED", 
            requestId = requestId,
            error = streamError,
            generationTimeMs = System.currentTimeMillis() - requestStartTime
        )
        
        Log.i("AIManager", "[VOICE_PIPELINE] AI_RESPONSE_COMPLETE id=$requestId duration=${response.generationTimeMs}ms")

        // 5. Finalize History (Memory logging, etc)
        finalizeAiResponse(response, ownerName, currentContext, onResponseComplete)
        return response
    }

    private suspend fun finalizeAiResponse(
        response: AIResponse, 
        ownerName: String, 
        currentContext: CurrentContext,
        onResponseComplete: () -> Unit
    ) {
        // Ensure final text is set correctly
        withContext(Dispatchers.Main) {
            conversationMemory.updateLastAiMessage(response.text)
        }
        
        try {
            LongTermMemory.getInstance(context).logInteraction(
                eventType = "AI_RESPONSE",
                userId = ownerName,
                aiResponse = response.text,
                context = mapOf(
                    "battery" to currentContext.batteryPercent,
                    "noise" to currentContext.noiseLevel,
                    "provider" to response.provider
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

        val response = orchestrator.generate(AIRequest(prompt), settings.value)
        
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
