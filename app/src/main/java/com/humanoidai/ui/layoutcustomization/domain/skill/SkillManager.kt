package com.humanoidai.ui.layoutcustomization.domain.skill

import android.content.Context
import android.util.Log
import com.humanoidai.runtime.AIRuntimeManager
import com.humanoidai.runtime.HealthState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Orchestrates skill execution and aggregates results.
 */
class SkillManager(
    private val context: Context? = null,
    private val registry: SkillRegistry = SkillRegistry()
) {
    companion object {
        private const val TAG = "SkillManager"
    }

    /**
     * Invokes a specific skill by ID.
     */
    suspend fun invokeSkill(skillId: String, context: SkillContext): SkillResult {
        val skill = registry.getSkill(skillId) ?: return SkillResult(
            skillId = skillId,
            status = SkillStatus.ERROR,
            error = "Skill not found in registry."
        )

        // Runtime Health Check
        val runtimeManager = this.context?.let { AIRuntimeManager.getInstance(it) }
        val health = runtimeManager?.healthState?.value ?: HealthState.HEALTHY
        
        if (health == HealthState.DEGRADED && skill.id != "essential_perception") {
            return SkillResult(
                skillId = skillId,
                status = SkillStatus.ERROR,
                error = "System in degraded state. Optional skills suspended."
            )
        }

        if (!skill.isAvailable()) {
            return SkillResult(
                skillId = skillId,
                status = SkillStatus.ERROR,
                error = "Skill is currently unavailable."
            )
        }

        Log.i(TAG, "Invoking skill: $skillId")
        val trust = this.context?.let { com.humanoidai.security.TrustFramework.getInstance(it) }
        
        return if (trust != null) {
            trust.sandbox.executeSecurely(skill, context) {
                withContext(Dispatchers.Default) {
                    skill.execute(context)
                }
            }
        } else {
            val startTime = System.currentTimeMillis()
            try {
                val result = withContext(Dispatchers.Default) {
                    skill.execute(context)
                }
                result.copy(executionTimeMs = System.currentTimeMillis() - startTime)
            } catch (e: Exception) {
                Log.e(TAG, "Skill execution failed: $skillId", e)
                SkillResult(
                    skillId = skillId,
                    status = SkillStatus.ERROR,
                    error = e.message ?: "Unknown execution error",
                    executionTimeMs = System.currentTimeMillis() - startTime
                )
            }
        }
    }
}
