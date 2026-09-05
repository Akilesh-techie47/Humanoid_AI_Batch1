package com.humanoidai.security

import android.util.Log
import com.humanoidai.memory.dao.AuditLogDao
import com.humanoidai.memory.entities.AuditLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Structured security event logging with persistence.
 */
class AuditLogger(private val auditLogDao: AuditLogDao? = null) {

    companion object {
        private const val TAG = "AuditLogger"
        private const val MAX_LOGS = 100
    }

    private val scope = CoroutineScope(Dispatchers.IO)
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

        // Persist to database
        auditLogDao?.let { dao ->
            scope.launch {
                try {
                    dao.insertLog(AuditLogEntity(
                        timestamp = event.timestamp,
                        category = event.category.name,
                        action = event.action,
                        status = event.status.name,
                        details = event.details
                    ))
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to persist audit log: ${e.message}")
                }
            }
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
