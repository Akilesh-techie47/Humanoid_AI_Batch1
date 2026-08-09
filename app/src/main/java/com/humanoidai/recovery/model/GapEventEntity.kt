package com.humanoidai.recovery.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gap_events")
data class GapEventEntity(
    @PrimaryKey val gapId: String,
    val gapStart: Long,
    val gapEnd: Long,
    val cause: String,                  // GapCause.name
    val lastKnownContextJson: String?,   // ContextSnapshot serialized as JSON
    val eventsDuringGapJson: String,     // List<GapAlertRef> serialized as JSON
    val reconstructedSummary: String?,
    val tier: String,                    // RecoveryTier.name
    val acknowledged: Boolean = false
)
