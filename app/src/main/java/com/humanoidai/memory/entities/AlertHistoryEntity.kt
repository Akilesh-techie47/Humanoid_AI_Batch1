package com.humanoidai.memory.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alert_history")
data class AlertHistoryEntity(
    @PrimaryKey val alertId: String,
    val alertType: String,
    val priority: String,
    val contactName: String,
    val triggeredAt: Long,
    val deliveredAt: Long = 0L,
    val acknowledgedAt: Long = 0L,
    val metadataJson: String // Encrypted JSON
)
