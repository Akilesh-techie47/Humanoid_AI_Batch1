package com.humanoidai.hearing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Responsible for detecting the wake word ("Humanoid" or "Hey Humanoid").
 * In a production app, this would use a local DSP/TF Lite model. 
 * For this phase, we use the SpeechRecognizer's results to detect the keyword.
 */
class WakeWordManager {
    private var customWakeWord: String = "humanoid"
    private val DEFAULT_WAKE_WORDS = listOf("hey", "assistant")

    private val _isWakeWordDetected = MutableStateFlow(false)
    val isWakeWordDetected: StateFlow<Boolean> = _isWakeWordDetected.asStateFlow()

    fun setCustomWakeWord(name: String) {
        customWakeWord = name.lowercase().trim()
    }

    /**
     * Checks if the recognized text contains any of our wake words.
     */
    fun checkText(text: String): Boolean {
        val normalizedText = text.lowercase().trim()
        val detected = normalizedText.contains(customWakeWord) || 
                       DEFAULT_WAKE_WORDS.any { normalizedText.contains(it) }

        if (detected) {
            _isWakeWordDetected.value = true
        }
        return detected
    }

    fun reset() {
        _isWakeWordDetected.value = false
    }
}
