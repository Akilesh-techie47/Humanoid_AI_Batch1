package com.humanoidai.bdi

/**
 * Selects the best action (Intention) to fulfill the strongest desires.
 */
class IntentionEngine {

    fun determineIntention(
        desires: List<Pair<Desire, Int>>,
        beliefs: Map<String, Belief>
    ): Intention? {
        if (desires.isEmpty()) return null

        val strongestDesire = desires.first().first
        
        return when (strongestDesire) {
            Desire.MAINTAIN_SECURITY -> {
                if (beliefs["threat_detected"]?.value == true) {
                    Intention(strongestDesire, { /* Handled via proactive callback */ }, 100)
                } else {
                    null
                }
            }
            Desire.SYSTEM_MAINTENANCE -> {
                Intention(strongestDesire, { /* Handled via proactive callback */ }, 80)
            }
            Desire.SOCIAL_INTERACTION -> {
                Intention(strongestDesire, { /* Handled via proactive callback */ }, 60)
            }
            else -> null
        }
    }
}
