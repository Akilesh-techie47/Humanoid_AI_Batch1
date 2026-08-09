package com.humanoidai.voice

import java.util.*

/**
 * Post-processes Gemini output into expressive SSML.
 * Handles sentence chunking and contextual tone mapping.
 */
class SpeechFormatter {

    fun format(rawText: String, tone: SpeechTone): String {
        // 1. Strip Markdown
        var clean = rawText.replace(Regex("[*#_~`]"), "")
        
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
        return text.replace(",", ", <break time=\"200ms\"/>")
                   .replace(".", ". <break time=\"400ms\"/>")
                   .replace("...", "... <break time=\"600ms\"/>")
    }

    private fun expandNaturally(text: String): String {
        // Example expansions
        return text.replace("AI", "A.I.")
                   .replace("etc.", "et cetera")
                   .replace(Regex("(\\d+)%"), "$1 percent")
                   .replace(Regex("(\\d+)m"), "$1 meters")
    }
}
