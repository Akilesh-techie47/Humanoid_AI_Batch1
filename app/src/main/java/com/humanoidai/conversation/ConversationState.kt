package com.humanoidai.conversation

/**
 * Defines the operational states of the Conversation Engine.
 */
enum class ConversationState {
    IDLE,       // Not in a conversation
    LISTENING,  // Actively listening to user speech
    THINKING,   // AI is processing or generating a response
    SPEAKING,   // AI is currently speaking via TTS
    WAITING,    // AI has finished speaking and is waiting for a follow-up
    PASSIVE     // AI is in the background, only listening for wake words
}
