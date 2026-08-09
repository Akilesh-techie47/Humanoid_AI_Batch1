package com.humanoidai.ui.layoutcustomization.domain.skill

/**
 * Interface that every modular AI capability must implement.
 */
interface AISkill {
    val id: String
    val displayName: String
    val category: SkillCategory
    val version: String
    val requiredPermissions: List<String>
    
    /**
     * Executes the skill's logic with the provided context.
     */
    suspend fun execute(context: SkillContext): SkillResult
    
    /**
     * Checks if the skill is currently capable of running (e.g. connectivity, hardware).
     */
    fun isAvailable(): Boolean = true
}
