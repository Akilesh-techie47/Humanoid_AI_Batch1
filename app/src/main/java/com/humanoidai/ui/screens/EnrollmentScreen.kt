package com.humanoidai.ui.screens

import android.annotation.SuppressLint
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.humanoidai.ml.FaceEnrollmentManager
import com.humanoidai.ml.OwnerEnrollmentManager
import com.humanoidai.ml.FaceRecognitionManager
import com.humanoidai.ml.FisheyeCorrector
import com.humanoidai.vision.FrameEnhancer
import com.humanoidai.ui.components.SidePanelDrawer
import com.humanoidai.ui.components.WithCameraPermission
import com.humanoidai.ui.theme.*
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@SuppressLint("UnsafeOptInUsageError")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnrollmentScreen(
    navController: NavController,
    enrollmentManager: FaceEnrollmentManager,
    ownerManager: OwnerEnrollmentManager,
    recognitionManager: FaceRecognitionManager,
    microphoneManager: com.humanoidai.hearing.SpeechRecognizerManager,
    voiceEngine: com.humanoidai.voice.VoiceEngine
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val drawerState    = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope          = rememberCoroutineScope()

    var name          by remember { mutableStateOf("") }
    var label         by remember { mutableStateOf("Family") }
    var step          by remember { mutableStateOf(EnrollStep.ENTER_NAME) }
    var captureCount  by remember { mutableIntStateOf(0) }
    var currentAngle  by remember { mutableIntStateOf(0) }
    var statusMessage by remember { mutableStateOf("") }
    val isListening   by microphoneManager.isListening.collectAsState()

    val angles = listOf("Center", "Tilt Up", "Tilt Down", "Look Left", "Look Right")
    val targetCapturesPerAngle = 3
    val totalTarget = angles.size * targetCapturesPerAngle

    LaunchedEffect(step) {
        when(step) {
            EnrollStep.ENTER_NAME -> voiceEngine.speak("Who are you enrolling today?")
            EnrollStep.CAPTURE -> voiceEngine.speak("Position the person's face. We'll capture a few angles.")
            EnrollStep.SUCCESS -> voiceEngine.speak("$name has been enrolled successfully.")
        }
    }

    val capturedEmbeddings = remember { mutableListOf<FloatArray>() }
    val embeddingHelper    = remember { FaceEmbeddingHelper(context) }
    val fisheyeCorrector   = remember { FisheyeCorrector() }
    val frameEnhancer      = remember { FrameEnhancer() }
    val analysisExecutor   = remember { Executors.newSingleThreadExecutor() }
    val detector           = remember {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setMinFaceSize(0.15f)
                .build()
        )
    }

    val labels = listOf("Family", "Colleague", "Neighbor", "Friend", "Other")

    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
            embeddingHelper.close()
            fisheyeCorrector.release()
            detector.close()
        }
    }

    SidePanelDrawer(
        navController = navController,
        drawerState = drawerState
    ) {
        Scaffold(
            containerColor = BackgroundDark,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text("FACE ENROLLMENT", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccentCyan)
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, "Menu", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (step) {
                    EnrollStep.ENTER_NAME -> {
                        Spacer(modifier = Modifier.height(32.dp))
                        Text("Who are you enrolling?", fontSize = 16.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(24.dp))
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Full Name", color = TextSecondary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = AccentCyan,
                                unfocusedBorderColor = TextSecondary,
                                cursorColor = AccentCyan,
                                focusedContainerColor = SurfaceDark,
                                unfocusedContainerColor = SurfaceDark
                            ),
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                IconButton(onClick = {
                                    if (isListening) {
                                        microphoneManager.stopListening()
                                    } else {
                                        microphoneManager.startListening(onFinalResult = { name = it })
                                    }
                                }) {
                                    Icon(if (isListening) Icons.Default.MicOff else Icons.Default.Mic, null, tint = if (isListening) Color.Red else AccentCyan)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Relationship", fontSize = 13.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            labels.forEach { l ->
                                val selected = label == l
                                Box(
                                    modifier = Modifier
                                        .background(if (selected) AccentCyan else SurfaceDark, RoundedCornerShape(8.dp))
                                        .border(1.dp, if (selected) AccentCyan else TextSecondary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .weight(1f, fill = false)
                                        .clickable { label = l },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(l, fontSize = 11.sp, color = if (selected) Color.Black else TextSecondary, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    step = EnrollStep.CAPTURE
                                    statusMessage = "Position face in frame and hold still..."
                                }
                            },
                            enabled = name.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                        ) {
                            Text("Continue", color = Color.Black, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    EnrollStep.CAPTURE -> {
                        val angleName = angles[currentAngle]
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Enrolling: $name", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AccentCyan)
                        Text("Angle: $angleName", fontSize = 13.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.height(12.dp))
                        WithCameraPermission {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(320.dp)
                                    .background(Color(0xFF060610), RoundedCornerShape(12.dp))
                                    .border(2.dp, if (captureCount >= totalTarget) Color(0xFF66BB6A) else AccentCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            ) {
                                AndroidView(
                                    factory = { ctx ->
                                        PreviewView(ctx).also { previewView ->
                                            val future = ProcessCameraProvider.getInstance(ctx)
                                            future.addListener({
                                                val provider = future.get()
                                                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                                                val imageAnalysis = ImageAnalysis.Builder()
                                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                                                    .build()
                                                imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                                    if (captureCount < totalTarget) {
                                                        processEnrollmentFrame(imageProxy, detector, embeddingHelper, fisheyeCorrector, frameEnhancer) { embedding ->
                                                            capturedEmbeddings.add(embedding)
                                                            captureCount++
                                                            
                                                            // Advance angle logic
                                                            if (captureCount % targetCapturesPerAngle == 0 && currentAngle < angles.size - 1) {
                                                                currentAngle++
                                                                voiceEngine.speak("Next: ${angles[currentAngle]}")
                                                            }

                                                            statusMessage = "Captured $captureCount/$totalTarget frames..."
                                                            if (captureCount >= totalTarget) {
                                                                enrollmentManager.enrollPerson(name, label, capturedEmbeddings)
                                                                enrollmentManager.loadAllInto(recognitionManager, ownerManager)
                                                                step = EnrollStep.SUCCESS
                                                            }
                                                        }
                                                    } else {
                                                        imageProxy.close()
                                                    }
                                                }
                                                try {
                                                    provider.unbindAll()
                                                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageAnalysis)
                                                } catch (e: Exception) { e.printStackTrace() }
                                            }, ContextCompat.getMainExecutor(ctx))
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(modifier = Modifier.size(180.dp).align(Alignment.Center).border(2.dp, if (captureCount > 0) Color(0xFF66BB6A) else AccentCyan, RoundedCornerShape(90.dp)))
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(progress = { captureCount.toFloat() / totalTarget }, modifier = Modifier.fillMaxWidth().height(6.dp), color = AccentCyan, trackColor = SurfaceDark)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(statusMessage, fontSize = 13.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Follow the prompts to capture different angles.\nKeep your face centered.", fontSize = 12.sp, color = TextSecondary.copy(alpha = 0.6f), lineHeight = 18.sp)
                    }
                    EnrollStep.SUCCESS -> {
                        Spacer(modifier = Modifier.height(80.dp))
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF66BB6A), modifier = Modifier.size(72.dp))
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("$name enrolled!", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("This person will now be recognized\nautomatically by the camera.", fontSize = 14.sp, color = TextSecondary, lineHeight = 20.sp)
                        Spacer(modifier = Modifier.height(40.dp))
                        Button(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)) {
                            Text("Done", color = Color.Black, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(onClick = {
                            name = ""
                            captureCount = 0
                            capturedEmbeddings.clear()
                            statusMessage = ""
                            step = EnrollStep.ENTER_NAME
                        }) {
                            Text("Enroll Another Person", color = AccentCyan)
                        }
                    }
                }
            }
        }
    }
}

enum class EnrollStep { ENTER_NAME, CAPTURE, SUCCESS }

@SuppressLint("UnsafeOptInUsageError")
private fun processEnrollmentFrame(
    imageProxy: ImageProxy,
    detector: com.google.mlkit.vision.face.FaceDetector,
    embeddingHelper: FaceEmbeddingHelper,
    fisheyeCorrector: FisheyeCorrector,
    frameEnhancer: FrameEnhancer,
    onEmbedding: (FloatArray) -> Unit
) {
    val rotation = imageProxy.imageInfo.rotationDegrees
    val original = try {
        val bitmap = imageProxy.toBitmap()
        val matrix = android.graphics.Matrix()
        
        // Handle Rotation
        matrix.postRotate(rotation.toFloat())
        
        // Front camera mirroring for enrollment (matching UI view)
        matrix.postScale(-1f, 1f)
        
        android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } catch (e: Exception) {
        imageProxy.close()
        return
    }

    val processedBitmap = if (FisheyeCorrector.isOpenCVLoaded) {
        try {
            // 1. Fisheye Correction
            val corrected = fisheyeCorrector.correct(original)
            
            // 2. Enhancement Pipeline (Mirroring FaceAnalyzer)
            val mat = org.opencv.core.Mat()
            org.opencv.android.Utils.bitmapToMat(corrected, mat)
            
            val bgr = org.opencv.core.Mat()
            org.opencv.imgproc.Imgproc.cvtColor(mat, bgr, org.opencv.imgproc.Imgproc.COLOR_RGBA2BGR)
            
            val enhancedBgr = frameEnhancer.enhance(bgr)
            
            val outputRgba = org.opencv.core.Mat()
            org.opencv.imgproc.Imgproc.cvtColor(enhancedBgr, outputRgba, org.opencv.imgproc.Imgproc.COLOR_BGR2RGBA)
            
            val outputBitmap = android.graphics.Bitmap.createBitmap(outputRgba.cols(), outputRgba.rows(), android.graphics.Bitmap.Config.ARGB_8888)
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

    val inputImage = InputImage.fromBitmap(processedBitmap, 0)

    detector.process(inputImage)
        .addOnSuccessListener { faces ->
            if (faces.isNotEmpty()) {
                val face = faces[0]
                val box  = face.boundingBox
                try {
                    val left   = box.left.coerceAtLeast(0)
                    val top    = box.top.coerceAtLeast(0)
                    val right  = box.right.coerceAtMost(processedBitmap.width)
                    val bottom = box.bottom.coerceAtMost(processedBitmap.height)
                    val w      = (right - left).coerceAtLeast(1)
                    val h      = (bottom - top).coerceAtLeast(1)
                    val crop = android.graphics.Bitmap.createBitmap(processedBitmap, left, top, w, h)
                    val embedding = embeddingHelper.getEmbedding(crop)
                    onEmbedding(embedding)
                } catch (e: Exception) {}
            }
        }
        .addOnCompleteListener { imageProxy.close() }
}