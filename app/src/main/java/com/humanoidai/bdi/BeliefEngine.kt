package com.humanoidai.bdi

import com.humanoidai.context.CurrentContext

/**
 * Translates raw context into higher-level beliefs.
 */
class BeliefEngine {
    private val beliefs = mutableMapOf<String, Belief>()

    fun updateBeliefs(context: CurrentContext): Map<String, Belief> {
        // 1. Security Beliefs
        beliefs["threat_detected"] = Belief("threat_detected", context.recentAlerts.isNotEmpty())
        beliefs["unknown_presence"] = Belief("unknown_presence", context.unknownCount > 0)
        
        // 2. Social Beliefs
        beliefs["owner_present"] = Belief("owner_present", context.visiblePeople.any { it.name == context.ownerName })
        beliefs["social_opportunity"] = Belief("social_opportunity", context.visiblePeople.isNotEmpty())

        // 3. System Beliefs
        beliefs["battery_low"] = Belief("battery_low", context.batteryPercent in 1..20)
        beliefs["system_healthy"] = Belief("system_healthy", context.batteryPercent > 20)

        // 4. Environment Beliefs
        beliefs["is_quiet"] = Belief("is_quiet", context.noiseLevel < 40f)
        beliefs["is_dark"] = Belief("is_dark", context.ambientLight < 10f)

        return beliefs.toMap()
    }
}
