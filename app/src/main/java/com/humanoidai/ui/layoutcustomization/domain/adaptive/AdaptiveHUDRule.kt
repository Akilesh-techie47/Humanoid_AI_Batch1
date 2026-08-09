package com.humanoidai.ui.layoutcustomization.domain.adaptive

/**
 * Interface for an adaptive rule that evaluates context and suggests actions.
 */
interface AdaptiveHUDRule {
    val name: String
    val priority: Int
    fun evaluate(context: HUDContext): List<HUDAction>
}

/**
 * Standard rule priority levels.
 */
object RulePriority {
    const val CRITICAL = 100
    const val HIGH = 80
    const val MEDIUM = 50
    const val LOW = 20
}
