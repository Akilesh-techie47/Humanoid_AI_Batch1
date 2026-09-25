package com.humanoidai.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.util.Log
import android.graphics.Matrix
import android.graphics.Bitmap
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.humanoidai.hearing.SpeechRecognizerManager
import com.humanoidai.ml.FacePreprocessor
import com.humanoidai.ml.FaceEmbeddingHelper
import com.humanoidai.ml.FaceRecognitionManager
import com.humanoidai.ml.FisheyeCorrector
import com.humanoidai.ml.OwnerEnrollmentManager
import com.humanoidai.vision.FrameEnhancer
import com.humanoidai.navigation.NavRoutes
import com.humanoidai.platform.capability.CameraCapability
import com.humanoidai.platform.capability.VoiceCapability
import com.humanoidai.platform.manager.AuraCapabilityManager
import com.humanoidai.platform.model.Capability
import com.humanoidai.ui.components.ArmsunFooter
import com.humanoidai.ui.components.WithCameraPermission
import com.humanoidai.ui.components.WithMicrophonePermission
import com.humanoidai.ui.theme.*
import com.humanoidai.voice.VoiceEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.util.*
import java.util.concurrent.Executors
import kotlin.math.abs

enum class OwnerEnrollStep { WELCOME, FACE_SCANNING, VOICE_SCANNING, PASSWORD_SETUP, PROCESSING, SUCCESS }
enum class FacePose { FRONT, LEFT, RIGHT, UP, DOWN, NATURAL }

