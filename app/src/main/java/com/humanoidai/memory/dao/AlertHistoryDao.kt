package com.humanoidai.memory.dao

import androidx.room.*
import com.humanoidai.memory.entities.AlertHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertHistoryEntity)

    @Query("SELECT * FROM alert_history WHERE deliveredAt = 0 ORDER BY triggeredAt DESC")
    suspend fun getPendingAlerts(): List<AlertHistoryEntity>

    @Query("UPDATE alert_history SET deliveredAt = :timestamp WHERE alertId = :alertId")
    suspend fun markDelivered(alertId: String, timestamp: Long)

    @Query("UPDATE alert_history SET acknowledgedAt = :timestamp WHERE alertId = :alertId")
    suspend fun markAcknowledged(alertId: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM alert_history ORDER BY triggeredAt DESC")
    fun observeAllAlerts(): Flow<List<AlertHistoryEntity>>

    @Query("SELECT * FROM alert_history ORDER BY triggeredAt DESC LIMIT :limit")
    suspend fun getRecentAlerts(limit: Int = 50): List<AlertHistoryEntity>

    @Query("SELECT COUNT(*) FROM alert_history WHERE triggeredAt > :startOfDay")
    suspend fun getTodayAlertCount(startOfDay: Long): Int

    @Query("SELECT priority, COUNT(*) as count FROM alert_history GROUP BY priority")
    suspend fun getCountByPriority(): List<PriorityCount>

    @Query("DELETE FROM alert_history WHERE triggeredAt < :cutoffTime")
    suspend fun deleteOlderThan(cutoffTime: Long)

    @Query("DELETE FROM alert_history")
    suspend fun deleteAll()
}

data class PriorityCount(val priority: String, val count: Int)
