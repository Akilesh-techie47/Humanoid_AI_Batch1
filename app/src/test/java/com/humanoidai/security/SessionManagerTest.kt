package com.humanoidai.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionManagerTest {

    private val auditLogger = AuditLogger()
    private val sessionManager = SessionManager(auditLogger)

    @Test
    fun `test session lifecycle`() {
        assertNull(sessionManager.currentSession.value)
        
        // 1. Create
        sessionManager.createSession("user_123")
        val session = sessionManager.currentSession.value
        assertEquals("user_123", session?.userId)
        assertEquals(SessionStatus.CREATED, session?.status)
        assertFalse(sessionManager.isSessionActive())

        // 2. Authenticate
        sessionManager.authenticateSession()
        assertEquals(SessionStatus.AUTHENTICATED, sessionManager.currentSession.value?.status)
        assertFalse(sessionManager.isSessionActive())

        // 3. Activate
        sessionManager.activateSession()
        assertEquals(SessionStatus.ACTIVE, sessionManager.currentSession.value?.status)
        assertTrue(sessionManager.isSessionActive())

        // 4. Lock
        sessionManager.lockSession()
        assertEquals(SessionStatus.LOCKED, sessionManager.currentSession.value?.status)
        assertFalse(sessionManager.isSessionActive())

        // 5. Unlock (Back to Active)
        sessionManager.activateSession()
        assertTrue(sessionManager.isSessionActive())

        // 6. Close
        sessionManager.closeSession()
        assertNull(sessionManager.currentSession.value)
    }
}