@Composable
fun OwnerEnrollmentScreen(
    navController: NavController,
    ownerManager: OwnerEnrollmentManager,
    recognitionManager: FaceRecognitionManager,
    capabilityManager: AuraCapabilityManager,
    microphoneManager: SpeechRecognizerManager,
    voiceEngine: VoiceEngine,
    isUpdateMode: Boolean = false,
    updateType: String? = null, // "face" or "voice"
    onEnrollmentComplete: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val appId = "aura.system.enrollment"
    
    // Initial step based on update mode
    val initialStep = when {
        isUpdateMode && updateType == "face" -> OwnerEnrollStep.FACE_SCANNING
        isUpdateMode && updateType == "voice" -> OwnerEnrollStep.VOICE_SCANNING
        else -> OwnerEnrollStep.WELCOME
    }
    
    var step by remember { mutableStateOf(initialStep) }
    var ownerName by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        if (isUpdateMode) {
            ownerName = ownerManager.getOwnerName()
        }
    }

    var ownerPassword by remember { mutableStateOf("") }
    val isListening by microphoneManager.isListening.collectAsState()

    var currentPose by remember { mutableStateOf(FacePose.FRONT) }
    var poseSamplesCaptured by remember { mutableIntStateOf(0) }
    val SAMPLES_PER_POSE = 10

    LaunchedEffect(step, currentPose) {
        if (step == OwnerEnrollStep.FACE_SCANNING) {
            val poseInstruction = when(currentPose) {
                FacePose.FRONT -> "Look straight at the camera."
                FacePose.LEFT -> "Turn your head slightly to the left."
                FacePose.RIGHT -> "Turn your head slightly to the right."
                FacePose.UP -> "Look slightly up."
                FacePose.DOWN -> "Look slightly down."
                FacePose.NATURAL -> "Now, just look natural."
            }
            voiceEngine.speak(poseInstruction)
        } else {
            when(step) {
                OwnerEnrollStep.WELCOME -> voiceEngine.speak("Welcome. Let's build your profile. Enter your name first.")
                OwnerEnrollStep.VOICE_SCANNING -> voiceEngine.speak("Say Aura.")
                OwnerEnrollStep.PASSWORD_SETUP -> voiceEngine.speak("Voice command verified. Finally, set a secure access password.")
                OwnerEnrollStep.PROCESSING -> voiceEngine.speak("Fusing your biometric data.")
                OwnerEnrollStep.SUCCESS -> voiceEngine.speak("Setup complete. Welcome, $ownerName.")
                else -> {}
            }
        }
    }
    
    var faceProgress by remember { mutableFloatStateOf(0f) }
    var voiceCount by remember { mutableIntStateOf(0) }
    val capturedEmbeddings = remember { mutableListOf<FloatArray>() }
    var lastCaptureTime by remember { mutableLongStateOf(0L) }

    DisposableEffect(Unit) {
        onDispose {
            capabilityManager.releaseAll(appId)
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AnimatedContent(
                targetState = step,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "step"
            ) { currentStep ->
                when (currentStep) {
                    OwnerEnrollStep.WELCOME -> WelcomeStep(
                        name = ownerName,
                        onNameChange = { ownerName = it },
                        onStart = { if(ownerName.isNotBlank()) step = OwnerEnrollStep.FACE_SCANNING },
                        isListening = isListening,
                        onMicClick = {
                            if (isListening) microphoneManager.stopListening()
                            else microphoneManager.startListening(
                                onPartialResult = { /* display logic if needed */ },
                                onFinalResult = { ownerName = it },
                                onError = { /* handle error */ }
                            )
                        }
                    )
                    OwnerEnrollStep.FACE_SCANNING -> FaceScanningStep(
                        navController = navController,
                        capabilityManager = capabilityManager,
                        appId = appId,
                        faceProgress = faceProgress,
                        sampleCount = capturedEmbeddings.size,
                        lastCaptureTime = lastCaptureTime,
                        onFrameProcessed = { embedding ->
                            val now = System.currentTimeMillis()
                            if (now - lastCaptureTime > 250) {
                                var isDiverse = true
                                if (capturedEmbeddings.isNotEmpty()) {
                                    for (existing in capturedEmbeddings.takeLast(10)) {
                                        var dot = 0f
                                        for (i in existing.indices) {
                                            dot += existing[i] * embedding[i]
                                        }
                                        if (dot > 0.96f) {
                                            isDiverse = false
                                            break
                                        }
                                    }
                                }

                                if (isDiverse) {
                                    capturedEmbeddings.add(embedding)
                                    faceProgress = capturedEmbeddings.size / 60f
                                    lastCaptureTime = now
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                                    if (capturedEmbeddings.size >= 60) {
                                        step = OwnerEnrollStep.VOICE_SCANNING
                                    }
                                }
                            }
                        }
                    )
                    OwnerEnrollStep.VOICE_SCANNING -> VoiceScanningStep(
                        voiceCount = voiceCount,
                        capabilityManager = capabilityManager,
                        appId = appId,
                        onVoiceSampleDetected = {
                            voiceCount++
                            if (voiceCount >= 3) {
                                if (isUpdateMode) step = OwnerEnrollStep.PROCESSING
                                else step = OwnerEnrollStep.PASSWORD_SETUP
                            }
                        },
                        onSkip = {
                            if (isUpdateMode) navController.popBackStack()
                            else step = OwnerEnrollStep.PASSWORD_SETUP
                        }
                    )
                    OwnerEnrollStep.PASSWORD_SETUP -> PasswordSetupStep(
                        password = ownerPassword,
                        onPasswordChange = { ownerPassword = it },
                        onComplete = { if(ownerPassword.length >= 4) step = OwnerEnrollStep.PROCESSING }
                    )
                    OwnerEnrollStep.PROCESSING -> ProcessingStep {
                        scope.launch {
                            if (isUpdateMode) {
                                if (updateType == "face") {
                                    // Just update embeddings for the existing owner
                                    ownerManager.enrollOwner(ownerName, capturedEmbeddings, 0.98f, voiceEnrolled = ownerManager.isVoiceEnrolled())
                                    ownerManager.getMasterEmbedding()?.let { recognitionManager.registerFace(ownerName, it) }
                                } else if (updateType == "voice") {
                                    ownerManager.markVoiceEnrolled(true)
                                }
                                withContext(Dispatchers.Main) {
                                    navController.popBackStack()
                                }
                            } else {
                                ownerManager.enrollOwner(ownerName, capturedEmbeddings, 0.98f, voiceEnrolled = voiceCount >= 3)
                                ownerManager.setMasterPassword(ownerPassword)
                                ownerManager.getMasterEmbedding()?.let { recognitionManager.registerFace(ownerName, it) }
                                withContext(Dispatchers.Main) {
                                    step = OwnerEnrollStep.SUCCESS
                                }
                            }
                        }
                    }
                    OwnerEnrollStep.SUCCESS -> SuccessStep(ownerName) {
                        onEnrollmentComplete(ownerName)
                        navController.navigate(NavRoutes.ENVIRONMENT) { 
                            popUpTo(NavRoutes.OWNER_ENROLLMENT) { inclusive = true } 
                        }
                    }
                }
            }
            
            ArmsunFooter(modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun StepContainer(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            title, 
            color = MaterialTheme.colorScheme.primary, 
            fontWeight = FontWeight.Black, 
            fontSize = 22.sp, 
            letterSpacing = 1.sp
        )
        Text(
            subtitle, 
            color = MaterialTheme.colorScheme.onSurfaceVariant, 
            fontSize = 13.sp, 
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(48.dp))
        content()
    }
}

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun FaceScanningStep(
    navController: NavController,
    capabilityManager: AuraCapabilityManager,
    appId: String,
    faceProgress: Float,
    sampleCount: Int,
    lastCaptureTime: Long,
    onFrameProcessed: (FloatArray) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val embeddingHelper = remember { FaceEmbeddingHelper(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val detector = remember {
        FaceDetection.getClient(FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build())
    }

    var faceDetected by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Camera ready") }

    val infiniteTransition = rememberInfiniteTransition(label = "scan_pulse")
    val scanAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "scan_alpha"
    )

    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
            embeddingHelper.close()
            detector.close()
        }
    }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val circleSize = minOf(screenWidth * 0.85f, screenHeight * 0.42f).coerceAtLeast(260.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
            // Level 1 — Title
            Text(
                "Face Biometrics Setup",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center
            )

            // Level 2 — Short instruction
            Text(
                "Look naturally at the camera while we collect samples",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))

            // Level 3 — Large circular camera (dominates screen)
            Box(
                modifier = Modifier
                    .size(circleSize)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                // Pulsing Scan Ring
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(3.dp, MaterialTheme.colorScheme.primary.copy(alpha = scanAlpha), CircleShape)
                )

                // Progress Indicator Ring
                CircularProgressIndicator(
                    progress = { faceProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = SuccessGreen,
                    strokeWidth = 4.dp,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                )

                WithCameraPermission {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .clip(CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                PreviewView(ctx).apply {
                                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                }.also { previewView ->
                                    val future = ProcessCameraProvider.getInstance(ctx)
                                    future.addListener({
                                        val cameraProvider = future.get()
                                        val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                                        val imageAnalysis = ImageAnalysis.Builder()
                                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                                            .build()
                                        imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                            val mediaImage = imageProxy.image
                                            if (mediaImage != null) {
                                                val rotation = imageProxy.imageInfo.rotationDegrees
                                                val inputImage = InputImage.fromMediaImage(mediaImage, rotation)
                                                detector.process(inputImage)
                                                    .addOnSuccessListener { faces ->
                                                        if (faces.size == 1) {
                                                            faceDetected = true
                                                            statusText = if (sampleCount < 60) "Capturing samples..." else "Complete"
                                                            val face = faces[0]
                                                            try {
                                                                val bitmap = imageProxy.toBitmap()
                                                                val isolated = FacePreprocessor.alignAndIsolate(bitmap, face)
                                                                val embedding = embeddingHelper.getEmbedding(isolated)
                                                                onFrameProcessed(embedding)
                                                            } catch (e: Exception) {
                                                                Log.e("OwnerEnrollment", "Error: ${e.message}")
                                                            }
                                                        } else if (faces.size > 1) {
                                                            faceDetected = false
                                                            statusText = "Multiple faces detected"
                                                        } else {
                                                            faceDetected = false
                                                            statusText = "No face detected"
                                                        }
                                                    }
                                                    .addOnCompleteListener { imageProxy.close() }
                                            } else {
                                                imageProxy.close()
                                            }
                                        }
                                        try {
                                            cameraProvider.unbindAll()
                                            val selector = try {
                                                CameraSelector.DEFAULT_FRONT_CAMERA
                                            } catch (_: Exception) {
                                                CameraSelector.DEFAULT_BACK_CAMERA
                                            }
                                            cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, imageAnalysis)
                                            Log.i("OwnerEnrollmentScreen", "Camera bound successfully")
                                        } catch (e: Exception) {
                                            Log.e("OwnerEnrollmentScreen", "Failed to bind camera", e)
                                        }
                                    }, ContextCompat.getMainExecutor(ctx))
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Level 4 — Status & Guidance
            Spacer(Modifier.height(4.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = statusText.uppercase(),
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (faceDetected) SuccessGreen else MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Hold naturally. We are building your profile.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Level 5 — Progress & Action / Cancel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Samples: $sampleCount / 60",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                TextButton(onClick = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }) {
                    Text("Cancel", color = Color.Red.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

@Composable
fun VoiceScanningStep(
    voiceCount: Int,
    capabilityManager: AuraCapabilityManager,
    appId: String,
    onVoiceSampleDetected: () -> Unit,
    onSkip: () -> Unit
) {
    var voiceCap by remember { mutableStateOf<VoiceCapability?>(null) }
    
    val isListening by (voiceCap?.isListening ?: MutableStateFlow(false)).collectAsState()
    
    val voicePulse by animateFloatAsState(0.2f, label = "pulse") // Simplified animation
    var lastVoiceTriggerTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        val acquired = capabilityManager.acquire(appId, Capability.VOICE)
        if (acquired) {
            voiceCap = capabilityManager.getCapability<VoiceCapability>(Capability.VOICE)
        }
    }

    DisposableEffect(Unit) { 
        onDispose { 
            capabilityManager.release(appId, Capability.VOICE)
        } 
    }

    val startScanning = {
        voiceCap?.startListening()
    }
    
    // Wire partial transcript for trigger
    val transcript by (voiceCap?.transcript ?: MutableStateFlow("")).collectAsState()
    LaunchedEffect(transcript) {
        val phrase = transcript.lowercase()
        if (phrase.contains("aura")) {
            val now = System.currentTimeMillis()
            if (now - lastVoiceTriggerTime > 1500) {
                lastVoiceTriggerTime = now
                onVoiceSampleDetected()
            }
        }
    }

    StepContainer(
        title = "Voice Trigger Verification",
        subtitle = "Say \"Aura\" three times to verify trigger"
    ) {
        WithMicrophonePermission {
            LaunchedEffect(voiceCap) { 
                if (voiceCap != null) startScanning() 
            }
            
            Box(contentAlignment = Alignment.Center) {
                FrequencyPulseWave(amplitude = voicePulse, color = MaterialTheme.colorScheme.primary)
                CircularProgressIndicator(
                    progress = { voiceCount.toFloat() / 3f },
                    modifier = Modifier.size(200.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 6.dp,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                )
                IconButton(
                    onClick = { if (isListening) voiceCap?.stopListening() else startScanning() },
                    modifier = Modifier.size(80.dp).background(if (isListening) ErrorRed.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape).border(2.dp, if (isListening) ErrorRed else MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(if (isListening) Icons.Default.StopCircle else Icons.Default.Mic, null, tint = if (isListening) ErrorRed else MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                }
            }
            Spacer(Modifier.height(40.dp))
            Text("SAMPLES: $voiceCount / 3", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            
            Spacer(Modifier.height(24.dp))
            
            TextButton(onClick = onSkip) {
                Text("Skip for now", color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun FrequencyPulseWave(amplitude: Float, color: Color) {
    Canvas(modifier = Modifier.size(280.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val innerRadius = 100.dp.toPx()
        val maxExtra = 50.dp.toPx()
        val barCount = 72
        for (i in 0 until barCount) {
            val angle = Math.toRadians((i * 360f / barCount).toDouble())
            val variation = 0.6f + 0.4f * abs(Math.sin(i.toDouble() * 0.3)).toFloat()
            val length = innerRadius + (amplitude * maxExtra * variation)
            val start = Offset((center.x + innerRadius * Math.cos(angle)).toFloat(), (center.y + innerRadius * Math.sin(angle)).toFloat())
            val end = Offset((center.x + length * Math.cos(angle)).toFloat(), (center.y + length * Math.sin(angle)).toFloat())
            drawLine(color = color.copy(alpha = 0.4f), start = start, end = end, strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
            drawLine(color = color, start = start, end = end, strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
        }
    }
}

@Composable
fun WelcomeStep(name: String, onNameChange: (String) -> Unit, onStart: () -> Unit, isListening: Boolean, onMicClick: () -> Unit) {
    StepContainer(title = "Unified Enrollment", subtitle = "Register your identity across multiple modalities") {
        AuraTextField(
            value = name, onValueChange = onNameChange,
            label = "Enter your name",
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = { IconButton(onClick = onMicClick) { Icon(if (isListening) Icons.Default.MicOff else Icons.Default.Mic, null, tint = if (isListening) ErrorRed else MaterialTheme.colorScheme.primary) } }
        )
        Spacer(Modifier.height(24.dp))
        AuraButton(onClick = onStart, enabled = name.isNotBlank(), text = "START SCANNING")
    }
}

@Composable
fun PasswordSetupStep(
    password: String,
    onPasswordChange: (String) -> Unit,
    onComplete: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }
    
    StepContainer(
        title = "System Password",
        subtitle = "Set a secure password for emergency access"
    ) {
        AuraTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = "Master Password",
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        "Toggle Password Visibility",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            modifier = Modifier.width(280.dp)
        )
        
        Spacer(Modifier.height(32.dp))
        
        AuraButton(
            text = "Finalize Setup",
            onClick = onComplete,
            enabled = password.length >= 6,
            modifier = Modifier.width(280.dp)
        )
    }
}

@Composable
fun ProcessingStep(onComplete: () -> Unit) {
    LaunchedEffect(Unit) { delay(2000); onComplete() }
    StepContainer(title = "Synthesizing", subtitle = "Generating secure biometric hash") {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun SuccessStep(name: String, onDone: () -> Unit) {
    StepContainer(title = "Securely Enrolled", subtitle = "Welcome, $name. Your AI companion is ready.") {
        Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(80.dp))
        Spacer(Modifier.height(48.dp))
        AuraButton(onClick = onDone, text = "LAUNCH AURA 360")
    }
}
