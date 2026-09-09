package com.humanoidai.ml

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.humanoidai.memory.database.Aura360Database
import com.humanoidai.memory.entities.UserEntity
import com.humanoidai.memory.entities.UserClass
import com.humanoidai.memory.security.PrivacyVault
import kotlinx.coroutines.runBlocking
import java.util.*

// -----------------------------------------------------------------
// OwnerEnrollmentManager
// -----------------------------------------------------------------
// Specialized manager for the primary app owner.
// Handles multi-angle capture, liveness verification, and 
// first-launch enrollment status using Aura360Database.
// -----------------------------------------------------------------
class OwnerEnrollmentManager(context: Context) {

    companion object {
        private const val PREFS_NAME        = "humanoid_owner_biometrics"
        private const val KEY_VOICE_ENROLLED = "is_voice_enrolled"
        private const val KEY_LANGUAGE      = "preferred_language"
        private const val KEY_AI_NAME       = "ai_name"
        private const val KEY_MASTER_PASS   = "master_password"
        
        // UI Customization Keys
        private const val KEY_MAX_ROI       = "ui_max_roi"
        private const val KEY_ROI_STRUCTURE = "ui_roi_structure"
        private const val KEY_FOCUS_MODE    = "ui_focus_mode"
        private const val KEY_SHOW_STATS    = "ui_show_stats"
        private const val KEY_SHOW_LABELS   = "ui_show_labels"
        private const val KEY_GLOW_EFFECT   = "ui_glow_enabled"
        private const val KEY_LAYOUT_PRESET = "ui_layout_preset"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val vault = PrivacyVault(context)
    private val userDao = Aura360Database.getInstance(context).userDao()
    private val gson = Gson()

    /**
     * Check if the owner has already completed the enrollment wizard.
     */
    fun isOwnerEnrolled(): Boolean = runBlocking {
        userDao.getOwner() != null
    }

    /**
     * Check if the owner has registered their voice.
     */
    fun isVoiceEnrolled(): Boolean = runBlocking {
        val owner = userDao.getOwner()
        owner?.voiceProfileId?.isNotEmpty() == true
    }

    /**
     * Save the master owner profile with multiple viewpoints.
     */
    fun enrollOwner(name: String, embeddings: List<FloatArray>, accuracy: Float, voiceEnrolled: Boolean = false) {
        if (embeddings.isEmpty()) return

        // 1. We'll store a subset of representative viewpoints (max 15 for owner)
        val representative = if (embeddings.size > 15) {
            val step = embeddings.size / 15
            List(15) { embeddings[it * step] }
        } else embeddings

        val encryptedViewpoints = representative.map { vault.encryptEmbedding(it) }

        // 2. Primary embedding is still the average for legacy support and quick match
        val masterEmbedding = calculateMasterEmbedding(embeddings)
        val encryptedMaster = vault.encryptEmbedding(masterEmbedding)

        val user = UserEntity(
            userId = "OWNER_001", 
            name = name,
            userClass = UserClass.OWNER,
            label = "Owner",
            embeddingData = encryptedMaster,
            viewpointsJson = gson.toJson(encryptedViewpoints),
            enrolledAt = System.currentTimeMillis(),
            isCriticalContact = true,
            voiceProfileId = if (voiceEnrolled) "OWNER_VOICE" else ""
        )

        runBlocking {
            userDao.insertUser(user)
        }
    }

    /**
     * Mark voice as enrolled separately if needed.
     */
    fun markVoiceEnrolled(enrolled: Boolean) {
        runBlocking {
            val owner = userDao.getOwner()
            if (owner != null) {
                userDao.updateUser(owner.copy(voiceProfileId = if (enrolled) "OWNER_VOICE" else ""))
            }
        }
    }

    fun getOwnerName(): String = runBlocking {
        userDao.getOwner()?.name ?: "Owner"
    }

    fun getMasterEmbedding(): FloatArray? = runBlocking {
        val encrypted = userDao.getOwner()?.embeddingData ?: return@runBlocking null
        try {
            vault.decryptEmbedding(encrypted)
        } catch (e: Exception) {
            null
        }
    }

    fun getOwnerViewpoints(): List<FloatArray> = runBlocking {
        val user = userDao.getOwner() ?: return@runBlocking emptyList()
        val json = user.viewpointsJson ?: return@runBlocking emptyList()
        try {
            val encryptedList: List<String> = gson.fromJson(json, Array<String>::class.java).toList()
            encryptedList.mapNotNull { encrypted ->
                try {
                    vault.decryptEmbedding(encrypted)
                } catch (e: Exception) { null }
            }
        } catch (e: Exception) { emptyList() }
    }

    fun getLastUpdated(): Long = runBlocking {
        userDao.getOwner()?.enrolledAt ?: 0L
    }
    
    fun getAccuracyScore(): Float = 0.95f // Default high accuracy for owner

    fun getPreferredLanguage(): String = prefs.getString(KEY_LANGUAGE, "auto") ?: "auto"
    fun setPreferredLanguage(lang: String) {
        prefs.edit().putString(KEY_LANGUAGE, lang).apply()
    }

    fun getAiName(): String = prefs.getString(KEY_AI_NAME, "Aura 360") ?: "Aura 360"
    fun setAiName(name: String) {
        prefs.edit().putString(KEY_AI_NAME, name).apply()
    }

    fun getMasterPassword(): String? = prefs.getString(KEY_MASTER_PASS, null)
    fun setMasterPassword(password: String) {
        prefs.edit().putString(KEY_MASTER_PASS, password).apply()
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
