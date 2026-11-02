package com.humanoidai.ui.screens

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.humanoidai.hearing.SpeechRecognizerManager
import com.humanoidai.ml.FaceEmbeddingHelper
import com.humanoidai.ml.FaceEnrollmentManager
import com.humanoidai.ml.FacePreprocessor
import com.humanoidai.ml.FaceRecognitionManager
import com.humanoidai.ml.OwnerEnrollmentManager
import com.humanoidai.ui.components.ArmsunFooter
import com.humanoidai.ui.components.WithCameraPermission
import com.humanoidai.ui.theme.*
import com.humanoidai.voice.VoiceEngine
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.math.abs

enum class EnrollStep { ENTER_NAME, CAPTURE, SUCCESS }

@SuppressLint("UnsafeOptInUsageError")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnrollmentScreen(
    navController: NavController,
    enrollmentManager: FaceEnrollmentManager,
    ownerManager: OwnerEnrollmentManager,
    recognitionManager: FaceRecognitionManager,
    microphoneManager: SpeechRecognizerManager,
    voiceEngine: VoiceEngine
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope          = rememberCoroutineScope()
    val haptic         = LocalHapticFeedback.current

    var name          by remember { mutableStateOf("") }
    var label         by remember { mutableStateOf("Family") }
    var step          by remember { mutableStateOf(EnrollStep.ENTER_NAME) }
    var faceProgress  by remember { mutableFloatStateOf(0f) }
    var currentPose   by remember { mutableStateOf(FacePose.FRONT) }
    var poseSamples   by remember { mutableIntStateOf(0) }
    var lastCaptureTime by remember { mutableLongStateOf(0L) }
    val SAMPLES_PER_POSE = 10
    val totalTarget = FacePose.entries.size * SAMPLES_PER_POSE
    val isListening by microphoneManager.isListening.collectAsState()

    LaunchedEffect(step, currentPose) {
        when(step) {
            EnrollStep.ENTER_NAME -> voiceEngine.speak("Who are you enrolling today?")
            EnrollStep.CAPTURE -> {
                val instruction = when(currentPose) {
                    FacePose.FRONT -> "Look straight at the camera."
                    FacePose.LEFT -> "Turn slightly left."
                    FacePose.RIGHT -> "Turn slightly right."
                    FacePose.UP -> "Look slightly up."
                    FacePose.DOWN -> "Look slightly down."
                    FacePose.NATURAL -> "Look naturally at the camera."
                }
                voiceEngine.speak(instruction)
            }
            EnrollStep.SUCCESS -> voiceEngine.speak("$name has been enrolled successfully.")
        }
    }

    val capturedEmbeddings = remember { mutableListOf<FloatArray>() }
    val embeddingHelper    = remember { FaceEmbeddingHelper(context) }
    val analysisExecutor   = remember { Executors.newSingleThreadExecutor() }
    val detector           = remember {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .build()
        )
    }

    val labels = listOf("Family", "Colleague", "Neighbor", "Friend", "Other")

    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
            embeddingHelper.close()
            detector.close()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Face Enrollment",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (step) {
                    EnrollStep.ENTER_NAME -> {
                        Spacer(modifier = Modifier.height(24.dp))

                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(AccentCyan.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.PersonAdd, null, tint = AccentCyan, modifier = Modifier.size(32.dp))
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Who are you enrolling?",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Aura 360 will map 60 facial angles for precise real-time recognition.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        AuraTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = "Full Name",
                            placeholder = "e.g. Sarah Connor",
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Outlined.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            trailingIcon = {
                                IconButton(onClick = {
                                    if (isListening) {
                                        microphoneManager.stopListening()
                                    } else {
                                        microphoneManager.startListening(
                                            onFinalResult = { name = it }
                                        )
                                    }
                                }) {
                                    Icon(
                                        if (isListening) Icons.Default.MicOff else Icons.Outlined.Mic,
                                        null,
                                        tint = if (isListening) ErrorRed else AccentCyan
                                    )
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            "RELATIONSHIP CATEGORY",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentCyan,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.fillMaxWidth().padding(start = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            labels.forEach { l ->
                                val selected = label == l
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { label = l },
                                    shape = RoundedCornerShape(CornerRadiusMedium),
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(
                                        1.dp,
                                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            l,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        AuraButton(
                            text = "Start Biometric Capture",
                            onClick = {
                                if (name.isNotBlank()) {
                                    step = EnrollStep.CAPTURE
                                }
                            },
                            enabled = name.isNotBlank(),
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                    }
                    EnrollStep.CAPTURE -> {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Header pill
                        Surface(
                            shape = RoundedCornerShape(CornerRadiusLarge),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "ENROLLING: ${name.uppercase()}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentCyan
                                    )
                                    Text(
                                        "POSE: ${currentPose.name}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    color = SuccessGreen.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(CornerRadiusSmall)
                                ) {
                                    Text(
                                        "${capturedEmbeddings.size} / 60",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = SuccessGreen,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Circular FaceID-style camera aperture
                        WithCameraPermission {
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val pulseAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.4f,
                                targetValue = 0.9f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1200, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "pulse_alpha"
                            )

                            Box(
                                modifier = Modifier
                                    .size(280.dp)
                                    .drawBehind {
                                        drawCircle(
                                            color = AccentCyan.copy(alpha = pulseAlpha * 0.25f),
                                            radius = size.minDimension / 2 + 12.dp.toPx()
                                        )
                                        drawCircle(
                                            color = AccentCyan.copy(alpha = 0.4f),
                                            radius = size.minDimension / 2 + 2.dp.toPx(),
                                            style = Stroke(width = 2.dp.toPx())
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
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
                                                            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                                            detector.process(inputImage)
                                                                .addOnSuccessListener { faces ->
                                                                    if (faces.isNotEmpty()) {
                                                                        val face = faces[0]
                                                                        val yaw = face.headEulerAngleY
                                                                        val pitch = face.headEulerAngleX

                                                                        val pose = when {
                                                                            yaw < -7f -> FacePose.LEFT
                                                                            yaw > 7f -> FacePose.RIGHT
                                                                            pitch > 7f -> FacePose.UP
                                                                            pitch < -7f -> FacePose.DOWN
                                                                            abs(yaw) < 12f && abs(pitch) < 12f -> FacePose.FRONT
                                                                            else -> FacePose.NATURAL
                                                                        }

                                                                        val now = System.currentTimeMillis()
                                                                        if (now - lastCaptureTime > 120 && (pose == currentPose || currentPose == FacePose.NATURAL)) {
                                                                            try {
                                                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                                lastCaptureTime = now

                                                                                val bitmap = imageProxy.toBitmap()
                                                                                val isolated = FacePreprocessor.alignAndIsolate(bitmap, face)
                                                                                capturedEmbeddings.add(embeddingHelper.getEmbedding(isolated))
                                                                                poseSamples++
                                                                                faceProgress = capturedEmbeddings.size / totalTarget.toFloat()

                                                                                if (poseSamples >= SAMPLES_PER_POSE) {
                                                                                    val nextPoseIndex = currentPose.ordinal + 1
                                                                                    if (nextPoseIndex < FacePose.entries.size) {
                                                                                        currentPose = FacePose.entries[nextPoseIndex]
                                                                                        poseSamples = 0
                                                                                    } else {
                                                                                        scope.launch {
                                                                                            enrollmentManager.enrollPerson(name, label, capturedEmbeddings)
                                                                                            enrollmentManager.loadAllInto(recognitionManager, ownerManager)
                                                                                            step = EnrollStep.SUCCESS
                                                                                        }
                                                                                    }
                                                                                }
                                                                            } catch (e: Exception) {
                                                                                Log.e("Enrollment", "Frame processing failed: ${e.message}")
                                                                            }
                                                                        }
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
                                                        Log.e("EnrollmentScreen", "Failed to bind camera", e)
                                                    }
                                                }, ContextCompat.getMainExecutor(ctx))
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Pose instruction badge
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(CornerRadiusMedium),
                            border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = when(currentPose) {
                                    FacePose.FRONT -> "LOOK STRAIGHT AT CAMERA"
                                    FacePose.LEFT -> "TURN SLIGHTLY LEFT"
                                    FacePose.RIGHT -> "TURN SLIGHTLY RIGHT"
                                    FacePose.UP -> "LOOK SLIGHTLY UP"
                                    FacePose.DOWN -> "LOOK SLIGHTLY DOWN"
                                    FacePose.NATURAL -> "LOOK NATURALLY AT CAMERA"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = AccentCyan,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        LinearProgressIndicator(
                            progress = { faceProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = AccentCyan,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "Hold still while poses are automatically calibrated...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    EnrollStep.SUCCESS -> {
                        Spacer(modifier = Modifier.height(60.dp))

                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(SuccessGreen.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(54.dp))
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "$name Enrolled!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Face embeddings successfully mapped.\nAura 360 will now identify this person on camera.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(48.dp))

                        AuraButton(
                            text = "Done",
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        AuraOutlinedButton(
                            text = "Enroll Another Person",
                            onClick = {
                                name = ""
                                faceProgress = 0f
                                currentPose = FacePose.FRONT
                                poseSamples = 0
                                capturedEmbeddings.clear()
                                step = EnrollStep.ENTER_NAME
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            ArmsunFooter(modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}
