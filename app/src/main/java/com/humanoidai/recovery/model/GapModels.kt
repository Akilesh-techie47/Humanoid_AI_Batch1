package com.humanoidai.recovery.model

/**
 * Represents a single detected "gap" — a period where the app was not
 * actively perceiving (backgrounded, camera occluded, crashed, etc.)
 */
data class GapEvent(
    val gapId: String,
    val gapStart: Long,
    val gapEnd: Long,
    val cause: GapCause,
    val lastKnownContext: ContextSnapshot?,
    val eventsDuringGap: List<GapAlertRef>,
    val reconstructedSummary: String?,
    val tier: RecoveryTier,
    val acknowledged: Boolean = false
) {
    val durationMillis: Long get() = gapEnd - gapStart
    val durationMinutes: Long get() = durationMillis / 60_000
}

enum class GapCause {
    BACKGROUNDED,
    CAMERA_OCCLUDED,
    PERMISSION_REVOKED,
    CRASH,
    DOZE,
    UNKNOWN
}

enum class RecoveryTier {
    NOTHING_TO_REPORT,
    PARTIAL_DATA,
    RICH_CONTEXT
}

data class ContextSnapshot(
    val timestamp: Long,
    val presentPeopleNames: List<String>,
    val ownerEmotion: String?,
    val primaryFocusName: String?
)

data class GapAlertRef(
    val alertId: String,
    val timestamp: Long,
    val alertType: String,
    val description: String
)
