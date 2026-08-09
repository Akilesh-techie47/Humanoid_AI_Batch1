package com.humanoidai.context.proactive

import com.humanoidai.context.ContextEngine
import com.humanoidai.context.model.ContextEventType
import com.humanoidai.context.model.EmotionType
import com.humanoidai.voice.VoiceEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

/**
 * CEA v1.4 Step 5: Proactive Trigger Loop.
 * Periodically evaluates context to decide if the AI should speak unprompted.
 */
class ProactiveTriggerEngine(
    private val contextEngine: ContextEngine,
    private val voiceEngine: VoiceEngine,
    private val scope: CoroutineScope
) {

    private var lastEngagementTime = 0L
    private val ENGAGEMENT_COOLDOWN = 60_000L // 1 minute between proactive chats

    fun start() {
        scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(8000) // Evaluate every 8 seconds
                evaluateProactiveEngagement()
            }
        }
    }

    private fun evaluateProactiveEngagement() {
        val state = contextEngine.state.value
        val now = System.currentTimeMillis()

        if (now - lastEngagementTime < ENGAGEMENT_COOLDOWN) return

        // ── Rule-Based Engagement (Step 5 first pass) ────────────────────────
        
        // 1. Owner Joy Detection
        val isOwnerSmiling = state.ownerEmotion?.type == EmotionType.JOY && state.ownerEmotion.confidence > 0.8f
        if (isOwnerSmiling) {
            val smileEvents = state.recentEvents.filter { 
                it.type == ContextEventType.MOOD_SHIFT && it.timestamp > now - 15000 
            }
            if (smileEvents.size >= 2) { // Smiling for a sustained period
                engage("You seem in a good mood. It's good to see you happy.")
                return
            }
        }

        // 2. Sound-Vision Correlation (Security Focus)
        val audio = state.ambientAudio
        if (audio != null && audio.voiceEnergyLevel > 0.6f && state.presentPeople.isEmpty()) {
            engage("I hear voices, but I don't see anyone. Everything alright?")
            return
        }

        // 3. Attention-Based Engagement (The Agent "Eye Contact")
        val primary = state.primaryFocus
        if (primary != null && primary.isLookingAtCamera && primary.distanceCategory == "NEAR") {
            val attentionEvents = state.recentEvents.filter { 
                it.type == ContextEventType.FACE_RECOGNIZED && it.timestamp > now - 10000 
            }
            if (attentionEvents.isNotEmpty()) {
                engage("At your service, ${primary.name}. Is there something you need?")
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
        voiceEngine.speak(text)
    }
}
