package com.humanoidai.security

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Structured security event logging.
 */
class AuditLogger {

    companion object {
        private const val TAG = "AuditLogger"
        private const val MAX_LOGS = 100
    }

    private val _events = MutableStateFlow<List<SecurityEvent>>(emptyList())
    val events: StateFlow<List<SecurityEvent>> = _events.asStateFlow()

    fun log(
        category: SecurityCategory,
        action: String,
        status: SecurityStatus = SecurityStatus.INFO,
        details: String = ""
    ) {
        val event = SecurityEvent(
            timestamp = System.currentTimeMillis(),
            category = category,
            action = action,
            status = status,
            details = details
        )

        Log.i(TAG, "Security Event: [$category] $action ($status) - $details")

        _events.update { current ->
            (listOf(event) + current).take(MAX_LOGS)
        }
    }
}

data class SecurityEvent(
    val timestamp: Long,
    val category: SecurityCategory,
    val action: String,
    val status: SecurityStatus,
    val details: String
)

enum class SecurityCategory {
    SESSION,
    PERMISSION,
    PRIVACY,
    SKILL,
    INTEGRITY,
    SYSTEM
}

enum class SecurityStatus {
    INFO,
    WARNING,
    ALERT,
    VIOLATION
}
