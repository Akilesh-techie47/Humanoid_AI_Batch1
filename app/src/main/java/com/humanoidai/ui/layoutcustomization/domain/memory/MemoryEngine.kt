package com.humanoidai.ui.layoutcustomization.domain.memory

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The core engine for AI working memory.
 * Part of Phase 4A.
 */
class MemoryEngine {

    companion object {
        private const val TAG = "MemoryEngine"
        private const val MAX_ENTRIES = 50
        private const val DEFAULT_EXPIRATION_MS = 30000L // 30 seconds
    }

    private val _state = MutableStateFlow(MemoryState())
    val state: StateFlow<MemoryState> = _state.asStateFlow()

    /**
     * Records a new event or observation into memory.
     */
    fun record(entry: MemoryEntry) {
        _state.update { current ->
            val updatedEntries = current.entries.toMutableMap()
            
            // Apply lifecycle management: Auto-expire if not specified
            val finalEntry = if (entry.expirationTime == -1L) {
                entry.copy(expirationTime = entry.timestamp + DEFAULT_EXPIRATION_MS)
            } else entry
            
            updatedEntries[finalEntry.id] = finalEntry.copy(status = MemoryStatus.ACTIVE)
            
            // Maintain timeline
            val updatedTimeline = (current.timeline + finalEntry.id).takeLast(MAX_ENTRIES)
            
            // Bounded Capacity: Remove oldest if capacity exceeded
            if (updatedEntries.size > MAX_ENTRIES) {
                val oldestId = updatedTimeline.first()
                updatedEntries.remove(oldestId)
            }
            
            current.copy(
                entries = updatedEntries,
                timeline = updatedTimeline
            )
        }
        
        Log.d(TAG, "Recorded memory [${entry.type}]: ${entry.id}")
        cleanupExpired()
    }

    /**
     * Shifts the AI's cognitive focus to a specific entry.
     */
    fun setFocus(entryId: String) {
        _state.update { current ->
            if (!current.entries.containsKey(entryId)) return@update current
            
            val updatedStack = (listOf(entryId) + current.focusStack.filter { it != entryId }).take(5)
            
            // Mark as referenced
            val updatedEntries = current.entries.toMutableMap()
            updatedEntries[entryId]?.let {
                updatedEntries[entryId] = it.copy(status = MemoryStatus.REFERENCED)
            }
            
            current.copy(
                activeFocusId = entryId,
                focusStack = updatedStack,
                entries = updatedEntries
            )
        }
        Log.i(TAG, "Shifted focus to: $entryId")
    }

    /**
     * Periodically removes items that have reached their expiration time.
     */
    fun cleanupExpired() {
        val now = System.currentTimeMillis()
        _state.update { current ->
            val toRemove = current.entries.filter { it.value.expirationTime != -1L && it.value.expirationTime < now }.keys
            if (toRemove.isEmpty()) return@update current
            
            val updatedEntries = current.entries.toMutableMap()
            toRemove.forEach { updatedEntries.remove(it) }
            
            val updatedTimeline = current.timeline.filter { !toRemove.contains(it) }
            val updatedStack = current.focusStack.filter { !toRemove.contains(it) }
            
            current.copy(
                entries = updatedEntries,
                timeline = updatedTimeline,
                focusStack = updatedStack,
                activeFocusId = if (toRemove.contains(current.activeFocusId)) updatedStack.firstOrNull() else current.activeFocusId
            )
        }
    }

    /**
     * Fully clears session memory.
     */
    fun clearSession() {
        _state.value = MemoryState()
        Log.i(TAG, "Working memory cleared.")
    }
}
