package com.humanoidai.ml

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

// -----------------------------------------------------------------
// OwnerEnrollmentManager
// -----------------------------------------------------------------
// Specialized manager for the primary app owner.
// Handles multi-angle capture, liveness verification, and 
// first-launch enrollment status.
// -----------------------------------------------------------------
class OwnerEnrollmentManager(context: Context) {

    companion object {
        private const val PREFS_NAME       = "humanoid_owner_biometrics"
        private const val KEY_ENROLLED     = "is_owner_enrolled"
        private const val KEY_OWNER_NAME   = "owner_name"
        private const val KEY_EMBEDDINGS   = "owner_embeddings"
        private const val KEY_LAST_UPDATED = "last_updated"
        private const val KEY_ACCURACY     = "accuracy_score"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    /**
     * Check if the owner has already completed the enrollment wizard.
     */
    fun isOwnerEnrolled(): Boolean = prefs.getBoolean(KEY_ENROLLED, false)

    /**
     * Save the master owner profile.
     * @param name Owner's name
     * @param embeddings A list of 20-30 high-quality embeddings from different angles.
     */
    fun enrollOwner(name: String, embeddings: List<FloatArray>, accuracy: Float) {
        if (embeddings.isEmpty()) return

        // Calculate Master Embedding (Cluster and Average)
        val masterEmbedding = calculateMasterEmbedding(embeddings)

        prefs.edit()
            .putBoolean(KEY_ENROLLED, true)
            .putString(KEY_OWNER_NAME, name)
            .putString(KEY_EMBEDDINGS, gson.toJson(masterEmbedding))
            .putLong(KEY_LAST_UPDATED, System.currentTimeMillis())
            .putFloat(KEY_ACCURACY, accuracy)
            .apply()
    }

    fun getOwnerName(): String = prefs.getString(KEY_OWNER_NAME, "Owner") ?: "Owner"

    fun getMasterEmbedding(): FloatArray? {
        val json = prefs.getString(KEY_EMBEDDINGS, null) ?: return null
        return try {
            val type = object : TypeToken<FloatArray>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            null
        }
    }

    fun getLastUpdated(): Long = prefs.getLong(KEY_LAST_UPDATED, 0L)
    fun getAccuracyScore(): Float = prefs.getFloat(KEY_ACCURACY, 0f)

    fun clearOwner() {
        prefs.edit().clear().apply()
    }

    /**
     * Advanced averaging: Remove outliers then average the remaining embeddings.
     */
    private fun calculateMasterEmbedding(embeddings: List<FloatArray>): FloatArray {
        if (embeddings.isEmpty()) return FloatArray(512)
        if (embeddings.size == 1) return embeddings[0]

        // 1. Calculate centroid
        val size = embeddings[0].size
        val centroid = FloatArray(size)
        for (i in 0 until size) {
            centroid[i] = embeddings.map { it[i] }.average().toFloat()
        }

        // 2. Filter outliers (simple version: keep top 80% closest to centroid)
        val distances = embeddings.map { e ->
            var dist = 0f
            for (i in 0 until size) {
                dist += (e[i] - centroid[i]) * (e[i] - centroid[i])
            }
            e to dist
        }.sortedBy { it.second }

        val subset = distances.take((embeddings.size * 0.8).toInt().coerceAtLeast(1)).map { it.first }

        // 3. Final average
        val result = FloatArray(size)
        for (i in 0 until size) {
            result[i] = subset.map { it[i] }.average().toFloat()
        }

        // 4. Normalize
        val norm = kotlin.math.sqrt(result.map { it * it }.sum())
        return if (norm > 0f) FloatArray(size) { result[it] / norm } else result
    }
}
