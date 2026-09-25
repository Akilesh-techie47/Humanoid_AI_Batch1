package com.humanoidai.ui.screens

import android.annotation.SuppressLint
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
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
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.humanoidai.hearing.SpeechRecognizerManager
import com.humanoidai.ml.FacePreprocessor

import com.humanoidai.ml.FaceEmbeddingHelper
import com.humanoidai.ml.FaceEnrollmentManager
import com.humanoidai.ml.OwnerEnrollmentManager
import com.humanoidai.ml.FaceRecognitionManager
import com.humanoidai.ml.FisheyeCorrector
import com.humanoidai.vision.FrameEnhancer
import com.humanoidai.ui.components.ArmsunFooter
import com.humanoidai.ui.components.SidePanelDrawer
import com.humanoidai.ui.components.WithCameraPermission
import com.humanoidai.ui.theme.*
import com.humanoidai.voice.VoiceEngine
import kotlinx.coroutines.launch
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
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
    val drawerState    = rememberDrawerState(initialValue = DrawerValue.Closed)
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

    SidePanelDrawer(
        navController = navController,
        drawerState = drawerState
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "FACE ENROLLMENT", 
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, 
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, "Menu", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (step) {
                        EnrollStep.ENTER_NAME -> {
                            Spacer(modifier = Modifier.height(32.dp))
                            Text("Who are you enrolling?", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(24.dp))
                            AuraTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = "Full Name",
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
                                        Icon(if (isListening) Icons.Default.MicOff else Icons.Default.Mic, null, tint = if (isListening) Color.Red else MaterialTheme.colorScheme.primary)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Relationship", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                labels.forEach { l ->
                                    val selected = label == l
                                    Box(
                                        modifier = Modifier
                                            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                            .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                            .weight(1f, fill = false)
                                            .clickable { label = l },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(l, style = MaterialTheme.typography.labelMedium, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                            AuraButton(
                                text = "Continue",
                                onClick = {
                                    if (name.isNotBlank()) {
                                        step = EnrollStep.CAPTURE
                                    }
                                },
                                enabled = name.isNotBlank()
                            )
                        }
                        EnrollStep.CAPTURE -> {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = Color(0xEE0F172A),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "ENROLLING: ${name.uppercase()}",
                                            style = TextStyle(
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = AccentCyan
                                            )
                                        )
                                        Text(
                                            "INSTRUCTION: ${currentPose.name}",
                                            style = TextStyle(
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                fontFamily = FontFamily.Monospace,
                                                color = TextSecondary
                                            )
                                        )
                                    }
                                    Text(
                                        "${capturedEmbeddings.size} / 60",
                                        style = TextStyle(
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            color = SuccessGreen
                                        )
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            WithCameraPermission {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(340.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    color = Color.Black,
                                    border = BorderStroke(1.5.dp, Brush.horizontalGradient(
                                        listOf(AccentCyan, AccentPurple, AccentCyan)
                                    )),
                                    tonalElevation = 8.dp,
                                    shadowElevation = 12.dp
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
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
                                                            Log.i("EnrollmentScreen", "Camera bound successfully")
                                                        } catch (e: Exception) {
                                                            Log.e("EnrollmentScreen", "Failed to bind camera", e)
                                                        }
                                                    }, ContextCompat.getMainExecutor(ctx))
                                                }
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        
                                        // Face guide oval overlay
                                        Box(
                                            modifier = Modifier
                                                .size(width = 180.dp, height = 230.dp)
                                                .align(Alignment.Center)
                                                .border(2.dp, Brush.sweepGradient(listOf(AccentCyan, AccentPurple, AccentCyan)), CircleShape)
                                        )
                                        
                                        // Top status pill
                                        Surface(
                                            modifier = Modifier
                                                .align(Alignment.TopCenter)
                                                .padding(top = 12.dp),
                                            color = Color(0xCC070A14),
                                            shape = RoundedCornerShape(20.dp),
                                            border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.4f))
                                        ) {
                                            Text(
                                                text = when(currentPose) {
                                                    FacePose.FRONT -> "LOOK STRAIGHT"
                                                    FacePose.LEFT -> "TURN SLIGHTLY LEFT"
                                                    FacePose.RIGHT -> "TURN SLIGHTLY RIGHT"
                                                    FacePose.UP -> "LOOK SLIGHTLY UP"
                                                    FacePose.DOWN -> "LOOK SLIGHTLY DOWN"
                                                    FacePose.NATURAL -> "LOOK NATURALLY"
                                                },
                                                style = TextStyle(
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = AccentCyan
                                                ),
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Sample progress indicator
                            LinearProgressIndicator(
                                progress = { faceProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = AccentCyan,
                                trackColor = Color(0xFF1E293B)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                "Hold still for pose verification...",
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextSecondary
                                )
                            )
                        }
                        EnrollStep.SUCCESS -> {
                            Spacer(modifier = Modifier.height(80.dp))
                            Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(72.dp))
                            Spacer(modifier = Modifier.height(20.dp))
                            Text("$name enrolled!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("This person will now be recognized\nautomatically by the camera.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(40.dp))
                            AuraButton(text = "Done", onClick = { navController.popBackStack() })
                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(onClick = {
                                name = ""
                                faceProgress = 0f
                                currentPose = FacePose.FRONT
                                poseSamples = 0
                                capturedEmbeddings.clear()
                                step = EnrollStep.ENTER_NAME
                            }) {
                                Text("Enroll Another Person", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                
                ArmsunFooter(modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}
