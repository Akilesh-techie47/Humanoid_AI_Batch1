package com.humanoidai.hearing

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import android.os.Handler
import android.os.Looper

/**
 * Manages continuous listening and wake word detection (simulated via voice activity).
 */
class MicrophoneManager(private val context: Context) {

    private var speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    private val wakeWordManager = WakeWordManager()
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _lastRecognizedText = MutableStateFlow("")
    val lastRecognizedText: StateFlow<String> = _lastRecognizedText.asStateFlow()

    private var isPassiveMode = false
    private var onWakeWordDetected: (() -> Unit)? = null

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) { _isListening.value = true }
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() { _isListening.value = false }
        override fun onError(error: Int) { 
            _isListening.value = false
            if (isPassiveMode) {
                // Schedule restart on main thread to avoid loops
                mainHandler.postDelayed({
                    if (isPassiveMode) {
                        restartPassiveListening()
                    }
                }, 1000)
            }
        }
        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val text = matches[0]
                _lastRecognizedText.value = text
                
                if (isPassiveMode) {
                    if (wakeWordManager.checkText(text)) {
                        isPassiveMode = false
                        onWakeWordDetected?.invoke()
                    } else {
                        restartPassiveListening()
                    }
                }
            } else if (isPassiveMode) {
                restartPassiveListening()
            }
        }
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    init {
        speechRecognizer.setRecognitionListener(recognitionListener)
    }

    private fun restartPassiveListening() {
        if (!isPassiveMode) return
        
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({
            if (isPassiveMode) {
                try {
                    speechRecognizer.cancel()
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    }
                    speechRecognizer.startListening(intent)
                } catch (e: Exception) {
                    recreateRecognizer()
                }
            }
        }, 2000)
    }

    private fun recreateRecognizer() {
        try {
            speechRecognizer.destroy()
        } catch (e: Exception) {}
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer.setRecognitionListener(recognitionListener)
        startPassiveListening(onWakeWordDetected)
    }

    /**
     * Starts continuous listening for the wake word.
     */
    fun startPassiveListening(onDetected: (() -> Unit)?) {
        isPassiveMode = true
        onWakeWordDetected = onDetected
        mainHandler.post {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            speechRecognizer.startListening(intent)
        }
    }

    fun startListening(onPartialResult: (String) -> Unit = {}, onFinalResult: (String) -> Unit = {}) {
        isPassiveMode = false
        mainHandler.post {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            
            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { _isListening.value = true }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { _isListening.value = false }
                override fun onError(error: Int) { _isListening.value = false }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val text = matches[0]
                        _lastRecognizedText.value = text
                        onFinalResult(text)
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        onPartialResult(matches[0])
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            speechRecognizer.startListening(intent)
        }
    }

    fun stopListening() {
        isPassiveMode = false
        mainHandler.post {
            speechRecognizer.stopListening()
            // Reset listener to default
            speechRecognizer.setRecognitionListener(recognitionListener)
        }
    }

    fun destroy() {
        speechRecognizer.destroy()
    }
}
