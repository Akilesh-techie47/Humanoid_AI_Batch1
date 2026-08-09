package com.humanoidai.hearing

import com.humanoidai.alerts.AlertEngine
import com.humanoidai.memory.entities.VoiceProfileEntity

sealed class CheckpointDecision {
    object Allow : CheckpointDecision()
    data class Deny(val reason: String) : CheckpointDecision()
}

/**
 * Gates voice commands during elevated alert states.
 */
class VoiceCheckpoint(
    private val identifier: VoiceIdentifier,
    private val alertEngine: AlertEngine
) {

    fun gate(liveEmbedding: FloatArray, trustedProfiles: List<VoiceProfileEntity>): CheckpointDecision {
        // If system is in a normal state, we don't gate strictly
        if (alertEngine.unreadCount.value == 0) return CheckpointDecision.Allow

        val match = identifier.match(liveEmbedding, trustedProfiles)
        
        return if (match.isTrusted) {
            CheckpointDecision.Allow
        } else {
            CheckpointDecision.Deny("Voice identity unverified during active alert.")
        }
    }
}
