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
import com.humanoidai.voice.SpeechProvider
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
 * Authoritative Speech Recognizer Manager for Aura 360°.
 * Implements a state machine to ensure stable transitions and suppresses system beeps.
 */
class SpeechRecognizerManager(
    private val context: Context,
    private val mainHandler: Handler = Handler(Looper.getMainLooper())
) : SpeechProvider {

    override val id: String = "android-speech"
    override val name: String = "Android System Speech"
    
    private var speechRecognizer: SpeechRecognizer? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    private val _state = MutableStateFlow(MicState.IDLE)
    val state: StateFlow<MicState> = _state.asStateFlow()

    private val _isListening = MutableStateFlow(value = false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _ambientNoise = MutableStateFlow(0f)
    val ambientNoise: StateFlow<Float> = _ambientNoise.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private var cumulativeTranscript = StringBuilder()
    private var lastSegmentText = ""

    private val wakeWordManager = WakeWordManager()
    private var onWakeWordDetected: (() -> Unit)? = null
    private var onSpeechStarted: (() -> Unit)? = null
    
    private var providerPartialCallback: ((String) -> Unit)? = null
    private var providerFinalCallback: ((String) -> Unit)? = null
    private var providerErrorCallback: ((String) -> Unit)? = null

    private var originalAudioMode = AudioManager.MODE_NORMAL
    private var currentLanguage = "en-US"
    private var consecutiveErrorCount = 0
    private var isContinuous = false

    companion object {
        private const val TAG = "SpeechRecognizerManager"
        private const val RESTART_DELAY_MS = 100L 
        private const val MAX_RETRY_COUNT = 5
    }

    init {
        initializeRecognizer()
    }

    override fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    private fun initializeRecognizer() {
        mainHandler.post {
            try {
                Log.d(TAG, "[VOICE_PIPELINE] MIC_INITIALIZE")
                speechRecognizer?.destroy()
            } catch (_: Exception) {}

            if (isAvailable()) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(InternalRecognitionListener())
                }
                Log.i(TAG, "[VOICE_PIPELINE] RECOGNIZER_READY")
            } else {
                Log.e(TAG, "[VOICE_PIPELINE] ERROR: RECOGNIZER_UNAVAILABLE")
                _state.value = MicState.ERROR
            }
        }
    }

    private inner class InternalRecognitionListener : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.i(TAG, "[VOICE_PIPELINE] LISTEN_READY state=${_state.value}")
            _isListening.value = true
            restoreAudioState()
            if (_state.value == MicState.STARTING || _state.value == MicState.RECOVERING) {
                _state.value = if (isContinuous) MicState.LISTENING_ACTIVE else MicState.LISTENING_PASSIVE
            }
        }

        override fun onBeginningOfSpeech() {
            Log.i(TAG, "[VOICE_PIPELINE] SPEECH_DETECTED")
            onSpeechStarted?.invoke()
        }

        private var lastRmsLogTime = 0L
        override fun onRmsChanged(rmsdB: Float) {
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f) * 100f
            _ambientNoise.value = normalized
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        private var speechEndTime = 0L

        override fun onEndOfSpeech() {
            speechEndTime = System.currentTimeMillis()
            Log.d(TAG, "[VOICE_PIPELINE] SPEECH_SEGMENT_END")
            _isListening.value = false
        }

        override fun onError(error: Int) {
            val message = getErrorMessage(error)
            Log.e(TAG, "[VOICE_PIPELINE] ERROR: $message ($error). State=${_state.value}")
            
            if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                cancelAndRestart()
                return
            }

            _isListening.value = false
            restoreAudioState()
            
            if (isContinuous && (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)) {
                // Keep listening in continuous mode even if silent
                restartInternal(delay = 100)
            } else {
                providerErrorCallback?.invoke(message)
                handleError(error)
            }
        }

        override fun onResults(results: Bundle?) {
            consecutiveErrorCount = 0
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.getOrNull(0) ?: ""
            
            if (text.isNotEmpty()) {
                if (isContinuous) {
                    cumulativeTranscript.append(text).append(" ")
                    val full = cumulativeTranscript.toString().trim()
                    Log.i(TAG, "[VOICE_PIPELINE] SEGMENT_FINAL: \"$text\" -> FULL: \"$full\"")
                    _partialTranscript.value = full
                    providerPartialCallback?.invoke(full)
                    
                    // Immediately restart for continuous flow
                    restartInternal(delay = 50) 
                } else {
                    Log.i(TAG, "[VOICE_PIPELINE] FINAL_TRANSCRIPT: \"$text\"")
                    _partialTranscript.value = text
                    _state.value = MicState.PROCESSING
                    restoreAudioState()
                    
                    if (onWakeWordDetected != null && wakeWordManager.checkText(text)) {
                        onWakeWordDetected?.invoke()
                    } else {
                        providerFinalCallback?.invoke(text)
                    }
                }
            } else if (isContinuous) {
                restartInternal(delay = 100)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.getOrNull(0) ?: ""
            if (text.isNotEmpty()) {
                val display = if (isContinuous) {
                    val current = cumulativeTranscript.toString() + text
                    current.trim()
                } else text
                
                Log.v(TAG, "[VOICE_PIPELINE] PARTIAL: \"$display\"")
                _partialTranscript.value = display
                providerPartialCallback?.invoke(display)
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
        isContinuous = false
        cumulativeTranscript.setLength(0)
        startInternal(isPartial = true)
    }

    override fun startListening(
        onPartialResult: (String) -> Unit,
        onFinalResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (_state.value == MicState.LISTENING_ACTIVE) return

        Log.i(TAG, "[VOICE] START_ACTIVE_CONTINUOUS_REQUESTED")
        onWakeWordDetected = null
        isContinuous = true
        cumulativeTranscript.setLength(0)
        
        providerPartialCallback = onPartialResult
        providerFinalCallback = onFinalResult
        providerErrorCallback = onError
        
        startInternal(isPartial = true)
    }

    private fun startInternal(isPartial: Boolean) {
        mainHandler.post {
            val currentState = _state.value
            if (currentState == MicState.STARTING || currentState == MicState.STOPPING) {
                 Log.w(TAG, "[VOICE_PIPELINE] REJECTED_START: State is $currentState")
                 return@post
            }

            Log.i(TAG, "[VOICE_PIPELINE] LISTEN_START: isPartial=$isPartial continuous=$isContinuous")
            _state.value = MicState.STARTING
            prepareAudioState() 
            
            try {
                speechRecognizer?.cancel()
            } catch (_: Exception) {}

            mainHandler.postDelayed({
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguage)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, isPartial)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    
                    // Increased sensitivity for continuous speech
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 5000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                    
                    if (isContinuous) {
                        putExtra("android.speech.extra.DICTATION_MODE", true)
                    }
                }
                
                try {
                    speechRecognizer?.startListening(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "[VOICE_PIPELINE] ERROR: START_FAILED ${e.message}")
                    _state.value = MicState.ERROR
                    recreateRecognizer()
                }
            }, 300L)
        }
    }

    override fun stopListening() {
        val oldState = _state.value
        if (oldState == MicState.IDLE || oldState == MicState.STOPPING) return
        
        Log.i(TAG, "[VOICE] STOP_REQUESTED")
        val finalResult = cumulativeTranscript.toString().trim()
        if (isContinuous && finalResult.isNotEmpty()) {
            providerFinalCallback?.invoke(finalResult)
        }
        
        isContinuous = false
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
        
        Log.w(TAG, "[VOICE_PIPELINE] ERROR_HANDLING code=$error. State=${_state.value}")

        if (!isTimeout) {
            consecutiveErrorCount++
        }
        
        if (consecutiveErrorCount >= MAX_RETRY_COUNT) {
            Log.e(TAG, "[VOICE_PIPELINE] CRITICAL_FAILURE: Too many consecutive errors. Re-initializing...")
            recreateRecognizer(extraDelay = 5000L)
            consecutiveErrorCount = 0 
            return
        }

        when (error) {
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                Log.w(TAG, "[VOICE_PIPELINE] RECOVERING: Recognizer busy, resetting...")
                cancelAndRestart()
            }
            SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                // Natural silence or timeout - expected in Passive Mode
                if (providerFinalCallback != null) {
                    providerFinalCallback?.invoke("RETRY_PROMPT")
                } else {
                    restartInternal(delay = 500L)
                }
            }
            SpeechRecognizer.ERROR_AUDIO, SpeechRecognizer.ERROR_CLIENT -> {
                Log.e(TAG, "[VOICE_PIPELINE] CRITICAL_RECOVERY: Recreating recognizer...")
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



    override fun destroy() {
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

