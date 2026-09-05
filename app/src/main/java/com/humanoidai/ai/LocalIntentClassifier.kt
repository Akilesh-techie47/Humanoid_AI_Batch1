package com.humanoidai.ai

/**
 * Fast local classifier for deterministic commands.
 * Bypasses Gemini for low-latency system actions.
 */
object LocalIntentClassifier {

    enum class LocalIntent {
        WHAT_TIME,
        GO_HOME,
        OPEN_SETTINGS,
        TURN_ON_CAMERA,
        WHAT_DID_I_MISS,
        WHO_CALLED_ME,
        WHO_ARE_YOU,
        UNKNOWN
    }

    fun classify(text: String): LocalIntent {
        val input = text.lowercase()
        
        return when {
            input.contains("time") && (input.contains("what") || input.contains("current")) -> LocalIntent.WHAT_TIME
            input.contains("go home") || input.contains("main screen") || input.contains("dashboard") || (input.contains("go") && input.contains("back")) -> LocalIntent.GO_HOME
            input.contains("settings") || input.contains("configuration") || input.contains("personalize") -> LocalIntent.OPEN_SETTINGS
            input.contains("camera on") || input.contains("turn on camera") || input.contains("start camera") || input.contains("vision") || input.contains("eyes") -> LocalIntent.TURN_ON_CAMERA
            input.contains("what did i miss") || input.contains("notifications") || input.contains("briefing") || input.contains("happen") || input.contains("summary") -> LocalIntent.WHAT_DID_I_MISS
            input.contains("who called") || input.contains("missed calls") || input.contains("phone") -> LocalIntent.WHO_CALLED_ME
            input.contains("who are you") || input.contains("your name") || input.contains("yourself") -> LocalIntent.WHO_ARE_YOU
            else -> LocalIntent.UNKNOWN
        }
    }
}
