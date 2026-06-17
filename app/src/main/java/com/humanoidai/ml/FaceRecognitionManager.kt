package com.humanoidai.ml

import kotlin.math.sqrt

// -----------------------------------------------------------------
// FaceRecognitionManager
// -----------------------------------------------------------------
// Stores a registry of known persons (name → embedding).
// For each detected face embedding, finds the closest match
// using cosine similarity. If similarity is below threshold,
// the person is marked UNKNOWN.
//
// Usage:
//   1. Call registerFace(name, embedding) to add known persons
//   2. Call findMatch(embedding) to identify a detected face
// -----------------------------------------------------------------
class FaceRecognitionManager {

    companion object {
        // Cosine similarity threshold — tune this after testing:
        // Higher = stricter matching (fewer false positives)
        // Lower  = more lenient (more false positives)
        private const val SIMILARITY_THRESHOLD = 0.75f
    }

    // Registry: person name → their face embedding
    private val knownFaces = mutableMapOf<String, FloatArray>()

    // ------------------------------------------------------------
    // Registration
    // ------------------------------------------------------------

    /**
     * Register a known person.
     * Call this when user adds a person in RecognitionScreen (Phase 3).
     * For now called manually or from a saved file.
     */
    fun registerFace(name: String, embedding: FloatArray) {
        knownFaces[name] = embedding
    }

    /**
     * Remove a known person.
     */
    fun removeFace(name: String) {
        knownFaces.remove(name)
    }

    /**
     * Clear all registered faces.
     */
    fun clearAll() {
        knownFaces.clear()
    }

    fun getRegisteredNames(): List<String> = knownFaces.keys.toList()

    // ------------------------------------------------------------
    // Recognition
    // ------------------------------------------------------------

    /**
     * Finds the best matching known person for a given embedding.
     * Returns Pair(name, confidence).
     * If no match above threshold, returns Pair("UNKNOWN", 0f).
     */
    fun findMatch(embedding: FloatArray): Pair<String, Float> {
        if (knownFaces.isEmpty()) return Pair("UNKNOWN", 0f)

        var bestName = "UNKNOWN"
        var bestSimilarity = 0f

        knownFaces.forEach { (name, knownEmbedding) ->
            val similarity = cosineSimilarity(embedding, knownEmbedding)
            if (similarity > bestSimilarity) {
                bestSimilarity = similarity
                bestName = name
            }
        }

        return if (bestSimilarity >= SIMILARITY_THRESHOLD) {
            Pair(bestName, bestSimilarity)
        } else {
            Pair("UNKNOWN", bestSimilarity)
        }
    }

    // ------------------------------------------------------------
    // Cosine similarity
    // ------------------------------------------------------------

    /**
     * Cosine similarity between two normalized vectors.
     * Result: 0.0 (completely different) to 1.0 (identical).
     * Since embeddings are L2-normalized in FaceEmbeddingHelper,
     * this reduces to a simple dot product.
     */
    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return 0f
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot   += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom > 0f) dot / denom else 0f
    }
}
