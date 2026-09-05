package com.humanoidai.ml

import android.util.Log
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
        private const val TAG = "FaceRecognition"
        
        // Confidence Bands - Optimized for Agent-level speed
        private const val THRESHOLD_CONFIRMED = 0.78f // Lowered from 0.87f for better reliability in varying light
        private const val THRESHOLD_PROBABLE  = 0.60f 
    }

    private var ownerName: String? = null
    
    fun setOwner(name: String) {
        this.ownerName = name
    }

    // Registry: person name → list of their face embeddings (viewpoints)
    private val knownFaces = mutableMapOf<String, MutableList<FloatArray>>()

    // ------------------------------------------------------------
    // Registration
    // ------------------------------------------------------------

    /**
     * Register a known person with a single embedding or add to existing.
     */
    fun registerFace(name: String, embedding: FloatArray) {
        val list = knownFaces.getOrPut(name) { mutableListOf() }
        // For owner/primary users, we want more viewpoints (multi-angle robustness)
        if (list.size >= 50) list.removeAt(0) 
        list.add(embedding)
        Log.d(TAG, "Registered viewpoint for $name. Total viewpoints: ${list.size}")
    }

    /**
     * Register multiple viewpoints at once.
     */
    fun registerFaces(name: String, embeddings: List<FloatArray>) {
        val list = knownFaces.getOrPut(name) { mutableListOf() }
        list.addAll(embeddings)
        if (list.size > 50) {
            val kept = list.takeLast(50)
            list.clear()
            list.addAll(kept)
        }
        Log.d(TAG, "Registered ${embeddings.size} viewpoints for $name.")
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
     * 
     * Logic:
     * > 0.85: Confirmed match.
     * 0.65 - 0.85: Probable match (often requires secondary auth like voice).
     * < 0.65: Unknown.
     */
    fun findMatch(embedding: FloatArray): Pair<String, Float> {
        if (knownFaces.isEmpty()) return Pair("UNKNOWN", 0f)

        var bestName = "UNKNOWN"
        var bestSimilarity = 0f

        knownFaces.forEach { (name, embeddings) ->
            // Check against ALL stored viewpoints for this person
            embeddings.forEach { knownEmbedding ->
                // Optimization: Use simple dot product for normalized vectors (Phase 11)
                val similarity = dotProduct(embedding, knownEmbedding)
                if (similarity > bestSimilarity) {
                    bestSimilarity = similarity
                    bestName = name
                }
            }
            // Early exit for extremely high confidence matches
            if (bestSimilarity > 0.96f) return@forEach
        }

        return when {
            // Priority 1: High Similarity Confirmation
            bestSimilarity >= 0.92f -> Pair(bestName, bestSimilarity)
            
            // Priority 2: Owner-specific lenient lock (Sticky Recognition)
            // If we are even moderately sure it's the owner, we keep the lock to avoid "Unknown" flicker
            ownerName != null && bestName == ownerName && bestSimilarity >= 0.65f -> Pair(bestName, bestSimilarity)

            // Priority 3: Standard Confirmation
            bestSimilarity >= THRESHOLD_CONFIRMED -> Pair(bestName, bestSimilarity)
            
            // Priority 4: Probable match
            bestSimilarity >= THRESHOLD_PROBABLE  -> Pair("PROBABLE_$bestName", bestSimilarity)
            
            else -> Pair("UNKNOWN", bestSimilarity)
        }
    }

    // ------------------------------------------------------------
    // Similarity Calculation
    // ------------------------------------------------------------

    /**
     * Dot product of two L2-normalized vectors.
     * Equivalent to Cosine Similarity but significantly faster (Phase 11 Optimization).
     */
    private fun dotProduct(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return 0f
        var dot = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
        }
        return dot
    }
}
