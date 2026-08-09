package com.humanoidai.memory.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voice_profiles")
data class VoiceProfileEntity(
    @PrimaryKey val userId: String,
    val voiceEmbedding: String, // Encrypted base64
    val lastUpdated: Long = System.currentTimeMillis()
)
