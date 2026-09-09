package com.humanoidai.ui.screens

import android.annotation.SuppressLint
import android.util.Log
import android.graphics.Bitmap
import android.graphics.Matrix
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
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
import com.humanoidai.ui.components.ArmsunFooter
import com.humanoidai.ui.components.WithCameraPermission
import com.humanoidai.ui.components.WithMicrophonePermission
import com.humanoidai.ui.theme.*
import com.humanoidai.voice.VoiceEngine
import kotlinx.coroutines.delay
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
    microphoneManager: SpeechRecognizerManager,
    voiceEngine: VoiceEngine
) {
    var step by remember { mutableStateOf(OwnerEnrollStep.WELCOME) }
    var ownerName by remember { mutableStateOf("") }
    var ownerPassword by remember { mutableStateOf("") }
    val isListening by microphoneManager.isListening.collectAsState()

    var currentPose by remember { mutableStateOf(FacePose.FRONT) }
    var poseSamplesCaptured by remember { mutableIntStateOf(0) }
    val SAMPLES_PER_POSE = 5

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
                OwnerEnrollStep.VOICE_SCANNING -> voiceEngine.speak("Face scan complete. Now, let's record your voice. Say the phrase: Aura 360, this is my voice, three times.")
                OwnerEnrollStep.PASSWORD_SETUP -> voiceEngine.speak("Voice registered. Finally, set a secure access password.")
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
                                onFinalResult = { ownerName = it }
                            )
                        }
                    )
                    OwnerEnrollStep.FACE_SCANNING -> FaceScanningStep(
                        faceProgress = faceProgress,
                        currentPose = currentPose,
                        onFrameProcessed = { embedding, pose, isStableFrame ->
                            if (pose == currentPose) {
                                val now = System.currentTimeMillis()
                                if (now - lastCaptureTime > 300) {
                                    capturedEmbeddings.add(embedding)
                                    poseSamplesCaptured++
                                    faceProgress = capturedEmbeddings.size / (FacePose.entries.size * SAMPLES_PER_POSE).toFloat()
                                    lastCaptureTime = now

                                    if (poseSamplesCaptured >= SAMPLES_PER_POSE) {
                                        val nextPoseIndex = currentPose.ordinal + 1
                                        if (nextPoseIndex < FacePose.entries.size) {
                                            currentPose = FacePose.entries[nextPoseIndex]
                                            poseSamplesCaptured = 0
                                        } else {
                                            step = OwnerEnrollStep.VOICE_SCANNING
                                        }
                                    }
                                }
                            }
                        }
                    )
                    OwnerEnrollStep.VOICE_SCANNING -> VoiceScanningStep(
                        voiceCount = voiceCount,
                        microphoneManager = microphoneManager,
                        onVoiceSampleDetected = {
                            voiceCount++
                            if (voiceCount >= 3) {
                                step = OwnerEnrollStep.PASSWORD_SETUP
                            }
                        }
                    )
                    OwnerEnrollStep.PASSWORD_SETUP -> PasswordSetupStep(
                        password = ownerPassword,
                        onPasswordChange = { ownerPassword = it },
                        onComplete = { if(ownerPassword.length >= 4) step = OwnerEnrollStep.PROCESSING }
                    )
                    OwnerEnrollStep.PROCESSING -> ProcessingStep {
                        ownerManager.enrollOwner(ownerName, capturedEmbeddings, 0.98f, voiceEnrolled = true)
                        ownerManager.setMasterPassword(ownerPassword)
                        ownerManager.getMasterEmbedding()?.let { recognitionManager.registerFace(ownerName, it) }
                        step = OwnerEnrollStep.SUCCESS
                    }
                    OwnerEnrollStep.SUCCESS -> SuccessStep(ownerName) {
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
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
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
    faceProgress: Float,
    currentPose: FacePose,
    onFrameProcessed: (FloatArray, FacePose, Boolean) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val embeddingHelper = remember { FaceEmbeddingHelper(context) }
    val fisheyeCorrector = remember { FisheyeCorrector() }
    val frameEnhancer = remember { FrameEnhancer() }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val detector = remember {
        FaceDetection.getClient(FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build())
    }

    var isCurrentlyStable by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
            embeddingHelper.close()
            fisheyeCorrector.release()
            detector.close()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "scanning")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val scannerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanner_rotation"
    )

    StepContainer(
        title = "Face Biometric Scan",
        subtitle = "Follow the pose instructions below"
    ) {
        Box(contentAlignment = Alignment.Center) {
            // 1. Static Outer Guide
            Canvas(modifier = Modifier.size(360.dp)) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.05f),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // 2. Main Rotating Aura Ring (Slow)
            Canvas(modifier = Modifier.size(340.dp).rotate(rotation)) {
                val strokeWidth = 3.dp.toPx()
                drawArc(
                    color = SuccessGreen,
                    startAngle = -90f, sweepAngle = 80f, useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                drawArc(
                    color = SuccessGreen,
                    startAngle = 90f, sweepAngle = 80f, useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // 3. NEW: Fast Rotating "Scanning Section" (Laser Sweep)
            Canvas(
                modifier = Modifier
                    .size(330.dp)
                    .rotate(scannerRotation)
            ) {
                drawArc(
                    brush = Brush.sweepGradient(
                        0f to Color.Transparent,
                        0.5f to SuccessGreen.copy(alpha = 0.5f),
                        1f to SuccessGreen
                    ),
                    startAngle = 0f,
                    sweepAngle = 45f,
                    useCenter = false,
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )
                
                // Add a "Lead Laser" point
                drawCircle(
                    color = SuccessGreen,
                    radius = 4.dp.toPx(),
                    center = Offset(size.width / 2, 0f) // Simplified for the sweep effect
                )
            }

            CircularProgressIndicator(
                progress = { faceProgress },
                modifier = Modifier.size(360.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp,
                trackColor = Color.White.copy(alpha = 0.02f)
            )
            
            WithCameraPermission {
                Box(
                    modifier = Modifier
                        .size(320.dp) // Large camera round as requested
                        .clip(CircleShape)
                        .border(1.5.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                        .drawBehind {
                             val step = 40.dp.toPx()
                             for(x in 0..(size.width/step).toInt()) {
                                 drawLine(Color.White.copy(alpha = 0.05f), Offset(x*step, 0f), Offset(x*step, size.height))
                             }
                             for(y in 0..(size.height/step).toInt()) {
                                 drawLine(Color.White.copy(alpha = 0.05f), Offset(0f, y*step), Offset(size.width, y*step))
                             }
                        }
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).also { pv ->
                                val future = ProcessCameraProvider.getInstance(ctx)
                                future.addListener({
                                    val provider = future.get()
                                    val preview = Preview.Builder().build().also { it.surfaceProvider = pv.surfaceProvider }
                                    val analysis = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                                        .build()
                                    analysis.setAnalyzer(analysisExecutor) { image ->
                                        processStepFrame(image, detector, embeddingHelper, fisheyeCorrector, frameEnhancer) { embedding, pose, isStable ->
                                            isCurrentlyStable = isStable
                                            onFrameProcessed(embedding, pose, isStable)
                                        }
                                    }
                                    try {
                                        provider.unbindAll()
                                        provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis)
                                    } catch (e: Exception) {}
                                }, ContextCompat.getMainExecutor(ctx))
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            if (isCurrentlyStable) {
                Surface(
                    color = SuccessGreen.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f)),
                    modifier = Modifier.align(Alignment.TopCenter).offset(y = (-60).dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("STABLE DETECTED", color = SuccessGreen, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                }
            }
        }
        
        Spacer(Modifier.height(40.dp))
        Text("SCANNING", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(Modifier.height(8.dp))
        Text(currentPose.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        Text(
            when(currentPose) {
                FacePose.FRONT -> "Look straight at the camera"
                FacePose.LEFT -> "Turn slightly LEFT"
                FacePose.RIGHT -> "Turn slightly RIGHT"
                FacePose.UP -> "Look slightly UP"
                FacePose.DOWN -> "Look slightly DOWN"
                FacePose.NATURAL -> "Natural Expression"
            },
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { faceProgress },
            modifier = Modifier.width(200.dp).height(4.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )
    }
}

@Composable
fun VoiceScanningStep(
    voiceCount: Int,
    microphoneManager: SpeechRecognizerManager,
    onVoiceSampleDetected: () -> Unit
) {
    val isListening by microphoneManager.isListening.collectAsState()
    val ambientNoise by microphoneManager.ambientNoise.collectAsState()
    val voicePulse by animateFloatAsState((ambientNoise / 100f).coerceIn(0f, 1f), label = "pulse")
    var lastVoiceTriggerTime by remember { mutableLongStateOf(0L) }

    val startScanning = {
        microphoneManager.startListening(
            onPartialResult = { text ->
                val phrase = text.lowercase()
                if (phrase.contains("aura 360") && (phrase.contains("voice") || phrase.contains("this"))) {
                    val now = System.currentTimeMillis()
                    if (now - lastVoiceTriggerTime > 2500) {
                        lastVoiceTriggerTime = now
                        onVoiceSampleDetected()
                    }
                }
            },
            onFinalResult = { /* Handled via partial results for trigger */ },
            onError = { /* Handled internally */ }
        )
    }

    LaunchedEffect(Unit) { startScanning() }
    DisposableEffect(Unit) { onDispose { microphoneManager.stopListening() } }

    StepContainer(
        title = "Voice Biometric Scan",
        subtitle = "Say \"Aura 360, this is my voice\" three times"
    ) {
        WithMicrophonePermission {
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
                    onClick = { if (isListening) microphoneManager.stopListening() else startScanning() },
                    modifier = Modifier.size(80.dp).background(if (isListening) ErrorRed.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape).border(2.dp, if (isListening) ErrorRed else MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(if (isListening) Icons.Default.StopCircle else Icons.Default.Mic, null, tint = if (isListening) ErrorRed else MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                }
            }
            Spacer(Modifier.height(40.dp))
            Text("SAMPLES: $voiceCount / 3", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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
    StepContainer(
        title = "System Password",
        subtitle = "Set a secure password for emergency access"
    ) {
        AuraTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = "Master Password",
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
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

@SuppressLint("UnsafeOptInUsageError")
private fun processStepFrame(
    image: ImageProxy,
    detector: FaceDetector,
    helper: FaceEmbeddingHelper,
    corrector: FisheyeCorrector,
    enhancer: FrameEnhancer,
    onResult: (FloatArray, FacePose, Boolean) -> Unit
) {
    val rotation = image.imageInfo.rotationDegrees
    val original = try {
        val bitmap = image.toBitmap()
        val matrix = Matrix()
        matrix.postRotate(rotation.toFloat())
        matrix.postScale(-1f, 1f)
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } catch (e: Exception) { image.close(); return }

    val processedBitmap = if (FisheyeCorrector.isOpenCVLoaded) {
        try {
            val corrected = corrector.correct(original)
            val mat = Mat()
            Utils.bitmapToMat(corrected, mat)
            val bgr = Mat()
            Imgproc.cvtColor(mat, bgr, Imgproc.COLOR_RGBA2BGR)
            val enhancedBgr = enhancer.enhance(bgr)
            val outputRgba = Mat()
            Imgproc.cvtColor(enhancedBgr, outputRgba, Imgproc.COLOR_BGR2RGBA)
            val outputBitmap = Bitmap.createBitmap(outputRgba.cols(), outputRgba.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(outputRgba, outputBitmap)
            mat.release(); bgr.release(); enhancedBgr.release(); outputRgba.release()
            outputBitmap
        } catch (e: Exception) { original }
    } else { original }

    detector.process(InputImage.fromBitmap(processedBitmap, 0))
        .addOnSuccessListener { faces ->
            if (faces.isNotEmpty()) {
                val face = faces[0]
                val yaw = face.headEulerAngleY
                val pitch = face.headEulerAngleX
                val box = face.boundingBox
                
                // Quality Checks (Phase 11: Production Biometrics)
                val isCentered = abs(box.centerX() - processedBitmap.width/2) < processedBitmap.width * 0.20
                val isSizedRight = box.width() > processedBitmap.width * 0.20
                
                val pose = when {
                    yaw < -15f -> FacePose.LEFT
                    yaw > 15f -> FacePose.RIGHT
                    pitch > 15f -> FacePose.UP
                    pitch < -15f -> FacePose.DOWN
                    abs(yaw) < 10f && abs(pitch) < 10f -> FacePose.FRONT
                    else -> FacePose.NATURAL
                }

                if (isCentered && isSizedRight) {
                    try {
                        val isolated = FacePreprocessor.alignAndIsolate(processedBitmap, face)
                        onResult(helper.getEmbedding(isolated), pose, true)
                    } catch (e: Exception) { Log.e("OwnerEnrollment", "Error: ${e.message}") }
                } else {
                    // Still notify for UI state even if not capture-worthy
                    onResult(FloatArray(0), pose, false)
                }
            }
        }
        .addOnCompleteListener { image.close() }
}
