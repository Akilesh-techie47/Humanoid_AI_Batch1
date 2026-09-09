package com.humanoidai.security

import android.content.Context
import com.humanoidai.memory.database.Aura360Database

/**
 * The central coordination layer for system security and trust.
 */
class TrustFramework(private val context: Context) {

    val auditLogger = AuditLogger(Aura360Database.getInstance(context).auditLogDao())
    val sessionManager = SessionManager(auditLogger)
    val permissionEngine = PermissionEngine(context, auditLogger)
    val privacyManager = PrivacyManager(auditLogger)
    val sandbox = SkillSandbox(permissionEngine, auditLogger)

    fun initialize() {
        auditLogger.log(SecurityCategory.SYSTEM, "TrustFramework initialized")
        permissionEngine.refreshPermissions()
    }

    companion object {
        @Volatile
        private var instance: TrustFramework? = null

        fun getInstance(context: Context): TrustFramework {
            return instance ?: synchronized(this) {
                instance ?: TrustFramework(context.applicationContext).also { instance = it }
            }
        }
    }
}
