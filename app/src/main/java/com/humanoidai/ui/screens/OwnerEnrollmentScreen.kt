package com.humanoidai.ui.screens

import android.annotation.SuppressLint
import android.util.Log
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.humanoidai.hearing.SpeechRecognizerManager
import com.humanoidai.ml.FaceEmbeddingHelper
import com.humanoidai.ml.FacePreprocessor
import com.humanoidai.ml.FaceRecognitionManager
import com.humanoidai.ml.OwnerEnrollmentManager
import com.humanoidai.navigation.NavRoutes
import com.humanoidai.platform.capability.VoiceCapability
import com.humanoidai.platform.manager.AuraCapabilityManager
import com.humanoidai.platform.model.Capability
import com.humanoidai.ui.components.WithCameraPermission
import com.humanoidai.ui.components.WithMicrophonePermission
import com.humanoidai.ui.theme.*
import com.humanoidai.voice.VoiceEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
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

    LaunchedEffect(step, currentPose) {
        if (step == OwnerEnrollStep.FACE_SCANNING) {
            val poseInstruction = when (currentPose) {
                FacePose.FRONT -> "Look straight at the camera."
                FacePose.LEFT -> "Turn your head slightly to the left."
                FacePose.RIGHT -> "Turn your head slightly to the right."
                FacePose.UP -> "Look slightly up."
                FacePose.DOWN -> "Look slightly down."
                FacePose.NATURAL -> "Now, just look natural."
            }
            voiceEngine.speak(poseInstruction)
        } else {
            when (step) {
                OwnerEnrollStep.WELCOME -> voiceEngine.speak("Welcome. Let's set up your profile. Enter your name to begin.")
                OwnerEnrollStep.VOICE_SCANNING -> voiceEngine.speak("Say Aura.")
                OwnerEnrollStep.PASSWORD_SETUP -> voiceEngine.speak("Set a secure access password for backup.")
                OwnerEnrollStep.PROCESSING -> voiceEngine.speak("Securing your biometric data.")
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            AnimatedContent(
                targetState = step,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(250)) },
                label = "enroll_step"
            ) { currentStep ->
                when (currentStep) {
                    OwnerEnrollStep.WELCOME -> WelcomeStep(
                        name = ownerName,
                        onNameChange = { ownerName = it },
                        onStart = { if (ownerName.isNotBlank()) step = OwnerEnrollStep.FACE_SCANNING },
                        isListening = isListening,
                        onMicClick = {
                            if (isListening) microphoneManager.stopListening()
                            else microphoneManager.startListening(
                                onPartialResult = {},
                                onFinalResult = { ownerName = it },
                                onError = {}
                            )
                        }
                    )
                    OwnerEnrollStep.FACE_SCANNING -> FaceScanningStep(
                        navController = navController,
                        faceProgress = faceProgress,
                        sampleCount = capturedEmbeddings.size,
                        currentPose = currentPose,
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

                                    // Dynamic pose rotation for full facial coverage
                                    val count = capturedEmbeddings.size
                                    currentPose = when {
                                        count < 12 -> FacePose.FRONT
                                        count < 24 -> FacePose.LEFT
                                        count < 36 -> FacePose.RIGHT
                                        count < 46 -> FacePose.UP
                                        count < 54 -> FacePose.DOWN
                                        else -> FacePose.NATURAL
                                    }

                                    if (capturedEmbeddings.size >= 60) {
                                        step = OwnerEnrollStep.VOICE_SCANNING
                                    }
                                }
                            }
                        },
                        onSimulateScan = {
                            capturedEmbeddings.clear()
                            repeat(60) {
                                capturedEmbeddings.add(FloatArray(192) { 0.1f })
                            }
                            faceProgress = 1f
                            if (isUpdateMode) step = OwnerEnrollStep.PROCESSING
                            else step = OwnerEnrollStep.VOICE_SCANNING
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
                        onComplete = {
                            if (ownerPassword.length >= 4) step = OwnerEnrollStep.PROCESSING
                        }
                    )
                    OwnerEnrollStep.PROCESSING -> ProcessingStep {
                        scope.launch {
                            if (isUpdateMode) {
                                if (updateType == "face") {
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
        }
    }
}

// -----------------------------------------------------------------
// Step 1: Welcome & Name Setup
// -----------------------------------------------------------------

@Composable
fun WelcomeStep(
    name: String,
    onNameChange: (String) -> Unit,
    onStart: () -> Unit,
    isListening: Boolean,
    onMicClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Hero visual emblem
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Face,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Welcome to Aura 360",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Set up your personal identity and biometric verification to customize your intelligent assistant experience.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(Modifier.height(36.dp))

        // Name input card
        Surface(
            shape = RoundedCornerShape(CornerRadiusLarge),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "YOUR NAME",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                AuraTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = "",
                    placeholder = "Enter your full name",
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Outlined.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingIcon = {
                        IconButton(onClick = onMicClick) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Voice dictation",
                                tint = if (isListening) ErrorRed else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        AuraButton(
            text = "Continue to Face Scan",
            enabled = name.isNotBlank(),
            leadingIcon = Icons.AutoMirrored.Filled.ArrowForward,
            onClick = onStart
        )
    }
}

// -----------------------------------------------------------------
// Step 2: Face Scanning with CameraX & ML Kit
// -----------------------------------------------------------------

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun FaceScanningStep(
    navController: NavController,
    faceProgress: Float,
    sampleCount: Int,
    currentPose: FacePose,
    onFrameProcessed: (FloatArray) -> Unit,
    onSimulateScan: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val embeddingHelper = remember { FaceEmbeddingHelper(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val detector = remember {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build()
        )
    }

    var faceDetected by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Position face in circle") }

    val infiniteTransition = rememberInfiniteTransition(label = "scan_pulse")
    val scanPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "scan_pulse_alpha"
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
    val circleSize = (screenWidth * 0.72f).coerceIn(240.dp, 300.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Biometric Face Scan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Look at the camera while we map your facial features",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(16.dp))

        // Circular Camera Viewport with dual animated rings
        Box(
            modifier = Modifier
                .size(circleSize)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            // Pulse ring
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.dp, AccentCyan.copy(alpha = scanPulseAlpha), CircleShape)
            )

            // Dynamic progress ring
            CircularProgressIndicator(
                progress = { faceProgress },
                modifier = Modifier.fillMaxSize(),
                color = AccentCyan,
                strokeWidth = 5.dp,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                strokeCap = StrokeCap.Round
            )

            WithCameraPermission {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .clip(CircleShape),
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
                                                        statusText = if (sampleCount < 60) "Capturing sample ${sampleCount + 1}/60" else "Processing..."
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
                                                        statusText = "Center face in circle"
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
                                    } catch (e: Exception) {
                                        Log.e("OwnerEnrollmentScreen", "Camera bind error", e)
                                    }
                                }, ContextCompat.getMainExecutor(ctx))
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Pose guidance card
        Surface(
            shape = RoundedCornerShape(CornerRadiusMedium),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val poseIcon = when (currentPose) {
                    FacePose.FRONT -> Icons.Outlined.Face
                    FacePose.LEFT -> Icons.Default.ArrowBack
                    FacePose.RIGHT -> Icons.Default.ArrowForward
                    FacePose.UP -> Icons.Default.ArrowUpward
                    FacePose.DOWN -> Icons.Default.ArrowDownward
                    FacePose.NATURAL -> Icons.Outlined.SentimentSatisfied
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(AccentCyan.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(poseIcon, null, tint = AccentCyan, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (currentPose) {
                            FacePose.FRONT -> "Look straight ahead"
                            FacePose.LEFT -> "Turn head slightly left"
                            FacePose.RIGHT -> "Turn head slightly right"
                            FacePose.UP -> "Tilt head slightly up"
                            FacePose.DOWN -> "Tilt head slightly down"
                            FacePose.NATURAL -> "Look natural and relax"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (faceDetected) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Progress bar and Cancel row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Samples: $sampleCount / 60",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onSimulateScan != null) {
                    TextButton(onClick = onSimulateScan) {
                        Text(
                            text = "Simulate",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                TextButton(
                    onClick = {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate(NavRoutes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelMedium,
                        color = ErrorRed
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// Step 3: Voice Scanning
// -----------------------------------------------------------------

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        WithMicrophonePermission {
            LaunchedEffect(voiceCap) {
                if (voiceCap != null) startScanning()
            }

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.RecordVoiceOver,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Voice Command Setup",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Say \"Aura\" clearly 3 times to register your voice recognition wake word.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(36.dp))

            // Sample progress badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..3) {
                    val captured = voiceCount >= i
                    Surface(
                        shape = RoundedCornerShape(CornerRadiusPill),
                        color = if (captured) AccentCyan.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            1.dp,
                            if (captured) AccentCyan else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (captured) {
                                Icon(Icons.Default.Check, null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(
                                text = "Sample $i",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (captured) FontWeight.Bold else FontWeight.Normal,
                                color = if (captured) AccentCyan else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(40.dp))

            TextButton(onClick = onSkip) {
                Text(
                    text = "Skip for now",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// -----------------------------------------------------------------
// Step 4: Master Backup Password Setup
// -----------------------------------------------------------------

@Composable
fun PasswordSetupStep(
    password: String,
    onPasswordChange: (String) -> Unit,
    onComplete: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Emergency Security PIN",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Create a master password as a fallback when facial biometrics are unavailable.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        Surface(
            shape = RoundedCornerShape(CornerRadiusLarge),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                AuraTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = "Master PIN / Password",
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle Visibility",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Minimum 4 characters required",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        AuraButton(
            text = "Finalize Setup",
            onClick = onComplete,
            enabled = password.length >= 4
        )
    }
}

// -----------------------------------------------------------------
// Step 5: Processing & Step 6: Success
// -----------------------------------------------------------------

@Composable
fun ProcessingStep(onComplete: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1800)
        onComplete()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(56.dp),
            color = AccentCyan,
            strokeWidth = 4.dp
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Securing Biometrics...",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Generating hardware-encrypted neural embeddings",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SuccessStep(name: String, onDone: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AuraSuccessCheckmark(
            size = 88.dp,
            color = SuccessGreen
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Identity Registered",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Welcome, $name. Aura 360 is personalized and ready for conversation and vision intelligence.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(40.dp))

        AuraButton(
            text = "Launch Aura 360",
            onClick = onDone
        )
    }
}
