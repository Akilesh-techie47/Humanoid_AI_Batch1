package com.humanoidai.memory.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "context_logs")
data class ContextLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val eventType: String? = null,
    val environmentData: String // Encrypted snapshot of lux, noise, etc.
)
