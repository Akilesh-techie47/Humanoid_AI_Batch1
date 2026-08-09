package com.humanoidai.ui.layoutcustomization.domain.skill

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Discovery and storage for available AI Skills.
 */
class SkillRegistry {
    
    private val _skills = MutableStateFlow<Map<String, AISkill>>(emptyMap())
    val skills: StateFlow<Map<String, AISkill>> = _skills.asStateFlow()

    fun register(skill: AISkill) {
        _skills.update { it + (skill.id to skill) }
    }

    fun unregister(skillId: String) {
        _skills.update { it - skillId }
    }

    fun getSkill(id: String): AISkill? = _skills.value[id]

    fun findByCategory(category: SkillCategory): List<AISkill> {
        return _skills.value.values.filter { it.category == category }
    }
}
