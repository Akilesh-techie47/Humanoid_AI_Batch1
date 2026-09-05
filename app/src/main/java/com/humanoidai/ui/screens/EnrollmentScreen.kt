package com.humanoidai.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
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
import com.humanoidai.ml.FacePreprocessor

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
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.util.concurrent.Executors
import kotlin.math.abs

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
    var faceProgress  by remember { mutableFloatStateOf(0f) }
    var currentPose   by remember { mutableStateOf(FacePose.FRONT) }
    var poseSamples   by remember { mutableIntStateOf(0) }
    val SAMPLES_PER_POSE = 3
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
    val fisheyeCorrector   = remember { FisheyeCorrector() }
    val frameEnhancer      = remember { FrameEnhancer() }
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
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Enrolling: $name", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AccentCyan)
                        Text("Pose: ${currentPose.name}", fontSize = 13.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.height(12.dp))
                        WithCameraPermission {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(320.dp)
                                    .background(Color(0xFF060610), RoundedCornerShape(12.dp))
                                    .border(2.dp, AccentCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
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
                                                    processEnrollmentFrame(imageProxy, detector, embeddingHelper, fisheyeCorrector, frameEnhancer) { embedding, pose ->
                                                        if (pose == currentPose) {
                                                            capturedEmbeddings.add(embedding)
                                                            poseSamples++
                                                            faceProgress = capturedEmbeddings.size / totalTarget.toFloat()
                                                            
                                                            if (poseSamples >= SAMPLES_PER_POSE) {
                                                                val nextPoseIndex = currentPose.ordinal + 1
                                                                if (nextPoseIndex < FacePose.entries.size) {
                                                                    currentPose = FacePose.entries[nextPoseIndex]
                                                                    poseSamples = 0
                                                                } else {
                                                                    enrollmentManager.enrollPerson(name, label, capturedEmbeddings)
                                                                    enrollmentManager.loadAllInto(recognitionManager, ownerManager)
                                                                    step = EnrollStep.SUCCESS
                                                                }
                                                            }
                                                        }
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
                                Box(modifier = Modifier.size(180.dp).align(Alignment.Center).border(2.dp, AccentCyan, RoundedCornerShape(90.dp)))
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(progress = { faceProgress }, modifier = Modifier.fillMaxWidth().height(6.dp), color = AccentCyan, trackColor = SurfaceDark)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Hold still for current pose...", fontSize = 13.sp, color = TextSecondary)
                    }
                    EnrollStep.SUCCESS -> {
                        Spacer(modifier = Modifier.height(80.dp))
                        Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(72.dp))
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
                            faceProgress = 0f
                            currentPose = FacePose.FRONT
                            poseSamples = 0
                            capturedEmbeddings.clear()
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
    onResult: (FloatArray, FacePose) -> Unit
) {
    val rotation = imageProxy.imageInfo.rotationDegrees
    val original = try {
        val bitmap = imageProxy.toBitmap()
        val matrix = android.graphics.Matrix()
        matrix.postRotate(rotation.toFloat())
        matrix.postScale(-1f, 1f)
        android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } catch (e: Exception) {
        imageProxy.close()
        return
    }

    val processedBitmap = if (FisheyeCorrector.isOpenCVLoaded) {
        try {
            val corrected = fisheyeCorrector.correct(original)
            val mat = Mat()
            Utils.bitmapToMat(corrected, mat)
            val bgr = Mat()
            Imgproc.cvtColor(mat, bgr, Imgproc.COLOR_RGBA2BGR)
            val enhancedBgr = frameEnhancer.enhance(bgr)
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
                
                val pose = when {
                    yaw < -15f -> FacePose.LEFT
                    yaw > 15f -> FacePose.RIGHT
                    pitch > 15f -> FacePose.UP
                    pitch < -15f -> FacePose.DOWN
                    abs(yaw) < 10f && abs(pitch) < 10f -> FacePose.FRONT
                    else -> FacePose.NATURAL
                }

                try {
                    val isolated = FacePreprocessor.alignAndIsolate(processedBitmap, face)
                    onResult(embeddingHelper.getEmbedding(isolated), pose)
                } catch (e: Exception) {
                    Log.e("Enrollment", "Frame processing failed: ${e.message}")
                }
            }
        }
        .addOnCompleteListener { imageProxy.close() }
}