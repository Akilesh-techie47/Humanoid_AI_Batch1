package com.humanoidai.context.proactive

import com.humanoidai.ai.AIManager
import com.humanoidai.context.ContextEngine
import com.humanoidai.context.model.ContextEventType
import com.humanoidai.context.model.EmotionType
import com.humanoidai.voice.VoiceEngine
import kotlinx.coroutines.*

/**
 * CEA v1.4 Step 5: Proactive Trigger Loop.
 * Optimized for high-speed engagement using Gemini.
 */
class ProactiveTriggerEngine(
    private val contextEngine: ContextEngine,
    private val voiceEngine: VoiceEngine,
    private val aiManager: AIManager,
    private val scope: CoroutineScope
) {

    private var lastEngagementTime = 0L
    private val ENGAGEMENT_COOLDOWN = 20_000L // Reduced to 20s for better responsiveness

    fun start() {
        scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(5000) // Evaluate every 5 seconds
                evaluateProactiveEngagement()
            }
        }
    }

    private suspend fun evaluateProactiveEngagement() {
        val state = contextEngine.state.value
        val now = System.currentTimeMillis()

        if (now - lastEngagementTime < ENGAGEMENT_COOLDOWN) return

        // 1. Attention-Based Engagement (Primary priority)
        val primary = state.primaryFocus
        if (primary != null && primary.isLookingAtCamera && primary.distanceCategory == "NEAR") {
            val thought = aiManager.thinkProactively()
            if (thought != null) {
                engage(thought)
                return
            }
        }

        // 2. Sound-Vision Correlation
        val audio = state.ambientAudio
        if (audio != null && audio.voiceEnergyLevel > 0.6f && state.presentPeople.isEmpty()) {
            engage("I hear voices, but my cameras show an empty room. Should I be concerned?")
            return
        }

        // 3. Mood-Based (Lighter touches)
        val isOwnerSmiling = state.ownerEmotion?.type == EmotionType.JOY && state.ownerEmotion.confidence > 0.8f
        if (isOwnerSmiling) {
            val thought = aiManager.thinkProactively()
            if (thought != null) {
                engage(thought)
                return
            }
        }
    }

    private fun engage(text: String) {
        lastEngagementTime = System.currentTimeMillis()
        contextEngine.recordEvent(
            ContextEventType.PROACTIVE_ENGAGEMENT,
            "AI initiated engagement: $text"
        )
        // deliver via voice immediately for responsiveness
        voiceEngine.speak(text)
    }
}
