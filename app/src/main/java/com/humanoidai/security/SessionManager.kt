package com.humanoidai.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Manages the lifecycle of AI interaction sessions.
 */
class SessionManager(private val auditLogger: AuditLogger) {

    private val _currentSession = MutableStateFlow<AiSession?>(null)
    val currentSession: StateFlow<AiSession?> = _currentSession.asStateFlow()

    fun createSession(userId: String): AiSession {
        val session = AiSession(
            id = UUID.randomUUID().toString(),
            userId = userId,
            startTime = System.currentTimeMillis(),
            accessLevel = UserAccessLevel.OWNER
        )
        _currentSession.value = session
        auditLogger.log(SecurityCategory.SESSION, "Created", details = "User: $userId, Level: OWNER")
        return session
    }

    fun authenticateSession() {
        _currentSession.value?.let { session ->
            _currentSession.value = session.copy(status = SessionStatus.AUTHENTICATED)
            auditLogger.log(SecurityCategory.SESSION, "Authenticated", details = "Session: ${session.id}")
        }
    }

    fun activateSession() {
        _currentSession.value?.let { session ->
            if (session.status == SessionStatus.AUTHENTICATED || session.status == SessionStatus.LOCKED) {
                _currentSession.value = session.copy(status = SessionStatus.ACTIVE)
                auditLogger.log(SecurityCategory.SESSION, "Activated", details = "Session: ${session.id}")
            }
        }
    }

    fun lockSession() {
        _currentSession.value?.let { session ->
            if (session.status == SessionStatus.ACTIVE) {
                _currentSession.value = session.copy(status = SessionStatus.LOCKED)
                auditLogger.log(SecurityCategory.SESSION, "Locked", details = "Session: ${session.id}")
            }
        }
    }

    fun closeSession() {
        _currentSession.value?.let { session ->
            _currentSession.value = session.copy(
                status = SessionStatus.CLOSED,
                endTime = System.currentTimeMillis()
            )
            auditLogger.log(SecurityCategory.SESSION, "Closed", details = "Session: ${session.id}")
            // Clear current session after logging
            _currentSession.value = null
        }
    }

    fun isSessionActive(): Boolean {
        return _currentSession.value?.status == SessionStatus.ACTIVE
    }
}

data class AiSession(
    val id: String,
    val userId: String,
    val startTime: Long,
    val endTime: Long = 0,
    val status: SessionStatus = SessionStatus.CREATED,
    val accessLevel: UserAccessLevel = UserAccessLevel.OWNER
)

enum class UserAccessLevel {
    OWNER
}

enum class SessionStatus {
    CREATED,
    AUTHENTICATED,
    ACTIVE,
    LOCKED,
    CLOSED
}
