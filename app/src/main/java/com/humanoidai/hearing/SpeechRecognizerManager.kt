package com.humanoidai.hearing

import android.content.Context
import android.content.Intent
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

/**
 * Robust Speech Recognizer Manager for Humanoid AI.
 * Handles continuous listening, error recovery, and detailed logging.
 */
class SpeechRecognizerManager(private val context: Context) {

    private val TAG = "SpeechRecognizerManager"
    
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _ambientNoise = MutableStateFlow(0f)
    val ambientNoise: StateFlow<Float> = _ambientNoise.asStateFlow()

    private val _lastRecognizedText = MutableStateFlow("")
    val lastRecognizedText: StateFlow<String> = _lastRecognizedText.asStateFlow()

    private var isPassiveMode = false
    private val wakeWordManager = WakeWordManager()
    private var onWakeWordDetected: (() -> Unit)? = null

    init {
        initializeRecognizer()
    }

    private fun initializeRecognizer() {
        mainHandler.post {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createDefaultListener())
                }
                Log.d(TAG, "SpeechRecognizer initialized successfully")
            } else {
                Log.e(TAG, "Speech recognition is NOT available on this device")
            }
        }
    }

    private fun createDefaultListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "onReadyForSpeech")
            _isListening.value = true
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "onBeginningOfSpeech")
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Robust normalization for visualizers
            // rmsdB typically ranges from -2.0 to 10.0+
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f) * 100f
            _ambientNoise.value = normalized
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            Log.d(TAG, "onBufferReceived: ${buffer?.size} bytes")
        }

        override fun onEndOfSpeech() {
            Log.d(TAG, "onEndOfSpeech")
            _isListening.value = false
        }

        override fun onError(error: Int) {
            val message = getErrorMessage(error)
            Log.e(TAG, "onError: $message ($error)")
            _isListening.value = false
            
            handleError(error)
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.getOrNull(0) ?: ""
            Log.d(TAG, "onResults: \"$text\"")
            
            _lastRecognizedText.value = text
            processResults(text)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.getOrNull(0) ?: ""
            Log.v(TAG, "onPartialResults: \"$text\"")
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            Log.d(TAG, "onEvent: $eventType")
        }
    }

    private fun processResults(text: String) {
        if (isPassiveMode) {
            if (wakeWordManager.checkText(text)) {
                Log.i(TAG, "Wake word detected!")
                isPassiveMode = false
                onWakeWordDetected?.invoke()
            } else {
                Log.d(TAG, "No wake word, returning to passive listening")
                restartListening()
            }
        }
    }

    private fun handleError(error: Int) {
        when (error) {
            SpeechRecognizer.ERROR_NO_MATCH, 
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                if (isPassiveMode) {
                    Log.d(TAG, "Timeout or no match in passive mode, restarting...")
                    restartListening()
                }
            }
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                Log.w(TAG, "Recognizer busy, canceling and restarting...")
                cancelAndRestart()
            }
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                Log.e(TAG, "Insufficient permissions for speech recognition")
            }
            else -> {
                if (isPassiveMode) {
                    Log.d(TAG, "Recoverable error in passive mode, restarting in 1s...")
                    mainHandler.postDelayed({ restartListening() }, 1000)
                }
            }
        }
    }

    fun startPassiveListening(onDetected: (() -> Unit)?) {
        Log.i(TAG, "Starting passive listening (waiting for wake word)")
        isPassiveMode = true
        onWakeWordDetected = onDetected
        startInternal(isPartial = false)
    }

    fun startListening(onPartialResult: (String) -> Unit = {}, onFinalResult: (String) -> Unit = {}) {
        Log.i(TAG, "Starting active listening")
        isPassiveMode = false
        
        mainHandler.post {
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                // ... wrap the results and partial results to the lambdas
                override fun onReadyForSpeech(params: Bundle?) { _isListening.value = true }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { _isListening.value = false }
                override fun onError(error: Int) { 
                    Log.e(TAG, "Active listening error: ${getErrorMessage(error)}")
                    _isListening.value = false 
                    onFinalResult("")
                }
                override fun onResults(results: Bundle?) {
                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.getOrNull(0) ?: ""
                    Log.d(TAG, "Active results: \"$text\"")
                    onFinalResult(text)
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.getOrNull(0) ?: ""
                    onPartialResult(text)
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            startInternal(isPartial = true)
        }
    }

    private var currentLanguage = "en-US"

    fun setLanguage(langCode: String) {
        currentLanguage = when(langCode) {
            "ta" -> "ta-IN"
            "en" -> "en-US"
            else -> "en-US" 
        }
        Log.d(TAG, "Recognizer language set to: $currentLanguage")
    }

    fun setCustomWakeWord(name: String) {
        wakeWordManager.setCustomWakeWord(name)
    }

    private fun startInternal(isPartial: Boolean) {
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
            } catch (e: Exception) {}

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguage)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, currentLanguage)
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, currentLanguage)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, isPartial)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }
            try {
                speechRecognizer?.startListening(intent)
                Log.i(TAG, ">>> Recognizer listening started ($currentLanguage, isPartial=$isPartial)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start listening: ${e.message}")
                recreateRecognizer()
            }
        }
    }

    fun stopListening() {
        Log.d(TAG, "stopListening() called")
        isPassiveMode = false
        mainHandler.post {
            speechRecognizer?.stopListening()
            speechRecognizer?.setRecognitionListener(createDefaultListener())
        }
    }

    private fun restartListening() {
        if (!isPassiveMode) return
        mainHandler.post {
            startInternal(isPartial = false)
        }
    }

    private fun cancelAndRestart() {
        mainHandler.post {
            speechRecognizer?.cancel()
            mainHandler.postDelayed({ restartListening() }, 500)
        }
    }

    fun destroy() {
        Log.d(TAG, "destroy() called")
        mainHandler.post {
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
    }

    private fun recreateRecognizer() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                Log.e(TAG, "Error destroying recognizer: ${e.message}")
            }
            initializeRecognizer()
            if (isPassiveMode) {
                startPassiveListening(onWakeWordDetected)
            }
        }
    }

    private fun getErrorMessage(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
            SpeechRecognizer.ERROR_SERVER -> "Error from server"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Unknown error"
        }
    }
}
