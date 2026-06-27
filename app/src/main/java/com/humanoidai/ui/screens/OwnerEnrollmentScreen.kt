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
import androidx.compose.ui.graphics.Color
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
import com.humanoidai.ui.components.WithCameraPermission
import com.humanoidai.ui.theme.*
import java.util.*
import java.util.concurrent.Executors

enum class OwnerEnrollStep { WELCOME, SCANNING, PROCESSING, SUCCESS }

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
            OwnerEnrollStep.WELCOME -> voiceEngine.speak("Welcome. Let's personalize your AI. What is your name?")
            OwnerEnrollStep.SCANNING -> voiceEngine.speak("Position your face in the circle and follow the instructions.")
            OwnerEnrollStep.PROCESSING -> voiceEngine.speak("Building your biometric profile.")
            OwnerEnrollStep.SUCCESS -> voiceEngine.speak("Enrollment complete. Welcome to your AI world, $ownerName.")
        }
    }
    
    // Scan State
    var progress by remember { mutableFloatStateOf(0f) }
    var instruction by remember { mutableStateOf("Position your face in the circle") }
    val capturedEmbeddings = remember { mutableListOf<FloatArray>() }
    var lastCaptureTime by remember { mutableLongStateOf(0L) }
    
    // Coverage points (representing angles: center, up, down, left, right, etc.)
    val coveragePoints = remember { mutableStateMapOf<Int, Boolean>().apply { 
        (0..8).forEach { put(it, false) } 
    } }

    Scaffold(containerColor = Color(0xFF020408)) { padding ->
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
                        onStart = { if(ownerName.isNotBlank()) step = OwnerEnrollStep.SCANNING },
                        isListening = isListening,
                        onMicClick = {
                            if (isListening) {
                                microphoneManager.stopListening()
                            } else {
                                microphoneManager.startListening(
                                    onFinalResult = { ownerName = it }
                                )
                            }
                        }
                    )
                    OwnerEnrollStep.SCANNING -> ScanningStep(
                        instruction = instruction,
                        progress = progress,
                        coveragePoints = coveragePoints,
                        onFrameProcessed = { embedding, angleId ->
                            val now = System.currentTimeMillis()
                            // Throttling: only capture one frame every 150ms to ensure variety
                            if (now - lastCaptureTime > 150) {
                                val isNewAngle = !coveragePoints[angleId]!!
                                coveragePoints[angleId] = true
                                
                                if (capturedEmbeddings.size < 25) {
                                    capturedEmbeddings.add(embedding)
                                    progress = capturedEmbeddings.size / 25f
                                    lastCaptureTime = now
                                    // Update instructions every time or when state changes
                                    updateInstruction(angleId, coveragePoints, capturedEmbeddings.size) { instruction = it }
                                }
                                
                                // Transition to processing only if target reached AND all angles covered
                                if (capturedEmbeddings.size >= 25 && coveragePoints.all { it.value }) {
                                    step = OwnerEnrollStep.PROCESSING
                                }
                            }
                        }
                    )
                    OwnerEnrollStep.PROCESSING -> ProcessingStep {
                        ownerManager.enrollOwner(ownerName, capturedEmbeddings, 0.98f)
                        ownerManager.getMasterEmbedding()?.let { 
                            recognitionManager.registerFace(ownerName, it) 
                        }
                        step = OwnerEnrollStep.SUCCESS
                    }
                    OwnerEnrollStep.SUCCESS -> SuccessStep(ownerName) {
                        navController.navigate("dashboard") {
                            popUpTo("owner_enrollment") { inclusive = true }
                        }
                    }
                }
            }
        }
    }
}

private fun updateInstruction(lastAngle: Int, coverage: Map<Int, Boolean>, currentCount: Int, setMsg: (String) -> Unit) {
    val remaining = coverage.filter { !it.value }.keys
    if (remaining.isEmpty()) {
        if (currentCount < 25) {
            setMsg("Hold still... finalizing biometric data")
        } else {
            setMsg("Scan complete. Processing...")
        }
        return
    }
    val next = when (remaining.first()) {
        1 -> "Tilt your head UP"
        2 -> "Tilt your head DOWN"
        3 -> "Look slowly LEFT"
        4 -> "Look slowly RIGHT"
        5 -> "Now SMILE"
        6 -> "Blink your eyes"
        else -> "Keep moving slowly"
    }
    setMsg(next)
}

