package com.humanoidai.ui.screens

import android.annotation.SuppressLint
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import com.humanoidai.ml.FaceEmbeddingHelper
import com.humanoidai.ml.FaceRecognitionManager
import com.humanoidai.ml.FisheyeCorrector
import com.humanoidai.ml.OwnerEnrollmentManager
import com.humanoidai.vision.FrameEnhancer
import com.humanoidai.navigation.NavRoutes
import com.humanoidai.ui.components.WithCameraPermission
import com.humanoidai.ui.components.WithMicrophonePermission
import com.humanoidai.ui.theme.*
import java.util.*
import java.util.concurrent.Executors

enum class OwnerEnrollStep { WELCOME, FACE_SCANNING, VOICE_SCANNING, PROCESSING, SUCCESS }

@Composable
fun OwnerEnrollmentScreen(
    navController: NavController,
    ownerManager: OwnerEnrollmentManager,
    recognitionManager: FaceRecognitionManager,
    microphoneManager: com.humanoidai.hearing.SpeechRecognizerManager,
    voiceEngine: com.humanoidai.voice.VoiceEngine
) {
    var step by remember { mutableStateOf(OwnerEnrollStep.WELCOME) }
    var ownerName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val isListening by microphoneManager.isListening.collectAsState()

    LaunchedEffect(step) {
        when(step) {
            OwnerEnrollStep.WELCOME -> voiceEngine.speak("Welcome. Let's build your profile. Enter your name first.")
            OwnerEnrollStep.FACE_SCANNING -> {
                voiceEngine.speak("Position your face in the circle. Move your head slowly to all angles.")
            }
            OwnerEnrollStep.VOICE_SCANNING -> {
                voiceEngine.speak("Face scan complete. Now, let's record your voice. Say the phrase: Humanoid, this is my voice, three times.")
            }
            OwnerEnrollStep.PROCESSING -> {
                voiceEngine.speak("Fusing your biometric data.")
            }
            OwnerEnrollStep.SUCCESS -> voiceEngine.speak("Setup complete. Welcome, $ownerName.")
        }
    }
    
    // Scan State
    var faceProgress by remember { mutableFloatStateOf(0f) }
    var voiceCount by remember { mutableIntStateOf(0) }
    val capturedEmbeddings = remember { mutableListOf<FloatArray>() }
    var lastCaptureTime by remember { mutableLongStateOf(0L) }
    
    val coveragePoints = remember { mutableStateMapOf<Int, Boolean>().apply { 
        (0..8).forEach { put(it, false) } 
    } }

    Scaffold(containerColor = BackgroundDark) { padding ->
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
                            else microphoneManager.startListening(onFinalResult = { ownerName = it })
                        }
                    )
                    OwnerEnrollStep.FACE_SCANNING -> FaceScanningStep(
                        faceProgress = faceProgress,
                        onFrameProcessed = { embedding, angleId ->
                            val now = System.currentTimeMillis()
                            if (now - lastCaptureTime > 200) {
                                coveragePoints[angleId] = true
                                if (capturedEmbeddings.size < 25) {
                                    capturedEmbeddings.add(embedding)
                                    faceProgress = capturedEmbeddings.size / 25f
                                    lastCaptureTime = now
                                }
                                
                                if (capturedEmbeddings.size >= 25) {
                                    step = OwnerEnrollStep.VOICE_SCANNING
                                }
                            }
                        }
                    )
                    OwnerEnrollStep.VOICE_SCANNING -> VoiceScanningStep(
                        voiceCount = voiceCount,
                        microphoneManager = microphoneManager,
                        onVoiceSampleDetected = {
                            voiceCount++
                            if (voiceCount < 3) {
                                voiceEngine.speak("Sample $voiceCount recorded.")
                            } else {
                                step = OwnerEnrollStep.PROCESSING
                            }
                        }
                    )
                    OwnerEnrollStep.PROCESSING -> ProcessingStep {
                        ownerManager.enrollOwner(ownerName, capturedEmbeddings, 0.98f, voiceEnrolled = true)
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
        }
    }
}

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun FaceScanningStep(
    faceProgress: Float,
    onFrameProcessed: (FloatArray, Int) -> Unit
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
            .build())
    }

    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
            embeddingHelper.close()
            fisheyeCorrector.release()
            detector.close()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Face Biometric Scan", color = AccentCyan, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text("Position face in the circle and rotate slowly", color = TextSecondary, fontSize = 12.sp)
        
        Spacer(Modifier.height(40.dp))
        
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { faceProgress },
                modifier = Modifier.size(280.dp),
                color = AccentCyan,
                strokeWidth = 4.dp,
                trackColor = Color.White.copy(alpha = 0.05f)
            )
            
            WithCameraPermission {
                Box(modifier = Modifier.size(240.dp).clip(CircleShape).border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)) {
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
                                        processStepFrame(image, detector, embeddingHelper, fisheyeCorrector, frameEnhancer) { embedding, angleId ->
                                            onFrameProcessed(embedding, angleId)
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
        }
        
        Spacer(Modifier.height(40.dp))
        Text("${(faceProgress * 100).toInt()}%", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AccentCyan)
        Text("Capturing face viewpoints...", color = TextSecondary, fontSize = 14.sp)
    }
}

