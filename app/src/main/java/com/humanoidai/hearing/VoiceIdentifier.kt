package com.humanoidai.hearing

import com.humanoidai.memory.entities.VoiceProfileEntity
import kotlin.math.sqrt

data class VoiceMatchResult(val userId: String?, val similarity: Float, val isTrusted: Boolean)

/**
 * Handles speaker identification via cosine similarity.
 */
class VoiceIdentifier {

    private val THRESHOLD = 0.85f

    fun match(liveEmbedding: FloatArray, profiles: List<VoiceProfileEntity>): VoiceMatchResult {
        var bestMatch: VoiceMatchResult = VoiceMatchResult(null, 0f, false)
        
        for (profile in profiles) {
            val stored = decryptEmbedding(profile.voiceEmbedding)
            val sim = cosineSimilarity(liveEmbedding, stored)
            if (sim > bestMatch.similarity) {
                bestMatch = VoiceMatchResult(profile.userId, sim, sim >= THRESHOLD)
            }
        }
        
        return bestMatch
    }

    private fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
        var dot = 0f
        var n1 = 0f
        var n2 = 0f
        for (i in v1.indices) {
            dot += v1[i] * v2[i]
            n1 += v1[i] * v1[i]
            n2 += v2[i] * v2[i]
        }
        return dot / (sqrt(n1) * sqrt(n2))
    }

    private fun decryptEmbedding(data: String): FloatArray {
        // This would call PrivacyVault, simplified for now
        return data.split(",").map { it.toFloat() }.toFloatArray()
    }
}
