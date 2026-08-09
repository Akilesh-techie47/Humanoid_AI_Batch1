package com.humanoidai.ui.layoutcustomization.domain.skill

import androidx.compose.runtime.Immutable
import com.humanoidai.ui.layoutcustomization.domain.perception.PerceptionState
import com.humanoidai.ui.layoutcustomization.domain.reasoning.SituationState

/**
 * Functional categories for AI Skills.
 */
enum class SkillCategory {
    VISION,
    LANGUAGE,
    UTILITY,
    PRODUCTIVITY,
    NAVIGATION,
    DEVELOPER
}

/**
 * Current operating status of a skill.
 */
enum class SkillStatus {
    DISCOVERED,
    REGISTERED,
    INITIALIZED,
    READY,
    RUNNING,
    COMPLETED,
    ERROR,
    IDLE
}

/**
 * Unified context provided to a skill during invocation.
 */
@Immutable
data class SkillContext(
    val situation: SituationState,
    val perception: PerceptionState,
    val userRequest: String? = null,
    val deviceState: Map<String, Any> = emptyMap()
)

/**
 * Standardized result returned by any AI Skill.
 */
@Immutable
data class SkillResult(
    val skillId: String,
    val status: SkillStatus,
    val confidence: Float = 1.0f,
    val payload: Any? = null,
    val error: String? = null,
    val executionTimeMs: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)
