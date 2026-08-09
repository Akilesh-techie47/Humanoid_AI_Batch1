package com.humanoidai.embodiment.phone

import android.content.Context
import android.util.Log
import com.humanoidai.embodiment.*
import com.humanoidai.voice.VoiceEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PhoneEmbodiment(
    private val context: Context,
    private val voiceEngine: VoiceEngine
) : Embodiment {

    override val profile = EmbodimentProfile(
        id = "phone_primary",
        type = EmbodimentType.PHONE,
        capabilities = setOf(
            Capability.VISION,
            Capability.AUDIO_INPUT,
            Capability.AUDIO_OUTPUT,
            Capability.DISPLAY,
            Capability.HAPTICS,
            Capability.ILLUMINATION,
            Capability.GPS,
            Capability.CONNECTIVITY_WIFI,
            Capability.CONNECTIVITY_BLUETOOTH
        ),
        manufacturer = android.os.Build.MANUFACTURER,
        model = android.os.Build.MODEL
    )

    override val vision = object : VisionSensor {
        private val _status = MutableStateFlow(HardwareStatus.HEALTHY)
        override val status: StateFlow<HardwareStatus> = _status.asStateFlow()
        override fun startCapture() { Log.d("PhoneVision", "Starting capture") }
        override fun stopCapture() { Log.d("PhoneVision", "Stopping capture") }
        override fun getLatestFrame(): Any? = null
    }

    override val audio = object : AudioSensor {
        private val _status = MutableStateFlow(HardwareStatus.HEALTHY)
        override val status: StateFlow<HardwareStatus> = _status.asStateFlow()
        override fun startListening() { Log.d("PhoneAudio", "Starting listening") }
        override fun stopListening() { Log.d("PhoneAudio", "Stopping listening") }
        override fun getIntensity(): Float = 0.5f
    }

    override val motion = object : MotionSensor {
        override val status = MutableStateFlow(HardwareStatus.HEALTHY).asStateFlow()
        override fun getAcceleration() = floatArrayOf(0f, 0f, 9.8f)
        override fun getRotation() = floatArrayOf(0f, 0f, 0f)
    }

    override val display = object : DisplayActuator {
        override val status = MutableStateFlow(HardwareStatus.HEALTHY).asStateFlow()
        override fun showOverlay(id: String, content: Any?) { Log.d("PhoneDisplay", "Show: $id") }
        override fun hideOverlay(id: String) { Log.d("PhoneDisplay", "Hide: $id") }
        override fun setBrightness(level: Float) { Log.d("PhoneDisplay", "Brightness: $level") }
    }

    override val speaker = object : SpeakerActuator {
        override val status = MutableStateFlow(HardwareStatus.HEALTHY).asStateFlow()
        override fun speak(text: String, priority: Int, onComplete: () -> Unit) {
            voiceEngine.speak(text, onComplete = onComplete)
        }
        override fun stop() { voiceEngine.stop() }
        override fun setVolume(level: Float) { /* TODO */ }
    }

    override val physical = null // Phone has no movement actuators

    override fun initialize() {
        Log.i("PhoneEmbodiment", "Phone embodiment initialized")
    }

    override fun shutdown() {
        Log.i("PhoneEmbodiment", "Phone embodiment shutdown")
    }
}
