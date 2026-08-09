package com.humanoidai.memory.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "interactions")
data class InteractionEntity(
    @PrimaryKey val interactionId: String,
    val userId: String?,
    val eventType: String,
    val timestamp: Long,
    val contextSnapshot: String, // Encrypted JSON
    val aiResponse: String,
    val wasOwnerPresent: Boolean,
    val recognitionConfidence: Float
)
