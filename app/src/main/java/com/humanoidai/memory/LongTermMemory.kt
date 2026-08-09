package com.humanoidai.memory

import android.content.Context
import com.humanoidai.memory.entities.*
import com.humanoidai.memory.repository.AnalyticsSummary
import com.humanoidai.memory.repository.MemoryRepository
import kotlinx.coroutines.flow.Flow

/**
 * LongTermMemory — the single public API for all persistent memory in Humanoid AI.
 * Now backed by encrypted Room DB with SQLCipher and AES-256.
 */
class LongTermMemory private constructor(context: Context) {

    private val repo = MemoryRepository(context)

    companion object {
        @Volatile private var INSTANCE: LongTermMemory? = null

        fun getInstance(context: Context): LongTermMemory {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LongTermMemory(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // ── Owner & User Management ───────────────────────────────────────────────

    suspend fun enrollOwner(
        name: String,
        embedding: FloatArray,
        voiceProfileId: String = ""
    ): String = repo.enrollUser(
        name = name,
        userClass = UserClass.OWNER,
        embedding = embedding,
        isCriticalContact = false,
        voiceProfileId = voiceProfileId
    )

    suspend fun enrollUser(
        name: String,
        userClass: String,
        embedding: FloatArray,
        isCriticalContact: Boolean = false
    ): String = repo.enrollUser(name, userClass, embedding, isCriticalContact)

    suspend fun getOwner(): UserEntity? = repo.getOwner()
    suspend fun getAllUsers(): List<UserEntity> = repo.getAllUsers()
    suspend fun getUsersByClass(userClass: String) = repo.getUsersByClass(userClass)
    suspend fun getCriticalContacts() = repo.getCriticalContacts()
    fun observeUsers(): Flow<List<UserEntity>> = repo.observeAllUsers()
    suspend fun updateUserLastSeen(userId: String) = repo.updateLastSeen(userId)
    suspend fun deleteUser(userId: String) = repo.deleteUser(userId)

    suspend fun getAllFaceEmbeddings(): Map<String, FloatArray> = repo.getAllEmbeddings()

    // ── Interaction Logging ───────────────────────────────────────────────────

    suspend fun logInteraction(
        eventType: String,
        userId: String? = null,
        context: Map<String, Any> = emptyMap(),
        aiResponse: String = "",
        wasOwnerPresent: Boolean = false,
        confidence: Float = 0f
    ) = repo.logInteraction(eventType, userId, context, aiResponse, wasOwnerPresent, confidence)

    suspend fun getRecentInteractions(limit: Int = 50) = repo.getRecentInteractions(limit)
    fun observeInteractions(): Flow<List<InteractionEntity>> = repo.observeInteractions()
    suspend fun getInteractionsByUser(userId: String) = repo.getInteractionsByUser(userId)

    // ── Alert Management ──────────────────────────────────────────────────────

    suspend fun logAlert(
        alertType: String,
        priority: String,
        contactName: String = "",
        metadata: Map<String, Any> = emptyMap()
    ): String = repo.logAlert(alertType, priority, contactName, metadata)

    suspend fun getPendingAlerts(): List<AlertHistoryEntity> = repo.getPendingAlerts()
    suspend fun markAlertDelivered(alertId: String) = repo.markAlertDelivered(alertId)
    suspend fun markAlertAcknowledged(alertId: String) = repo.markAlertAcknowledged(alertId)
    fun observeAlerts(): Flow<List<AlertHistoryEntity>> = repo.observeAlerts()
    suspend fun getRecentAlerts(limit: Int = 50) = repo.getRecentAlerts(limit)

    // ── Analytics & Maintenance ───────────────────────────────────────────────

    suspend fun getAnalyticsSummary(): AnalyticsSummary = repo.getAnalyticsSummary()
    suspend fun applyRetentionPolicy(days: Int = 90) = repo.applyRetentionPolicy(days)
    suspend fun fullWipe() = repo.fullWipe()
}
