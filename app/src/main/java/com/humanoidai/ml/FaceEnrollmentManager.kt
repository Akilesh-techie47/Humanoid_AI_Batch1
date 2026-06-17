package com.humanoidai.ml

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// -----------------------------------------------------------------
// FaceEnrollmentManager
// -----------------------------------------------------------------
// Persists known face embeddings across app sessions using
// SharedPreferences + Gson serialization.
//
// Each enrolled person is stored as:
//   key   → "face_<name>"
//   value → JSON array of FloatArray (multiple embeddings per person
//            for better accuracy — average of 5 captures)
//
// On app start, FaceRecognitionManager loads all saved faces
// via loadAllInto(recognitionManager).
// -----------------------------------------------------------------
class FaceEnrollmentManager(context: Context) {

    companion object {
        private const val PREFS_NAME      = "humanoid_faces"
        private const val KEY_NAMES       = "enrolled_names"
        private const val FACE_PREFIX     = "face_"
        private const val LABEL_PREFIX    = "label_"
        private const val MAX_EMBEDDINGS  = 5   // capture 5 frames, average them
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    // ------------------------------------------------------------
    // Enrollment
    // ------------------------------------------------------------

    /**
     * Save a person with their averaged embedding.
     * embeddings: list of captures (ideally 3-5 frames of same person)
     */
    fun enrollPerson(name: String, label: String, embeddings: List<FloatArray>) {
        if (embeddings.isEmpty()) return

        // Average all captured embeddings for robustness
        val averaged = averageEmbeddings(embeddings)

        // Save embedding
        val json = gson.toJson(averaged)
        prefs.edit()
            .putString("$FACE_PREFIX$name", json)
            .putString("$LABEL_PREFIX$name", label)
            .apply()

        // Update name registry
        val names = getEnrolledNames().toMutableSet()
        names.add(name)
        prefs.edit().putStringSet(KEY_NAMES, names).apply()
    }

    /**
     * Remove a person from the registry.
     */
    fun removePerson(name: String) {
        val names = getEnrolledNames().toMutableSet()
        names.remove(name)
        prefs.edit()
            .putStringSet(KEY_NAMES, names)
            .remove("$FACE_PREFIX$name")
            .remove("$LABEL_PREFIX$name")
            .apply()
    }

    /**
     * Clear all enrolled persons.
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }

    // ------------------------------------------------------------
    // Loading
    // ------------------------------------------------------------

    fun getEnrolledNames(): Set<String> =
        prefs.getStringSet(KEY_NAMES, emptySet()) ?: emptySet()

    fun getLabel(name: String): String =
        prefs.getString("$LABEL_PREFIX$name", "Unknown") ?: "Unknown"

    fun getEmbedding(name: String): FloatArray? {
        val json = prefs.getString("$FACE_PREFIX$name", null) ?: return null
        return try {
            val type = object : TypeToken<FloatArray>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Load all saved faces into the FaceRecognitionManager.
     * Call this on app start from MainActivity or EnvironmentScreen.
     */
    fun loadAllInto(recognitionManager: FaceRecognitionManager) {
        recognitionManager.clearAll()
        getEnrolledNames().forEach { name ->
            val embedding = getEmbedding(name)
            if (embedding != null) {
                recognitionManager.registerFace(name, embedding)
            }
        }
    }

    /**
     * Get all enrolled persons as EnrolledPerson list for UI display.
     */
    fun getAllPersons(): List<EnrolledPerson> {
        return getEnrolledNames().map { name ->
            EnrolledPerson(
                name  = name,
                label = getLabel(name)
            )
        }
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    private fun averageEmbeddings(embeddings: List<FloatArray>): FloatArray {
        if (embeddings.size == 1) return embeddings[0]
        val size   = embeddings[0].size
        val result = FloatArray(size)
        for (i in 0 until size) {
            result[i] = embeddings.map { it[i] }.average().toFloat()
        }
        // Re-normalize after averaging
        val norm = kotlin.math.sqrt(result.map { it * it }.sum())
        return if (norm > 0f) FloatArray(size) { result[it] / norm } else result
    }
}

// Simple UI model
data class EnrolledPerson(
    val name: String,
    val label: String
)
