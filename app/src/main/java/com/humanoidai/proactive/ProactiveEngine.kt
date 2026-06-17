package com.humanoidai.proactive

import com.humanoidai.context.CurrentContext
import com.humanoidai.memory.LongTermMemory

/**
 * The proactive heart of Humanoid.
 * Decides when to speak without being asked.
 */
class ProactiveEngine(
    private val memory: LongTermMemory,
    private val onActionRequired: (String) -> Unit
) {
    private var lastProactiveTime = 0L
    private val PROACTIVE_COOLDOWN = 60000L // 1 minute

    fun evaluate(context: CurrentContext) {
        val now = System.currentTimeMillis()
        if (now - lastProactiveTime < PROACTIVE_COOLDOWN) return

        // 1. Alert Proactivity
        if (context.currentAlerts > 0) {
            onActionRequired("Security alert active. I am monitoring the unknown subject.")
            lastProactiveTime = now
            return
        }

        // 2. Battery Proactivity
        if (context.batteryLevel in 1..15) {
            onActionRequired("Battery is low at ${context.batteryLevel}%. Please connect to power.")
            lastProactiveTime = now
            return
        }

        // 3. Social Proactivity (New arrival already handled by FaceAnalyzer, but can be refined here)
        if (context.unknownCount > 1) {
            onActionRequired("I see multiple unrecognized individuals. Initiating group logging.")
            lastProactiveTime = now
            return
        }
    }
}
