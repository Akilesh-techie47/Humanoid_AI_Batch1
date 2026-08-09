package com.humanoidai.ui.layoutcustomization.domain.memory

import androidx.compose.runtime.Immutable

/**
 * Fundamental types of memory in the AI's cognitive stack.
 */
enum class MemoryType {
    SESSION,      // Ephemeral session-wide metadata
    VISUAL,       // Recent observations from the camera
    CONVERSATION, // Recent dialogue and speech context
    TASK          // Active goals and progress
}

/**
 * Importance levels for memory retention and eviction.
 */
enum class MemoryImportance {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Lifecycle states of a memory entry.
 */
enum class MemoryStatus {
    CREATED,
    ACTIVE,
    REFERENCED,
    AGING,
    EXPIRED,
    REMOVED
}

/**
 * Immutable atomic unit of memory.
 */
@Immutable
data class MemoryEntry(
    val id: String,
    val type: MemoryType,
    val timestamp: Long = System.currentTimeMillis(),
    val confidence: Float = 1.0f,
    val source: String = "unknown",
    val importance: MemoryImportance = MemoryImportance.MEDIUM,
    val expirationTime: Long = -1L, // -1 for no fixed expiration
    val status: MemoryStatus = MemoryStatus.CREATED,
    val payload: Any? = null
)

/**
 * High-level state of the cognitive layers.
 */
@Immutable
data class MemoryState(
    val activeFocusId: String? = null,
    val focusStack: List<String> = emptyList(),
    val entries: Map<String, MemoryEntry> = emptyMap(),
    val timeline: List<String> = emptyList() // List of entry IDs in order
)
