package com.humanoidai.bdi

/**
 * Reasons about beliefs to generate potential goals (Desires).
 */
class DesireEngine {

    fun generateDesires(beliefs: Map<String, Belief>): List<Pair<Desire, Int>> {
        val desires = mutableListOf<Pair<Desire, Int>>()

        // Priority 1: Security
        if (beliefs["threat_detected"]?.value == true) {
            desires.add(Desire.MAINTAIN_SECURITY to 100)
        }

        // Priority 2: System Health
        if (beliefs["battery_low"]?.value == true) {
            desires.add(Desire.SYSTEM_MAINTENANCE to 80)
        }

        // Priority 3: Social
        if (beliefs["owner_present"]?.value == true) {
            desires.add(Desire.SOCIAL_INTERACTION to 60)
        } else if (beliefs["unknown_presence"]?.value == true) {
            desires.add(Desire.MAINTAIN_SECURITY to 50)
        }

        // Priority 4: Monitoring
        desires.add(Desire.ENVIRONMENT_MONITORING to 10)

        return desires.sortedByDescending { it.second }
    }
}
