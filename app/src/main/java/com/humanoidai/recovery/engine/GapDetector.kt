package com.humanoidai.recovery.engine

import com.humanoidai.recovery.model.GapCause
import java.util.UUID

interface LastContextTimestampProvider {
    suspend fun getLastContextTimestamp(): Long?
    suspend fun getLastContextEventType(): String?
}

data class DetectedGap(
    val gapId: String,
    val gapStart: Long,
    val gapEnd: Long,
    val cause: GapCause
)

class GapDetector(
    private val timestampProvider: LastContextTimestampProvider,
    private val gapThresholdMillis: Long = DEFAULT_GAP_THRESHOLD_MILLIS
) {

    suspend fun checkForGap(now: Long = System.currentTimeMillis()): DetectedGap? {
        val lastTimestamp = timestampProvider.getLastContextTimestamp() ?: return null
        val elapsed = now - lastTimestamp

        if (elapsed < gapThresholdMillis) return null

        val cause = inferCause(timestampProvider.getLastContextEventType())

        return DetectedGap(
            gapId = UUID.randomUUID().toString(),
            gapStart = lastTimestamp,
            gapEnd = now,
            cause = cause
        )
    }

    private fun inferCause(lastEventType: String?): GapCause {
        return when (lastEventType) {
            "CAMERA_OCCLUDED" -> GapCause.CAMERA_OCCLUDED
            "PERMISSION_REVOKED" -> GapCause.PERMISSION_REVOKED
            "APP_BACKGROUNDED" -> GapCause.BACKGROUNDED
            "DEVICE_DOZE" -> GapCause.DOZE
            else -> GapCause.UNKNOWN
        }
    }

    companion object {
        const val DEFAULT_GAP_THRESHOLD_MILLIS = 2 * 60 * 1000L
    }
}
