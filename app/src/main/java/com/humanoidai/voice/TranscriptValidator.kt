package com.humanoidai.voice

import android.util.Log

/**
 * Validates and repairs speech-to-text transcripts.
 * Ensures the AI pipeline receives meaningful input while allowing natural short commands.
 */
object TranscriptValidator {
    
    private const val TAG = "TranscriptValidator"
    
    private val LEGITIMATE_SHORT_COMMANDS = setOf(
        "stop", "yes", "no", "hello", "back", "again", "who", "what", "go", "open", "help"
    )

    private val FILLER_WORDS = setOf("uh", "um", "ah", "er", "hm")

    /**
     * Determines if a transcript is meaningful enough for AI processing.
     */
    fun isValid(text: String): Boolean {
        val trimmed = text.trim().lowercase()
        
        if (trimmed.isEmpty()) return false
        
        // Filter filler words
        if (trimmed in FILLER_WORDS) return false
        
        // Allow legitimate short commands
        if (trimmed in LEGITIMATE_SHORT_COMMANDS) return true
        
        // Filter out obvious junk (single letters that aren't 'a' or 'i', or just punctuation)
        if (trimmed.length == 1 && trimmed != "a" && trimmed != "i") {
            return false
        }
        
        // Reject transcripts that are only punctuation
        if (trimmed.all { !it.isLetterOrDigit() }) {
            return false
        }

        return true
    }

    /**
     * Performs basic repairs on common speech recognition artifacts.
     */
    fun normalize(text: String): String {
        var normalized = text.trim()
        
        // Remove trailing punctuation if it's repeated or strange
        normalized = normalized.replace(Regex("[.!?]{2,}$"), ".")
        
        // Common STT misinterpretations (Example: "camra" -> "camera")
        // Note: We use Gemini for deep reasoning, but minor deterministic fixes help.
        val repairs = mapOf(
            "camra" to "camera",
            "humanoid ai" to "Aura 360°",
            "humanoid" to "Aura 360°",
            "aura 360" to "Aura 360°"
        )
        
        // One-pass replacement to avoid double-correction
        val pattern = repairs.keys.joinToString("|") { Regex.escape(it) }
        val regex = Regex("(?i)\\b($pattern)\\b")
        normalized = regex.replace(normalized) { matchResult ->
            repairs[matchResult.value.lowercase()] ?: matchResult.value
        }
        
        return normalized
    }
}
