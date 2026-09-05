package com.humanoidai.memory.repository

import android.content.Context
import com.google.gson.Gson
import com.humanoidai.memory.dao.PriorityCount
import com.humanoidai.memory.dao.UserInteractionCount
import com.humanoidai.memory.database.HumanoidDatabase
import com.humanoidai.memory.entities.*
import com.humanoidai.memory.security.PrivacyVault
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import java.util.concurrent.TimeUnit

class MemoryRepository(context: Context) {
    private val db = HumanoidDatabase.getInstance(context)
    private val vault = PrivacyVault(context)
    private val gson = Gson()

    suspend fun enrollUser(
        name: String,
        userClass: String,
        embedding: FloatArray,
        isCriticalContact: Boolean = false,
        voiceProfileId: String = ""
    ): String {
        val userId = UUID.randomUUID().toString()
        val encryptedEmbedding = vault.encryptEmbedding(embedding)
        db.userDao().insertUser(
            UserEntity(
                userId = userId,
                name = name,
                userClass = userClass,
                embeddingData = encryptedEmbedding,
                enrolledAt = System.currentTimeMillis(),
                isCriticalContact = isCriticalContact,
                voiceProfileId = voiceProfileId
            )
        )
        return userId
    }

    suspend fun getOwner(): UserEntity? = db.userDao().getOwner()
    suspend fun getAllUsers(): List<UserEntity> = db.userDao().getAllUsers()
    fun observeAllUsers(): Flow<List<UserEntity>> = db.userDao().observeAllUsers()
    suspend fun getUsersByClass(userClass: String) = db.userDao().getUsersByClass(userClass)
    suspend fun getCriticalContacts() = db.userDao().getCriticalContacts()
    suspend fun updateLastSeen(userId: String) = db.userDao().updateLastSeen(userId, System.currentTimeMillis())
    suspend fun deleteUser(userId: String) = db.userDao().getUserById(userId)?.let { db.userDao().deleteUser(it) }

    suspend fun getAllEmbeddings(): Map<String, FloatArray> {
        return db.userDao().getAllUsers().associate { user ->
            user.userId to vault.decryptEmbedding(user.embeddingData)
        }
    }

    suspend fun logInteraction(
        eventType: String,
        userId: String? = null,
        contextSnapshot: Map<String, Any> = emptyMap(),
        aiResponse: String = "",
        wasOwnerPresent: Boolean = false,
        confidence: Float = 0f
    ): String {
        val id = UUID.randomUUID().toString()
        db.interactionDao().insertInteraction(
            InteractionEntity(
                interactionId = id,
                userId = userId,
                eventType = eventType,
                timestamp = System.currentTimeMillis(),
                contextSnapshot = vault.encrypt(gson.toJson(contextSnapshot)),
                aiResponse = vault.encrypt(aiResponse),
                wasOwnerPresent = wasOwnerPresent,
                recognitionConfidence = confidence
            )
        )
        return id
    }

    suspend fun getRecentInteractions(limit: Int = 50): List<InteractionEntity> {
        return db.interactionDao().getRecentInteractions(limit).map { decryptInteraction(it) }
    }

    private fun decryptInteraction(entity: InteractionEntity): InteractionEntity {
        return try {
            entity.copy(
                aiResponse = vault.decrypt(entity.aiResponse),
                contextSnapshot = vault.decrypt(entity.contextSnapshot)
            )
        } catch (e: Exception) {
            android.util.Log.e("MemoryRepository", "Decryption failed for interaction ${entity.interactionId}: ${e.message}")
            // Return placeholder on failure to avoid UI garbage
            entity.copy(
                aiResponse = "[ENCRYPTED DATA]",
                contextSnapshot = "{}"
            )
        }
    }
    fun observeInteractions(): Flow<List<InteractionEntity>> {
        return db.interactionDao().observeAllInteractions().map { list ->
            list.map { decryptInteraction(it) }
        }
    }
    suspend fun getInteractionsByUser(userId: String) = db.interactionDao().getInteractionsByUser(userId)

    suspend fun logAlert(
        alertType: String,
        priority: String,
        contactName: String = "",
        metadata: Map<String, Any> = emptyMap()
    ): String {
        val id = UUID.randomUUID().toString()
        db.alertHistoryDao().insertAlert(
            AlertHistoryEntity(
                alertId = id,
                alertType = alertType,
                priority = priority,
                contactName = contactName,
                triggeredAt = System.currentTimeMillis(),
                metadataJson = vault.encrypt(gson.toJson(metadata))
            )
        )
        return id
    }

    suspend fun getPendingAlerts() = db.alertHistoryDao().getPendingAlerts()
    suspend fun markAlertDelivered(alertId: String) = db.alertHistoryDao().markDelivered(alertId, System.currentTimeMillis())
    suspend fun markAlertAcknowledged(alertId: String) = db.alertHistoryDao().markAcknowledged(alertId)
    fun observeAlerts(): Flow<List<AlertHistoryEntity>> = db.alertHistoryDao().observeAllAlerts()
    suspend fun getRecentAlerts(limit: Int = 50) = db.alertHistoryDao().getRecentAlerts(limit)

    suspend fun applyRetentionPolicy(days: Int = 90) {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
        db.interactionDao().deleteOlderThan(cutoff)
        db.alertHistoryDao().deleteOlderThan(cutoff)
    }

    suspend fun fullWipe() {
        db.userDao().deleteAllUsers()
        db.interactionDao().deleteAll()
        db.alertHistoryDao().deleteAll()
        vault.wipeAll()
        HumanoidDatabase.closeDatabase()
    }

    suspend fun getAnalyticsSummary(): AnalyticsSummary {
        val startOfDay = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        return AnalyticsSummary(
            totalDetectionsToday = db.interactionDao().getTodayInteractionCount(startOfDay),
            totalAlertsToday = db.alertHistoryDao().getTodayAlertCount(startOfDay),
            alertsByPriority = db.alertHistoryDao().getCountByPriority(),
            topUsers = db.interactionDao().getInteractionCountPerUser().take(5),
            registeredUserCount = db.userDao().getRegisteredUserCount()
        )
    }
}

data class AnalyticsSummary(
    val totalDetectionsToday: Int,
    val totalAlertsToday: Int,
    val alertsByPriority: List<PriorityCount>,
    val topUsers: List<UserInteractionCount>,
    val registeredUserCount: Int
)
