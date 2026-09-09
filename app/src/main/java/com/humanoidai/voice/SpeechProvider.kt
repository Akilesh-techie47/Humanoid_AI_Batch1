package com.humanoidai.voice

import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction for Speech-to-Text providers.
 */
interface SpeechProvider {
    val id: String
    val name: String
    
    fun startListening(
        onPartialResult: (String) -> Unit = {},
        onFinalResult: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    )
    
    fun stopListening()
    fun destroy()
    
    fun isAvailable(): Boolean
}
