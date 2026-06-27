package com.humanoidai.companion

/**
 * Defines the operational states of the Humanoid AI Companion.
 * All modules read and write to this state.
 */
sealed class CompanionState(val label: String) {
    object SLEEPING : CompanionState("Sleeping")
    object WAKING : CompanionState("Waking Up...")
    object OBSERVING : CompanionState("Observing Quietly")
    object LISTENING : CompanionState("Listening...")
    object THINKING : CompanionState("Thinking...")
    object SPEAKING : CompanionState("Speaking...")
    object WAITING : CompanionState("Waiting...")
    object ALERTING : CompanionState("ALERTING")
}
