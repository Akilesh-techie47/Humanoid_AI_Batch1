package com.humanoidai.ml

import android.content.Context
import android.content.SharedPreferences
import com.humanoidai.memory.security.PrivacyVault

// -----------------------------------------------------------------
// FaceEnrollmentManager
// -----------------------------------------------------------------
// Persists known face embeddings across app sessions using
// SharedPreferences + PrivacyVault (AES-256).
//
// Each enrolled person is stored as:
//   key   → "face_<name>"
//   value → Encrypted JSON string of averaged embedding
//
// On app start, FaceRecognitionManager loads all saved faces
// via loadAllInto(recognitionManager, ownerManager).
// -----------------------------------------------------------------
class FaceEnrollmentManager(context: Context) {

    companion object {
        private const val PREFS_NAME      = "humanoid_faces"
        private const val KEY_NAMES       = "enrolled_names"
        private const val FACE_PREFIX     = "face_"
        private const val LABEL_PREFIX    = "label_"
        private const val VIEWPOINT_COUNT = "vcount_"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val vault = PrivacyVault(context)

    // ------------------------------------------------------------
    // Enrollment
    // ------------------------------------------------------------

    /**
     * Save a person with multiple viewpoints.
     */
    fun enrollPerson(name: String, label: String, embeddings: List<FloatArray>) {
        if (embeddings.isEmpty()) return

        // We'll store up to 5 representative viewpoints (e.g. from a larger list)
        val representative = if (embeddings.size > 5) {
            // Take start, middle-ish, and end of the session to get different angles
            listOf(embeddings[0], embeddings[embeddings.size/4], embeddings[embeddings.size/2], embeddings[(3 * embeddings.size) / 4], embeddings.last())
        } else embeddings

        representative.forEachIndexed { index, emb ->
            val encrypted = vault.encryptEmbedding(emb)
            prefs.edit().putString("$FACE_PREFIX${name}_$index", encrypted).apply()
        }
        
        prefs.edit()
            .putInt("$VIEWPOINT_COUNT$name", representative.size)
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
        
        val count = prefs.getInt("$VIEWPOINT_COUNT$name", 0)
        val editor = prefs.edit()
        for (i in 0 until count) {
            editor.remove("${FACE_PREFIX}${name}_$i")
        }
        
        editor.putStringSet(KEY_NAMES, names)
            .remove("$VIEWPOINT_COUNT$name")
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

    fun getEmbeddings(name: String): List<FloatArray> {
        val count = prefs.getInt("$VIEWPOINT_COUNT$name", 0)
        val list = mutableListOf<FloatArray>()
        for (i in 0 until count) {
            val encrypted = prefs.getString("${FACE_PREFIX}${name}_$i", null)
            if (encrypted != null) {
                try {
                    list.add(vault.decryptEmbedding(encrypted))
                } catch (e: Exception) {}
            }
        }
        return list
    }

    /**
     * Load all saved faces into the FaceRecognitionManager.
     * Call this on app start. Also loads the Owner if present.
     */
    fun loadAllInto(recognitionManager: FaceRecognitionManager, ownerManager: OwnerEnrollmentManager? = null) {
        recognitionManager.clearAll()
        
        // 1. Load Owner first (Primary priority)
        ownerManager?.let { om ->
            if (om.isOwnerEnrolled()) {
                val name = om.getOwnerName()
                recognitionManager.setOwner(name)
                om.getMasterEmbedding()?.let { emb ->
                    recognitionManager.registerFace(name, emb)
                }
            }
        }

        // 2. Load all other enrolled persons (multiple viewpoints)
        getEnrolledNames().forEach { name ->
            val embeddings = getEmbeddings(name)
            if (embeddings.isNotEmpty()) {
                recognitionManager.registerFaces(name, embeddings)
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

}

// Simple UI model
data class EnrolledPerson(
    val name: String,
    val label: String
)
