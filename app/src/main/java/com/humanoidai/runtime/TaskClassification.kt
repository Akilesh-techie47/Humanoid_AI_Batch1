package com.humanoidai.runtime

/**
 * Priority levels for AI tasks.
 */
enum class TaskPriority(val weight: Int) {
    /**
     * Essential for system stability and core user experience (e.g., Camera Preview, ROI Tracking).
     */
    CRITICAL(100),

    /**
     * Important features with high visibility (e.g., Object Detection, Active Conversation).
     */
    HIGH(75),

    /**
     * Background processing and pre-loading (e.g., Skill preparation, OCR).
     */
    MEDIUM(50),

    /**
     * Non-essential background tasks (e.g., Analytics, Cache cleanup).
     */
    LOW(25)
}

/**
 * Category of the task to help with resource grouping.
 */
enum class TaskCategory {
    VISION,
    AUDIO,
    REASONING,
    SKILL,
    SYSTEM,
    UI
}