@Composable
fun WelcomeStep(name: String, onNameChange: (String) -> Unit, onStart: () -> Unit, isListening: Boolean, onMicClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Face, null, tint = AccentCyan, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(24.dp))
        Text("Welcome to Humanoid AI", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(12.dp))
        Text(
            "Let's personalize your AI. This will build your master biometric profile.",
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
            Text("Start Enrollment", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun ScanningStep(
    instruction: String,
    progress: Float,
    coveragePoints: Map<Int, Boolean>,
    onFrameProcessed: (FloatArray, Int) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val embeddingHelper = remember { FaceEmbeddingHelper(context) }
    val fisheyeCorrector = remember { FisheyeCorrector() }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val detector = remember {
        FaceDetection.getClient(FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .build())
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Owner Enrollment", color = AccentCyan, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(32.dp))
        
        Box(contentAlignment = Alignment.Center) {
            // Progress Ring
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(260.dp),
                color = AccentCyan,
                strokeWidth = 4.dp,
                trackColor = Color.White.copy(alpha = 0.1f)
            )
            
            // Camera Preview
            WithCameraPermission {
                Box(modifier = Modifier.size(240.dp).clip(CircleShape).border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)) {
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).also { pv ->
                                val future = ProcessCameraProvider.getInstance(ctx)
                                future.addListener({
                                    val provider = future.get()
                                    val preview = Preview.Builder().build().also { it.surfaceProvider = pv.surfaceProvider }
                                    val analysis = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()
                                    
                                    analysis.setAnalyzer(analysisExecutor) { image ->
                                        processStepFrame(image, detector, embeddingHelper, fisheyeCorrector) { embedding, angleId ->
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
        
        Spacer(Modifier.height(32.dp))
        
        Text(instruction, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.White, textAlign = TextAlign.Center)
        
        Spacer(Modifier.height(48.dp))
        
        // Coverage Dots Grid
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("ANGLE COVERAGE", fontSize = 10.sp, color = TextSecondary, letterSpacing = 2.sp)
            Spacer(Modifier.height(16.dp))
            CoverageGrid(coveragePoints)
        }
    }
}

@Composable
fun CoverageGrid(points: Map<Int, Boolean>) {
    // 3x3 Grid representation
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { col ->
                    val id = row * 3 + col
                    val active = points[id] ?: false
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (active) AlertGreen else Color.White.copy(alpha = 0.1f))
                            .border(1.dp, if (active) AlertGreen else Color.White.copy(alpha = 0.2f), CircleShape)
                    )
                }
            }
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
        Text("Building Owner Profile...", color = Color.White)
        Text("Normalizing embeddings and encrypting data", fontSize = 12.sp, color = TextSecondary)
    }
}

@Composable
fun SuccessStep(name: String, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.CheckCircle, null, tint = AlertGreen, modifier = Modifier.size(80.dp))
        Spacer(Modifier.height(32.dp))
        Text("Enrollment Complete", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(12.dp))
        Text("Welcome, $name. Your AI now recognizes you with 98% confidence.", textAlign = TextAlign.Center, color = TextSecondary)
        
        Spacer(Modifier.height(48.dp))
        
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
        ) {
            Text("Enter Dashboard", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@SuppressLint("UnsafeOptInUsageError")
private fun processStepFrame(
    image: ImageProxy,
    detector: com.google.mlkit.vision.face.FaceDetector,
    helper: FaceEmbeddingHelper,
    corrector: FisheyeCorrector,
    onResult: (FloatArray, Int) -> Unit
) {
    val raw = try {
        val original = image.toBitmap()
        val matrix = android.graphics.Matrix().apply { 
            // Mirror front camera (Standard toBitmap handles rotation)
            postScale(-1f, 1f) 
        }
        Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
    } catch (e: Exception) { image.close(); return }

    val corrected = try { corrector.correct(raw) } catch (e: Exception) { raw }
    val input = InputImage.fromBitmap(corrected, 0)

    detector.process(input)
        .addOnSuccessListener { faces ->
            if (faces.isNotEmpty()) {
                val face = faces[0]
                val box = face.boundingBox
                
                val rotY = face.headEulerAngleY 
                val rotX = face.headEulerAngleX 
                
                val col = when {
                    rotY < -15 -> 0
                    rotY > 15 -> 2
                    else -> 1
                }
                val row = when {
                    rotX < -15 -> 0
                    rotX > 15 -> 2
                    else -> 1
                }
                val angleId = row * 3 + col

                try {
                    val crop = Bitmap.createBitmap(corrected, box.left.coerceAtLeast(0), box.top.coerceAtLeast(0), box.width().coerceAtLeast(1), box.height().coerceAtLeast(1))
                    onResult(helper.getEmbedding(crop), angleId)
                } catch (e: Exception) {}
            }
        }
        .addOnCompleteListener { image.close() }
}
