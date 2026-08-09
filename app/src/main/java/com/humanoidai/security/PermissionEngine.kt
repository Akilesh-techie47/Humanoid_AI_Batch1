package com.humanoidai.security

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Governs sensor and capability access.
 */
class PermissionEngine(
    private val context: Context,
    private val auditLogger: AuditLogger
) {

    private val _permissions = MutableStateFlow<Map<AiPermission, Boolean>>(emptyMap())
    val permissions: StateFlow<Map<AiPermission, Boolean>> = _permissions.asStateFlow()

    init {
        refreshPermissions()
    }

    fun refreshPermissions() {
        val updated = AiPermission.entries.associateWith { permission ->
            isPermissionGranted(permission)
        }
        _permissions.value = updated
    }

    fun isPermissionGranted(permission: AiPermission): Boolean {
        // 1. Check Android system permission if applicable
        val systemGranted = permission.androidPermission?.let {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        } ?: true

        // 2. Check Logical / Policy constraints (future expansion)
        
        return systemGranted
    }

    fun requestPermission(permission: AiPermission, onResult: (Boolean) -> Unit) {
        // Note: Real request must happen in Activity/Fragment.
        // This engine tracks and validates.
        auditLogger.log(SecurityCategory.PERMISSION, "Request", details = permission.name)
        // Implementation would delegate to a handler that uses ActivityResultLauncher
    }

    fun logViolation(permission: AiPermission, details: String) {
        auditLogger.log(
            SecurityCategory.PERMISSION,
            "Violation",
            SecurityStatus.VIOLATION,
            "Denied access to ${permission.name}: $details"
        )
    }
}

enum class AiPermission(val androidPermission: String? = null) {
    CAMERA(android.Manifest.permission.CAMERA),
    MICROPHONE(android.Manifest.permission.RECORD_AUDIO),
    NOTIFICATIONS(if (android.os.Build.VERSION.SDK_INT >= 33) android.Manifest.permission.POST_NOTIFICATIONS else null),
    CLOUD_INFERENCE(null), // Logical permission
    THIRD_PARTY_SKILLS(null) // Logical permission
}
