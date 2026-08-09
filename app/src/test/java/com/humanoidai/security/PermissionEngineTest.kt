package com.humanoidai.security

import android.content.Context
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class PermissionEngineTest {

    private val context = mock(Context::class.java)
    private val auditLogger = AuditLogger()
    private val permissionEngine = PermissionEngine(context, auditLogger)

    @Test
    fun testLogicalPermissionsAlwaysGranted() {
        // Logical permissions like CLOUD_INFERENCE don't have an androidPermission string
        assertTrue(permissionEngine.isPermissionGranted(AiPermission.CLOUD_INFERENCE))
        assertTrue(permissionEngine.isPermissionGranted(AiPermission.THIRD_PARTY_SKILLS))
    }
}
