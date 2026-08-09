package com.humanoidai.context

/**
 * Data model for a person within the AI's current context.
 */
data class PersonContext(
    val name: String,
    val label: String,
    val confidence: Float,
    val isPrimary: Boolean = false,
    val lastSeenAt: Long = System.currentTimeMillis(),
    val distanceCategory: String = "MEDIUM",
    val isLookingAtCamera: Boolean = false
)
