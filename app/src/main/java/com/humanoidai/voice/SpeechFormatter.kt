package com.humanoidai.voice

import java.util.*

/**
 * Post-processes Gemini output into expressive SSML.
 * Handles sentence chunking and contextual tone mapping.
 */
class SpeechFormatter {

    fun format(rawText: String, tone: SpeechTone): String {
        // 1. Strip Markdown for TTS naturalness (Rule 26)
        var clean = rawText
            .replace(Regex("(?m)^#+\\s+"), "") // Strip headers
            .replace(Regex("(?m)^[-*+]\\s+"), "") // Strip bullets
            .replace(Regex("(?m)^\\d+\\.\\s+"), "") // Strip numbers
            .replace(Regex("[*#_~`]"), "") // Strip emphasis
            .replace(Regex("\\[(.*?)]\\((.*?)\\)"), "$1") // Strip links but keep text
        
        // 2. Expand abbreviations and numbers for natural flow
        clean = expandNaturally(clean)

        // 3. Wrap in SSML based on tone
        return wrapSSML(clean, tone)
    }

    /**
     * Splits long responses into natural clause-sized chunks (~8-14 words).
     */
    fun splitIntoChunks(text: String): List<String> {
        val sentences = text.split(Regex("(?<=[.!?])\\s+"))
        val chunks = mutableListOf<String>()
        
        for (sentence in sentences) {
            val words = sentence.split(" ")
            if (words.size > 15) {
                // Split further at commas or conjunctions if too long
                sentence.split(Regex("(?<=[,;])|(?=\\s(and|but|or|so)\\s)")).forEach { 
                    if (it.trim().isNotBlank()) chunks.add(it.trim()) 
                }
            } else {
                chunks.add(sentence.trim())
            }
        }
        return chunks
    }

    private fun wrapSSML(text: String, tone: SpeechTone): String {
        val (rate, pitch) = when (tone) {
            SpeechTone.URGENT -> "1.2" to "1.1"
            SpeechTone.ALERT -> "1.1" to "1.05"
            SpeechTone.CONCERNED -> "0.9" to "0.95"
            SpeechTone.PLAYFUL -> "1.05" to "1.15"
            else -> "0.95" to "1.0" // Default Calm JARVIS
        }

        // Note: Android system TTS has limited SSML support, but we prepare it
        // for higher-end engines (ElevenLabs/Cloud TTS) as per spec.
        return """
            <speak>
                <prosody rate="$rate" pitch="$pitch">
                    ${insertPauses(text)}
                </prosody>
            </speak>
        """.trimIndent()
    }

    private fun insertPauses(text: String): String {
        // Only insert pauses for punctuation that is not part of an abbreviation (e.g. A.I.)
        // We look for punctuation followed by a space or end of string.
        return text.replace(Regex("(?<!\\b[A-Z]),\\s?"), ", <break time=\"200ms\"/> ")
                   .replace(Regex("(?<!\\b[A-Z])\\.\\s?"), ". <break time=\"400ms\"/> ")
                   .replace("...", "... <break time=\"600ms\"/> ")
    }

    private fun expandNaturally(text: String): String {
        // Enhanced expansions for professional persona (Rule 28)
        return text.replace("AI", "A.I.")
                   .replace("STT", "S.T.T.")
                   .replace("TTS", "T.T.S.")
                   .replace("ARMSUN", "Arm-sun")
                   .replace("etc.", "et cetera")
                   .replace(Regex("(\\d+)%"), "$1 percent")
                   .replace(Regex("(\\d+)m "), "$1 meters ")
                   .replace(Regex("(\\d+)km"), "$1 kilometers")
                   .replace(Regex("(\\d+)kg"), "$1 kilograms")
    }
}
