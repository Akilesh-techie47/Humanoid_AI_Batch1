package com.humanoidai.hearing

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class MicState {
    IDLE,
    STARTING,
    LISTENING_PASSIVE, // Wake word mode
    LISTENING_ACTIVE,  // User command mode
    PROCESSING,
    STOPPING,
    ERROR,
    RECOVERING
}

/**
 * Authoritative Speech Recognizer Manager for Humanoid AI.
 * Implements a state machine to ensure stable transitions and suppresses system beeps.
 */
class SpeechRecognizerManager(
    private val context: Context,
    private val mainHandler: Handler = Handler(Looper.getMainLooper())
) {

    private var speechRecognizer: SpeechRecognizer? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    private val _state = MutableStateFlow(MicState.IDLE)
    val state: StateFlow<MicState> = _state.asStateFlow()

    private val _isListening = MutableStateFlow(value = false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _ambientNoise = MutableStateFlow(0f)
    val ambientNoise: StateFlow<Float> = _ambientNoise.asStateFlow()

    private val wakeWordManager = WakeWordManager()
    private var onWakeWordDetected: (() -> Unit)? = null
    private var onSpeechStarted: (() -> Unit)? = null
    private var activeCallbacks: Pair<(String) -> Unit, (String) -> Unit>? = null

    private var originalAudioMode = AudioManager.MODE_NORMAL
    private var currentLanguage = "en-US"
    private var consecutiveErrorCount = 0

    companion object {
        private const val TAG = "SpeechRecognizerManager"
        private const val RESTART_DELAY_MS = 500L // Reduced from 2000L for faster recovery
        private const val MAX_RETRY_COUNT = 3
    }

    init {
        initializeRecognizer()
    }

    private fun initializeRecognizer() {
        mainHandler.post {
            try {
                Log.d(TAG, "[VOICE] INITIALIZING_RECOGNIZER")
                speechRecognizer?.destroy()
            } catch (_: Exception) {}

            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(InternalRecognitionListener())
                }
                Log.i(TAG, "[VOICE] RECOGNIZER_CREATED_SUCCESSFULLY")
            } else {
                Log.e(TAG, "[VOICE] RECOGNIZER_UNAVAILABLE_ON_DEVICE")
                _state.value = MicState.ERROR
            }
        }
    }

    private inner class InternalRecognitionListener : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.i(TAG, "[VOICE] READY_FOR_SPEECH state=${_state.value}")
            _isListening.value = true
            restoreAudioState()
            if (_state.value == MicState.STARTING) {
                _state.value = if (activeCallbacks == null) MicState.LISTENING_PASSIVE else MicState.LISTENING_ACTIVE
            }
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "[VOICE] BEGINNING_OF_SPEECH")
            onSpeechStarted?.invoke()
        }

        override fun onRmsChanged(rmsdB: Float) {
            if (rmsdB > 0) {
                 Log.v(TAG, "[VOICE] AUDIO_LEVEL: $rmsdB")
            }
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f) * 100f
            _ambientNoise.value = normalized
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        private var speechEndTime = 0L

        override fun onEndOfSpeech() {
            speechEndTime = System.currentTimeMillis()
            Log.d(TAG, "[VOICE] END_OF_SPEECH [TS] $speechEndTime")
            _isListening.value = false
        }

        override fun onError(error: Int) {
            val message = getErrorMessage(error)
            Log.e(TAG, "[VOICE] ERROR: $message ($error). Current state=${_state.value}")
            _isListening.value = false
            restoreAudioState()
            
            handleError(error)
        }

        override fun onResults(results: Bundle?) {
            val transcriptTime = System.currentTimeMillis()
            consecutiveErrorCount = 0
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.getOrNull(0) ?: ""
            Log.i(TAG, "[VOICE] FINAL_RESULT: \"$text\" latency=${transcriptTime - speechEndTime}ms")
            
            val currentState = _state.value
            _state.value = MicState.PROCESSING
            restoreAudioState()
            
            if (currentState == MicState.LISTENING_PASSIVE) {
                if (text.isNotEmpty() && wakeWordManager.checkText(text)) {
                    Log.i(TAG, "[VOICE] WAKE_WORD_DETECTED")
                    onWakeWordDetected?.invoke()
                } else {
                    restartInternal(delay = RESTART_DELAY_MS)
                }
            } else if (currentState == MicState.LISTENING_ACTIVE) {
                activeCallbacks?.second?.invoke(text)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            consecutiveErrorCount = 0
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.getOrNull(0) ?: ""
            if (text.isNotEmpty()) {
                Log.v(TAG, "[VOICE] PARTIAL_RESULT: \"$text\"")
                
                if (_state.value == MicState.LISTENING_PASSIVE) {
                    if (wakeWordManager.checkText(text)) {
                        Log.i(TAG, "[VOICE] WAKE_WORD_DETECTED (partial)")
                        onWakeWordDetected?.invoke()
                    }
                } else {
                    activeCallbacks?.first?.invoke(text)
                }
            }
        }


        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun prepareAudioState() {
        try {
            originalAudioMode = audioManager.mode
            Log.d(TAG, "[AUDIO_FOCUS_REQUEST] Setting MODE_IN_COMMUNICATION to suppress beep. Prev mode: $originalAudioMode")
            // This mode often suppresses the SpeechRecognizer beep on many devices
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set audio mode: ${e.message}")
        }
    }

    private fun restoreAudioState() {
        try {
            if (audioManager.mode != originalAudioMode) {
                Log.d(TAG, "[AUDIO_FOCUS_ABANDONED] Restoring audio mode to $originalAudioMode")
                audioManager.mode = originalAudioMode
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to restore audio mode: ${e.message}")
        }
    }

    fun startPassiveListening(onDetected: (() -> Unit)?, onSpeechStarted: (() -> Unit)? = null) {
        if (_state.value == MicState.LISTENING_PASSIVE) return
        
        Log.i(TAG, "[VOICE] START_PASSIVE_REQUESTED")
        onWakeWordDetected = onDetected
        this.onSpeechStarted = onSpeechStarted
        activeCallbacks = null
        // Enable partial results even for passive to speed up detection
        startInternal(isPartial = true)
    }

    fun startListening(onPartialResult: (String) -> Unit = {}, onFinalResult: (String) -> Unit = {}, onSpeechStarted: (() -> Unit)? = null) {
        if (_state.value == MicState.LISTENING_ACTIVE) return

        Log.i(TAG, "[VOICE] START_ACTIVE_REQUESTED")
        onWakeWordDetected = null
        this.onSpeechStarted = onSpeechStarted
        activeCallbacks = onPartialResult to onFinalResult
        startInternal(isPartial = true)
    }

    private fun startInternal(isPartial: Boolean) {
        mainHandler.post {
            val currentState = _state.value
            if (currentState == MicState.STARTING || currentState == MicState.STOPPING) {
                 Log.w(TAG, "[VOICE] REJECTED_START: State is $currentState")
                 return@post
            }

            Log.d(TAG, "[VOICE] START_REQUESTED: isPartial=$isPartial from $currentState")
            _state.value = MicState.STARTING
            prepareAudioState() 
            
            // Critical fix for "Busy (8)": Ensure complete cancellation before next start
            try {
                speechRecognizer?.cancel()
            } catch (_: Exception) {}

            // Small delay to allow service to clear
            mainHandler.postDelayed({
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguage)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, isPartial)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    
                    // Added for stability
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
                    putExtra("android.speech.extra.DICTATION_MODE", true)
                    
                    // Recognition sensitivity
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                }
                
                try {
                    Log.d(TAG, "[VOICE] START_SUCCESS (Invoking Recognizer)")
                    speechRecognizer?.startListening(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "[VOICE] START_FAILED: ${e.message}")
                    _state.value = MicState.ERROR
                    restoreAudioState()
                    recreateRecognizer()
                }
            }, 400L)
        }
    }

    fun stopListening() {
        val oldState = _state.value
        if (oldState == MicState.IDLE || oldState == MicState.STOPPING) return
        
        Log.i(TAG, "[VOICE] STOP_REQUESTED from $oldState")
        _state.value = MicState.STOPPING
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {}
            restoreAudioState()
            _state.value = MicState.IDLE
        }
    }

    fun setLanguage(langCode: String) {
        currentLanguage = when(langCode) {
            "ta" -> "ta-IN"
            "en" -> "en-US"
            else -> "en-US" 
        }
        Log.d(TAG, "Recognizer language set to: $currentLanguage")
    }

    private fun handleError(error: Int) {
        _state.value = MicState.ERROR
        restoreAudioState()
        
        // Interaction 2.0 Optimization: Only count critical failures that prevent usage
        val isTimeout = error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
        
        Log.w(TAG, "[MIC_ERROR] code=$error. State=${_state.value}")

        if (!isTimeout) {
            consecutiveErrorCount++
        }
        
        if (consecutiveErrorCount >= MAX_RETRY_COUNT) {
            Log.e(TAG, "[MIC_ERROR] Too many consecutive critical errors. Re-initializing...")
            recreateRecognizer(extraDelay = 5000L)
            consecutiveErrorCount = 0 
            return
        }

        when (error) {
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                Log.w(TAG, "[MIC_ERROR] Recognizer busy, resetting...")
                cancelAndRestart()
            }
            SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                // Natural silence or timeout - expected in Passive Mode
                if (activeCallbacks != null) {
                    activeCallbacks?.second?.invoke("RETRY_PROMPT")
                } else {
                    // Passive mode: keep listening without re-init
                    // Increased delay to 500ms to allow hardware reset
                    restartInternal(delay = 500L)
                }
            }
            SpeechRecognizer.ERROR_AUDIO, SpeechRecognizer.ERROR_CLIENT -> {
                Log.e(TAG, "[MIC_ERROR] Critical mic error: $error. Recreating...")
                recreateRecognizer()
            }
            else -> {
                // For other errors (Network, etc), use standard delay
                restartInternal(delay = RESTART_DELAY_MS)
            }
        }
    }

    private fun restartInternal(delay: Long = 0) {
        if (_state.value == MicState.STARTING) return
        
        Log.d(TAG, "[VOICE] RESTART_SCHEDULED in ${delay}ms")
        _state.value = MicState.RECOVERING
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({
            // Always enable partial results for faster response (Rule 11)
            startInternal(isPartial = true)
        }, delay)
    }


    private fun cancelAndRestart() {
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
            } catch (_: Exception) {}
            restoreAudioState()
            restartInternal(delay = 500)
        }
    }

    private fun recreateRecognizer(extraDelay: Long = 0L) {
        mainHandler.post {
            Log.w(TAG, "[MIC_RECREATE] Reinitializing SpeechRecognizer instance. extraDelay=$extraDelay")
            initializeRecognizer()
            restartInternal(delay = 500 + extraDelay) 
        }
    }



    fun destroy() {
        Log.i(TAG, "[MIC_DESTROY] Releasing resources")
        mainHandler.removeCallbacksAndMessages(null)
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        restoreAudioState()
        speechRecognizer = null
    }

    private fun getErrorMessage(error: Int): String {

        return when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio error"
            SpeechRecognizer.ERROR_CLIENT -> "Client error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Timeout"
            else -> "Unknown ($error)"
        }
    }
}

