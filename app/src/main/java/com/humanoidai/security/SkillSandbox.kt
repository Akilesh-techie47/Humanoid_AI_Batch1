package com.humanoidai.security

import android.util.Log
import com.humanoidai.ui.layoutcustomization.domain.skill.AISkill
import com.humanoidai.ui.layoutcustomization.domain.skill.SkillContext
import com.humanoidai.ui.layoutcustomization.domain.skill.SkillResult
import com.humanoidai.ui.layoutcustomization.domain.skill.SkillStatus

/**
 * Isolates skill execution and enforces permissions.
 */
class SkillSandbox(
    private val permissionEngine: PermissionEngine,
    private val auditLogger: AuditLogger
) {

    companion object {
        private const val TAG = "SkillSandbox"
    }

    suspend fun executeSecurely(
        skill: AISkill,
        context: SkillContext,
        executionBlock: suspend () -> SkillResult
    ): SkillResult {
        // 1. Verify required permissions
        val missingPermissions = skill.requiredPermissions.filter { permName ->
            // Map skill permission strings to AiPermission enums
            val aiPerm = mapToAiPermission(permName)
            aiPerm == null || !permissionEngine.isPermissionGranted(aiPerm)
        }

        if (missingPermissions.isNotEmpty()) {
            val error = "Denied: Skill ${skill.displayName} missing permissions: $missingPermissions"
            auditLogger.log(SecurityCategory.SKILL, "AccessDenied", SecurityStatus.VIOLATION, error)
            return SkillResult(
                skillId = skill.id,
                status = SkillStatus.ERROR,
                error = error
            )
        }

        // 2. Integrity check (Simulation)
        if (skill.version.contains("alpha") && !skill.id.startsWith("humanoid.")) {
             auditLogger.log(SecurityCategory.INTEGRITY, "Warning", SecurityStatus.WARNING, "Running non-humanoid alpha skill: ${skill.id}")
        }

        // 3. Execute
        auditLogger.log(SecurityCategory.SKILL, "Execute", details = skill.id)
        return try {
            executionBlock()
        } catch (e: Exception) {
            Log.e(TAG, "Sandbox trapped crash in ${skill.id}", e)
            auditLogger.log(SecurityCategory.SKILL, "Crash", SecurityStatus.ALERT, "Skill ${skill.id} failed: ${e.message}")
            SkillResult(
                skillId = skill.id,
                status = SkillStatus.ERROR,
                error = "Sandboxed execution failure: ${e.message}"
            )
        }
    }

    private fun mapToAiPermission(name: String): AiPermission? {
        return when (name.uppercase()) {
            "CAMERA" -> AiPermission.CAMERA
            "MICROPHONE", "RECORD_AUDIO" -> AiPermission.MICROPHONE
            "NOTIFICATIONS" -> AiPermission.NOTIFICATIONS
            "CLOUD" -> AiPermission.CLOUD_INFERENCE
            else -> null
        }
    }
}