@Composable
fun VoiceScanningStep(
    voiceCount: Int,
    microphoneManager: com.humanoidai.hearing.SpeechRecognizerManager,
    onVoiceSampleDetected: () -> Unit
) {
    val isListening by microphoneManager.isListening.collectAsState()
    val ambientNoise by microphoneManager.ambientNoise.collectAsState()
    
    val voicePulse by animateFloatAsState(
        targetValue = (ambientNoise / 100f).coerceIn(0f, 1f),
        animationSpec = tween(100),
        label = "voice_pulse"
    )

    var lastVoiceTriggerTime by remember { mutableLongStateOf(0L) }

    val startScanning = {
        microphoneManager.startListening(
            onPartialResult = { text ->
                val phrase = text.lowercase()
                val hasKeywords = phrase.contains("humanoid") && 
                                 (phrase.contains("voice") || phrase.contains("this"))
                
                val now = System.currentTimeMillis()
                if (hasKeywords && now - lastVoiceTriggerTime > 2500) {
                    lastVoiceTriggerTime = now
                    onVoiceSampleDetected()
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        startScanning()
    }

    DisposableEffect(Unit) {
        onDispose {
            microphoneManager.stopListening()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Voice Biometric Scan", color = AccentPurple, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(16.dp))
        
        WithMicrophonePermission {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Say the phrase 3 times:",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    "\"Humanoid, this is my voice.\"",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                
                Spacer(Modifier.height(40.dp))
                
                Box(contentAlignment = Alignment.Center) {
                    // Frequency Pulse Wave
                    FrequencyPulseWave(
                        amplitude = voicePulse,
                        color = AccentPurple
                    )

                    CircularProgressIndicator(
                        progress = { voiceCount.toFloat() / 3f },
                        modifier = Modifier.size(200.dp),
                        color = AccentPurple,
                        strokeWidth = 6.dp,
                        trackColor = Color.White.copy(alpha = 0.05f)
                    )
                    
                    Surface(
                        onClick = {
                            if (isListening) microphoneManager.stopListening()
                            else startScanning()
                        },
                        modifier = Modifier.size(80.dp),
                        color = if (isListening) Color.Red.copy(alpha = 0.2f) else AccentPurple.copy(alpha = 0.1f),
                        shape = CircleShape,
                        border = BorderStroke(2.dp, if (isListening) Color.Red else AccentPurple)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (isListening) Icons.Default.StopCircle else Icons.Default.Mic,
                                null,
                                tint = if (isListening) Color.Red else AccentPurple,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(40.dp))
                Text("SAMPLES: $voiceCount / 3", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                
                // Live Guidance Text
                val guidanceText = when {
                    !isListening -> "TAP TO START"
                    voicePulse < 0.15f -> "SPEAK LOUDER"
                    voicePulse < 0.4f -> "LISTENING..."
                    else -> "PERFECT VOLUME"
                }
                val guidanceColor = when {
                    !isListening -> TextSecondary
                    voicePulse < 0.15f -> Color.Yellow
                    voicePulse < 0.4f -> SuccessGreen
                    else -> AccentCyan
                }
                
                Text(guidanceText, color = guidanceColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
            // Simulate frequency variety using a sine-based variation
            val variation = 0.6f + 0.4f * Math.abs(Math.sin(i.toDouble() * 0.3)).toFloat()
            val length = innerRadius + (amplitude * maxExtra * variation)
            
            val start = Offset(
                (center.x + innerRadius * Math.cos(angle)).toFloat(),
                (center.y + innerRadius * Math.sin(angle)).toFloat()
            )
            val end = Offset(
                (center.x + length * Math.cos(angle)).toFloat(),
                (center.y + length * Math.sin(angle)).toFloat()
            )
            
            // Draw a glowing bar
            drawLine(
                color = color.copy(alpha = 0.4f * (0.2f + amplitude)),
                start = start,
                end = end,
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
            
            // Core sharp bar
            drawLine(
                color = color.copy(alpha = 0.8f),
                start = start,
                end = end,
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun WelcomeStep(name: String, onNameChange: (String) -> Unit, onStart: () -> Unit, isListening: Boolean, onMicClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Fingerprint, null, tint = AccentCyan, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(24.dp))
        Text("Multi-Modal Enrollment", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(12.dp))
        Text(
            "Position your face and speak the phrase simultaneously to build your unified profile.",
            textAlign = TextAlign.Center, color = TextSecondary, fontSize = 14.sp
        )
        
        Spacer(Modifier.height(48.dp))
        
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            placeholder = { Text("Enter your name", color = TextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentCyan,
                unfocusedBorderColor = Color(0xFF1E1E24),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            trailingIcon = {
                IconButton(onClick = onMicClick) {
                    Icon(if (isListening) Icons.Default.MicOff else Icons.Default.Mic, null, tint = if (isListening) Color.Red else AccentCyan)
                }
            }
        )
        
        Spacer(Modifier.height(24.dp))
        
        Button(
            onClick = onStart,
            enabled = name.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
        ) {
            Text("Start Scanning", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ProcessingStep(onComplete: () -> Unit) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        onComplete()
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = AccentCyan)
        Spacer(Modifier.height(24.dp))
        Text("Synthesizing Biometric Data...", color = Color.White)
        Text("Generating unified face-voice hash", fontSize = 12.sp, color = TextSecondary)
    }
}

@Composable
fun SuccessStep(name: String, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(80.dp))
        Spacer(Modifier.height(32.dp))
        Text("Enrollment Successful", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(12.dp))
        Text("Welcome, $name. Your AI world is now secured.", textAlign = TextAlign.Center, color = TextSecondary)
        
        Spacer(Modifier.height(48.dp))
        
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
        ) {
            Text("Launch Humanoid AI", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@SuppressLint("UnsafeOptInUsageError")
private fun processStepFrame(
    image: ImageProxy,
    detector: com.google.mlkit.vision.face.FaceDetector,
    helper: FaceEmbeddingHelper,
    corrector: FisheyeCorrector,
    enhancer: FrameEnhancer,
    onResult: (FloatArray, Int) -> Unit
) {
    val rotation = image.imageInfo.rotationDegrees
    val original = try {
        val bitmap = image.toBitmap()
        val matrix = android.graphics.Matrix()
        
        // Handle Rotation
        matrix.postRotate(rotation.toFloat())
        
        // Front camera mirroring
        matrix.postScale(-1f, 1f)
        
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } catch (e: Exception) { image.close(); return }

    val processedBitmap = if (FisheyeCorrector.isOpenCVLoaded) {
        try {
            // 1. Fisheye Correction
            val corrected = corrector.correct(original)
            
            // 2. Enhancement Pipeline (Mirroring FaceAnalyzer)
            val mat = org.opencv.core.Mat()
            org.opencv.android.Utils.bitmapToMat(corrected, mat)
            
            val bgr = org.opencv.core.Mat()
            org.opencv.imgproc.Imgproc.cvtColor(mat, bgr, org.opencv.imgproc.Imgproc.COLOR_RGBA2BGR)
            
            val enhancedBgr = enhancer.enhance(bgr)
            
            val outputRgba = org.opencv.core.Mat()
            org.opencv.imgproc.Imgproc.cvtColor(enhancedBgr, outputRgba, org.opencv.imgproc.Imgproc.COLOR_BGR2RGBA)
            
            val outputBitmap = Bitmap.createBitmap(outputRgba.cols(), outputRgba.rows(), Bitmap.Config.ARGB_8888)
            org.opencv.android.Utils.matToBitmap(outputRgba, outputBitmap)
            
            mat.release()
            bgr.release()
            enhancedBgr.release()
            outputRgba.release()
            
            outputBitmap
        } catch (e: Exception) {
            original
        }
    } else {
        original
    }

    val input = InputImage.fromBitmap(processedBitmap, 0)

    detector.process(input)
        .addOnSuccessListener { faces ->
            if (faces.isNotEmpty()) {
                val face = faces[0]
                val box = face.boundingBox
                val rotY = face.headEulerAngleY 
                val rotX = face.headEulerAngleX 
                val col = when { rotY < -15 -> 0; rotY > 15 -> 2; else -> 1 }
                val row = when { rotX < -15 -> 0; rotX > 15 -> 2; else -> 1 }
                val angleId = row * 3 + col
                
                try {
                    val left = box.left.coerceAtLeast(0)
                    val top = box.top.coerceAtLeast(0)
                    val width = box.width().coerceAtLeast(1)
                    val height = box.height().coerceAtLeast(1)
                    
                    // 3. Face Isolation Logic (Background Removal)
                    val crop = Bitmap.createBitmap(processedBitmap, left, top, width, height)
                    val isolated = isolateFace(crop)
                    
                    onResult(helper.getEmbedding(isolated), angleId)
                } catch (e: Exception) {}
            }
        }
        .addOnCompleteListener { image.close() }
}

/**
 * Advanced Face Isolation: Applies a circular mask to the face crop, 
 * blacking out the background to improve recognition accuracy.
 */
private fun isolateFace(source: Bitmap): Bitmap {
    val size = Math.min(source.width, source.height)
    val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(output)
    
    val paint = android.graphics.Paint()
    val rect = android.graphics.Rect(0, 0, size, size)
    
    paint.isAntiAlias = true
    canvas.drawARGB(0, 0, 0, 0)
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    
    paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(source, rect, rect, paint)
    
    return output
}
