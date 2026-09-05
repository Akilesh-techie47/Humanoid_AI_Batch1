package com.humanoidai.ml

import android.content.Context
import android.util.Log
import com.humanoidai.memory.database.HumanoidDatabase
import com.humanoidai.memory.entities.UserEntity
import com.humanoidai.memory.entities.UserClass
import com.humanoidai.memory.security.PrivacyVault
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking

// -----------------------------------------------------------------
// FaceEnrollmentManager
// -----------------------------------------------------------------
// Persists known face embeddings across app sessions using
// HumanoidDatabase (Room + SQLCipher).
// -----------------------------------------------------------------
class FaceEnrollmentManager(context: Context) {

    private val userDao = HumanoidDatabase.getInstance(context).userDao()
    private val vault = PrivacyVault(context)
    private val gson = Gson()

    // ------------------------------------------------------------
    // Enrollment
    // ------------------------------------------------------------

    /**
     * Save a person with multiple viewpoints.
     */
    fun enrollPerson(name: String, label: String, embeddings: List<FloatArray>) {
        if (embeddings.isEmpty()) return

        // We'll store up to 15 representative viewpoints (Phase 11: Multi-Angle Robustness)
        val representative = if (embeddings.size > 15) {
            val step = embeddings.size / 15
            List(15) { embeddings[it * step] }
        } else embeddings

        val encryptedViewpoints = representative.map { vault.encryptEmbedding(it) }
        val masterEmbedding = encryptedViewpoints[0] // Use first as primary for now

        val user = UserEntity(
            userId = name,
            name = name,
            userClass = UserClass.FRIEND, // Default to FRIEND for general enrollment
            label = label,
            embeddingData = masterEmbedding,
            viewpointsJson = gson.toJson(encryptedViewpoints),
            enrolledAt = System.currentTimeMillis()
        )

        runBlocking {
            userDao.insertUser(user)
        }
    }

    /**
     * Remove a person from the registry.
     */
    fun removePerson(name: String) {
        runBlocking {
            val user = userDao.getUserById(name)
            if (user != null) {
                userDao.deleteUser(user)
            }
        }
    }

    /**
     * Clear all enrolled persons.
     */
    fun clearAll() {
        runBlocking {
            userDao.deleteAllUsers()
        }
    }

    // ------------------------------------------------------------
    // Loading
    // ------------------------------------------------------------

    fun getEnrolledNames(): Set<String> = runBlocking {
        userDao.getAllUsers().filter { it.userClass != UserClass.OWNER }.map { it.name }.toSet()
    }

    fun getLabel(name: String): String = runBlocking {
        userDao.getUserById(name)?.label ?: "Unknown"
    }

    fun getEmbeddings(name: String): List<FloatArray> = runBlocking {
        val user = userDao.getUserById(name) ?: run {
            Log.e("FaceEnrollment", "GetEmbeddings failed: User $name not found in DB")
            return@runBlocking emptyList()
        }
        val json = user.viewpointsJson ?: run {
            android.util.Log.w("FaceEnrollment", "GetEmbeddings: No viewpoints JSON for $name")
            return@runBlocking emptyList()
        }
        
        try {
            val encryptedList: List<String> = gson.fromJson(json, Array<String>::class.java).toList()
            encryptedList.mapNotNull { encrypted ->
                try {
                    vault.decryptEmbedding(encrypted)
                } catch (e: Exception) {
                    android.util.Log.e("FaceEnrollment", "Decryption failed for $name: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FaceEnrollment", "JSON parsing failed for $name: ${e.message}")
            emptyList()
        }
    }

    fun loadAllInto(recognitionManager: FaceRecognitionManager, ownerManager: OwnerEnrollmentManager? = null) {
        recognitionManager.clearAll()
        android.util.Log.d("FaceEnrollment", "Loading known faces into recognition manager...")
        
        // 1. Load Owner first (Primary priority)
        ownerManager?.let { om ->
            if (om.isOwnerEnrolled()) {
                val name = om.getOwnerName()
                recognitionManager.setOwner(name)
                
                // Load all viewpoints for owner if available, otherwise just master
                val viewpoints = om.getOwnerViewpoints()
                if (viewpoints.isNotEmpty()) {
                    recognitionManager.registerFaces(name, viewpoints)
                    Log.d("FaceEnrollment", "Loaded Owner: $name (${viewpoints.size} viewpoints)")
                } else {
                    om.getMasterEmbedding()?.let { emb ->
                        recognitionManager.registerFace(name, emb)
                        Log.d("FaceEnrollment", "Loaded Owner: $name (master only)")
                    }
                }
            } else {
                android.util.Log.w("FaceEnrollment", "Owner not enrolled.")
            }
        }

        // 2. Load all other enrolled persons (multiple viewpoints)
        val names = getEnrolledNames()
        android.util.Log.d("FaceEnrollment", "Found ${names.size} enrolled persons.")
        
        names.forEach { name ->
            val embeddings = getEmbeddings(name)
            if (embeddings.isNotEmpty()) {
                recognitionManager.registerFaces(name, embeddings)
                android.util.Log.d("FaceEnrollment", "Loaded $name (${embeddings.size} viewpoints)")
            } else {
                android.util.Log.e("FaceEnrollment", "No embeddings found for $name!")
            }
        }
    }

    /**
     * Get all enrolled persons as EnrolledPerson list for UI display.
     */
    fun getAllPersons(): List<EnrolledPerson> = runBlocking {
        userDao.getAllUsers().filter { it.userClass != UserClass.OWNER }.map {
            EnrolledPerson(
                name  = it.name,
                label = it.label,
                isCritical = it.isCriticalContact
            )
        }
    }

    fun toggleCriticalStatus(name: String) = runBlocking {
        userDao.getUserById(name)?.let { user ->
            userDao.updateUser(user.copy(isCriticalContact = !user.isCriticalContact))
        }
    }

}

// Simple UI model
data class EnrolledPerson(
    val name: String,
    val label: String,
    val isCritical: Boolean = false
)
