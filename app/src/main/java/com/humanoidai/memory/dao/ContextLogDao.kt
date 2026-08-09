package com.humanoidai.memory.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.humanoidai.memory.entities.ContextLogEntity

@Dao
interface ContextLogDao {
    @Insert
    suspend fun insert(log: ContextLogEntity)

    @Query("SELECT * FROM context_logs ORDER BY timestamp DESC LIMIT 1")
    suspend fun getMostRecent(): ContextLogEntity?

    @Query("DELETE FROM context_logs WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}
