package com.humanoidai.memory.dao

import androidx.room.*
import com.humanoidai.memory.entities.InteractionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InteractionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInteraction(interaction: InteractionEntity)

    @Query("SELECT * FROM interactions ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentInteractions(limit: Int = 50): List<InteractionEntity>

    @Query("SELECT * FROM interactions ORDER BY timestamp DESC")
    fun observeAllInteractions(): Flow<List<InteractionEntity>>

    @Query("SELECT * FROM interactions WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getInteractionsByUser(userId: String): List<InteractionEntity>

    @Query("SELECT userId, COUNT(*) as count FROM interactions WHERE userId IS NOT NULL GROUP BY userId ORDER BY count DESC")
    suspend fun getInteractionCountPerUser(): List<UserInteractionCount>

    @Query("SELECT COUNT(*) FROM interactions WHERE timestamp > :startOfDay")
    suspend fun getTodayInteractionCount(startOfDay: Long): Int

    @Query("DELETE FROM interactions WHERE timestamp < :cutoffTime")
    suspend fun deleteOlderThan(cutoffTime: Long)

    @Query("DELETE FROM interactions")
    suspend fun deleteAll()
}

data class UserInteractionCount(val userId: String?, val count: Int)
