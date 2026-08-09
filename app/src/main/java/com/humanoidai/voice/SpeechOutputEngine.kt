package com.humanoidai.voice

enum class SpeechTone { NEUTRAL, ALERT, CONCERNED, PLAYFUL, URGENT }

interface SpeechOutputEngine {
    fun speak(text: String, tone: SpeechTone, onComplete: () -> Unit = {})
    fun stop()
    fun shutdown()
}
