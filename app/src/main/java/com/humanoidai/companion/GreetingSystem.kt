package com.humanoidai.companion

import com.humanoidai.ml.OwnerEnrollmentManager
import java.util.*

/**
 * Manages proactive greetings based on owner identity and time of day.
 */
class GreetingSystem(private val ownerManager: OwnerEnrollmentManager) {

    fun generateGreeting(): String {
        val ownerName = ownerManager.getOwnerName()
        if (ownerName.isEmpty()) return "System online. Please enroll as owner."

        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        
        val timeBasedGreeting = when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }

        return "$timeBasedGreeting, $ownerName. Humanoid AI is now active and monitoring your environment."
    }
}
