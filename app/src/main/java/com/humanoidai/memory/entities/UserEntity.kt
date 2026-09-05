package com.humanoidai.memory.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val name: String,
    val userClass: String, // OWNER, FAMILY, FRIEND, UNKNOWN
    val label: String = "Unknown",
    val passwordHash: String? = null,
    val passwordSalt: String? = null,
    val embeddingData: String, // Encrypted AES-256 base64
    val viewpointsJson: String? = null, // JSON list of encrypted embeddings
    val enrolledAt: Long,
    val lastSeenAt: Long = 0L,
    val detectionCount: Int = 0,
    val isCriticalContact: Boolean = false,
    val voiceProfileId: String = ""
)

object UserClass {
    const val OWNER = "OWNER"
    const val FAMILY = "FAMILY"
    const val FRIEND = "FRIEND"
    const val UNKNOWN = "UNKNOWN"
}
