package com.humanoidai.ml

import android.content.Context
import android.content.SharedPreferences
import com.humanoidai.memory.security.PrivacyVault
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
        private const val PREFS_NAME        = "humanoid_owner_biometrics"
        private const val KEY_ENROLLED      = "is_owner_enrolled"
        private const val KEY_VOICE_ENROLLED = "is_voice_enrolled"
        private const val KEY_OWNER_NAME    = "owner_name"
        private const val KEY_EMBEDDINGS    = "owner_embeddings"
        private const val KEY_LAST_UPDATED  = "last_updated"
        private const val KEY_ACCURACY      = "accuracy_score"
        private const val KEY_LANGUAGE      = "preferred_language"
        private const val KEY_AI_NAME       = "ai_name"
        
        // UI Customization Keys
        private const val KEY_MAX_ROI       = "ui_max_roi"
        private const val KEY_ROI_STRUCTURE = "ui_roi_structure" // "classic", "minimal", "expanded"
        private const val KEY_FOCUS_MODE    = "ui_focus_mode" // "owner", "primary", "all"
        private const val KEY_SHOW_STATS    = "ui_show_stats"
        private const val KEY_SHOW_LABELS   = "ui_show_labels"
        private const val KEY_GLOW_EFFECT   = "ui_glow_enabled"
        private const val KEY_LAYOUT_PRESET = "ui_layout_preset"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val vault = PrivacyVault(context)

    /**
     * Check if the owner has already completed the enrollment wizard.
     */
    fun isOwnerEnrolled(): Boolean = prefs.getBoolean(KEY_ENROLLED, false)

    /**
     * Check if the owner has registered their voice.
     */
    fun isVoiceEnrolled(): Boolean = prefs.getBoolean(KEY_VOICE_ENROLLED, false)

    /**
     * Save the master owner profile.
     * @param name Owner's name
     * @param embeddings A list of 20-30 high-quality embeddings from different angles.
     */
    fun enrollOwner(name: String, embeddings: List<FloatArray>, accuracy: Float, voiceEnrolled: Boolean = false) {
        if (embeddings.isEmpty()) return

        // Calculate Master Embedding (Cluster and Average)
        val masterEmbedding = calculateMasterEmbedding(embeddings)

        // Encrypt embedding before saving
        val encrypted = vault.encryptEmbedding(masterEmbedding)

        prefs.edit()
            .putBoolean(KEY_ENROLLED, true)
            .putBoolean(KEY_VOICE_ENROLLED, voiceEnrolled)
            .putString(KEY_OWNER_NAME, name)
            .putString(KEY_EMBEDDINGS, encrypted)
            .putLong(KEY_LAST_UPDATED, System.currentTimeMillis())
            .putFloat(KEY_ACCURACY, accuracy)
            .apply()
    }

    /**
     * Mark voice as enrolled separately if needed.
     */
    fun markVoiceEnrolled(enrolled: Boolean) {
        prefs.edit().putBoolean(KEY_VOICE_ENROLLED, enrolled).apply()
    }

    fun getOwnerName(): String = prefs.getString(KEY_OWNER_NAME, "Owner") ?: "Owner"

    fun getMasterEmbedding(): FloatArray? {
        val encrypted = prefs.getString(KEY_EMBEDDINGS, null) ?: return null
        return try {
            vault.decryptEmbedding(encrypted)
        } catch (e: Exception) {
            null
        }
    }

    fun getLastUpdated(): Long = prefs.getLong(KEY_LAST_UPDATED, 0L)
    fun getAccuracyScore(): Float = prefs.getFloat(KEY_ACCURACY, 0f)

    fun getPreferredLanguage(): String = prefs.getString(KEY_LANGUAGE, "auto") ?: "auto"
    fun setPreferredLanguage(lang: String) {
        prefs.edit().putString(KEY_LANGUAGE, lang).apply()
    }

    fun getAiName(): String = prefs.getString(KEY_AI_NAME, "Humanoid") ?: "Humanoid"
    fun setAiName(name: String) {
        prefs.edit().putString(KEY_AI_NAME, name).apply()
    }

    // UI Customization Getters/Setters
    fun getMaxRoi(): Int = prefs.getInt(KEY_MAX_ROI, 3)
    fun setMaxRoi(count: Int) = prefs.edit().putInt(KEY_MAX_ROI, count).apply()

    fun getRoiStructure(): String = prefs.getString(KEY_ROI_STRUCTURE, "classic") ?: "classic"
    fun setRoiStructure(mode: String) = prefs.edit().putString(KEY_ROI_STRUCTURE, mode).apply()

    fun getFocusMode(): String = prefs.getString(KEY_FOCUS_MODE, "owner") ?: "owner"
    fun setFocusMode(mode: String) = prefs.edit().putString(KEY_FOCUS_MODE, mode).apply()

    fun isShowStatsEnabled(): Boolean = prefs.getBoolean(KEY_SHOW_STATS, true)
    fun setShowStatsEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_SHOW_STATS, enabled).apply()

    fun isShowLabelsEnabled(): Boolean = prefs.getBoolean(KEY_SHOW_LABELS, true)
    fun setShowLabelsEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_SHOW_LABELS, enabled).apply()

    fun isGlowEnabled(): Boolean = prefs.getBoolean(KEY_GLOW_EFFECT, true)
    fun setGlowEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_GLOW_EFFECT, enabled).apply()

    fun getLayoutPreset(): String = prefs.getString(KEY_LAYOUT_PRESET, "classic") ?: "classic"
    fun setLayoutPreset(preset: String) = prefs.edit().putString(KEY_LAYOUT_PRESET, preset).apply()

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
