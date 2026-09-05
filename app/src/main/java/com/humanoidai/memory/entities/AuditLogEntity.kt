package com.humanoidai.memory.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val logId: Long = 0,
    val timestamp: Long,
    val category: String,
    val action: String,
    val status: String,
    val details: String
)
