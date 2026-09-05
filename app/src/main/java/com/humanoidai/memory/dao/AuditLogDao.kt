package com.humanoidai.memory.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.humanoidai.memory.entities.AuditLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {
    @Insert
    suspend fun insertLog(log: AuditLogEntity)

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 500")
    fun getAllLogs(): Flow<List<AuditLogEntity>>

    @Query("DELETE FROM audit_logs WHERE timestamp < :expiry")
    suspend fun purgeOldLogs(expiry: Long)
}
