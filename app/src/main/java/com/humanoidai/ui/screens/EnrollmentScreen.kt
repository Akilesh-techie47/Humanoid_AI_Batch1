package com.humanoidai.ui.screens

import android.annotation.SuppressLint
import android.graphics.RectF
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
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
import com.humanoidai.ml.FaceRecognitionManager
import com.humanoidai.ui.components.WithCameraPermission
import com.humanoidai.ui.theme.*
import java.util.concurrent.Executors

// -----------------------------------------------------------------
// EnrollmentScreen
// -----------------------------------------------------------------
// Guides user to:
//  1. Enter person name + label
//  2. Point camera at their face
//  3. Capture 5 frames automatically
//  4. Save averaged embedding via FaceEnrollmentManager
// -----------------------------------------------------------------

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun EnrollmentScreen(
    navController: NavController,
    enrollmentManager: FaceEnrollmentManager,
    recognitionManager: FaceRecognitionManager
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // State
    var name          by remember { mutableStateOf("") }
    var label         by remember { mutableStateOf("Family") }
    var step          by remember { mutableStateOf(EnrollStep.ENTER_NAME) }
    var captureCount  by remember { mutableIntStateOf(0) }
    var statusMessage by remember { mutableStateOf("") }

    val capturedEmbeddings = remember { mutableListOf<FloatArray>() }
    val embeddingHelper    = remember { FaceEmbeddingHelper(context) }
    val analysisExecutor   = remember { Executors.newSingleThreadExecutor() }
    val detector           = remember {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setMinFaceSize(0.2f)
                .build()
        )
    }

    val targetCaptures = 8
    val labels = listOf("Family", "Colleague", "Neighbor", "Friend", "Other")

    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
            embeddingHelper.close()
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundDark)
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Text(
                    "Enroll New Person",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }
        },
        containerColor = BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (step) {

                // ---- Step 1: Enter name + label ----
                EnrollStep.ENTER_NAME -> {
                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        "Who are you enrolling?",
                        fontSize = 16.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )

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
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Label selector
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
                                    .background(
                                        if (selected) AccentCyan else SurfaceDark,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (selected) AccentCyan else TextSecondary.copy(alpha = 0.3f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .weight(1f, fill = false),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    l,
                                    fontSize = 11.sp,
                                    color = if (selected) Color.Black else TextSecondary,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
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

                // ---- Step 2: Camera capture ----
                EnrollStep.CAPTURE -> {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Enrolling: $name",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentCyan
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Camera preview
                    WithCameraPermission {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                                .background(Color(0xFF060610), RoundedCornerShape(12.dp))
                                .border(
                                    2.dp,
                                    if (captureCount >= targetCaptures) Color(0xFF66BB6A)
                                    else AccentCyan.copy(alpha = 0.5f),
                                    RoundedCornerShape(12.dp)
                                )
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    PreviewView(ctx).also { previewView ->
                                        val future = ProcessCameraProvider.getInstance(ctx)
                                        future.addListener({
                                            val provider = future.get()

                                            val preview = Preview.Builder().build().also {
                                                it.surfaceProvider = previewView.surfaceProvider
                                            }

                                            val imageAnalysis = ImageAnalysis.Builder()
                                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                                                .build()

                                            imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                                if (captureCount < targetCaptures) {
                                                    processEnrollmentFrame(
                                                        imageProxy      = imageProxy,
                                                        detector        = detector,
                                                        embeddingHelper = embeddingHelper,
                                                        onEmbedding     = { embedding ->
                                                            capturedEmbeddings.add(embedding)
                                                            captureCount++
                                                            statusMessage = "Captured $captureCount/$targetCaptures frames..."
                                                            if (captureCount >= targetCaptures) {
                                                                // Save enrollment
                                                                enrollmentManager.enrollPerson(
                                                                    name       = name,
                                                                    label      = label,
                                                                    embeddings = capturedEmbeddings
                                                                )
                                                                enrollmentManager.loadAllInto(recognitionManager)
                                                                step = EnrollStep.SUCCESS
                                                            }
                                                        }
                                                    )
                                                } else {
                                                    imageProxy.close()
                                                }
                                            }

                                            try {
                                                provider.unbindAll()
                                                provider.bindToLifecycle(
                                                    lifecycleOwner,
                                                    CameraSelector.DEFAULT_FRONT_CAMERA,
                                                    preview,
                                                    imageAnalysis
                                                )
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }, ContextCompat.getMainExecutor(ctx))
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            // Face guide overlay
                            Box(
                                modifier = Modifier
                                    .size(180.dp)
                                    .align(Alignment.Center)
                                    .border(
                                        2.dp,
                                        if (captureCount > 0) Color(0xFF66BB6A) else AccentCyan,
                                        RoundedCornerShape(90.dp)
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress bar
                    LinearProgressIndicator(
                        progress = { captureCount.toFloat() / targetCaptures },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = AccentCyan,
                        trackColor = SurfaceDark
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        statusMessage,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Keep your face centered in the circle.\nFront camera will be used automatically.",
                        fontSize = 12.sp,
                        color = TextSecondary.copy(alpha = 0.6f),
                        lineHeight = 18.sp
                    )
                }

                // ---- Step 3: Success ----
                EnrollStep.SUCCESS -> {
                    Spacer(modifier = Modifier.height(80.dp))

                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF66BB6A),
                        modifier = Modifier.size(72.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        "$name enrolled!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "This person will now be recognized\nautomatically by the camera.",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    Button(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                    ) {
                        Text("Done", color = Color.Black, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = {
                        // Enroll another person
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

// -----------------------------------------------------------------
// Enrollment step enum
// -----------------------------------------------------------------
enum class EnrollStep { ENTER_NAME, CAPTURE, SUCCESS }

// -----------------------------------------------------------------
// Frame processor for enrollment
// -----------------------------------------------------------------
@SuppressLint("UnsafeOptInUsageError")
private fun processEnrollmentFrame(
    imageProxy: ImageProxy,
    detector: com.google.mlkit.vision.face.FaceDetector,
    embeddingHelper: FaceEmbeddingHelper,
    onEmbedding: (FloatArray) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        return
    }

    val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

    detector.process(inputImage)
        .addOnSuccessListener { faces ->
            if (faces.isNotEmpty()) {
                val face = faces[0]
                val bitmap = imageProxy.toBitmap()
                val box    = face.boundingBox
                val rotation = imageProxy.imageInfo.rotationDegrees

                val left   = box.left.coerceAtLeast(0)
                val top    = box.top.coerceAtLeast(0)
                val right  = box.right.coerceAtMost(bitmap.width)
                val bottom = box.bottom.coerceAtMost(bitmap.height)
                val w      = (right - left).coerceAtLeast(1)
                val h      = (bottom - top).coerceAtLeast(1)

                val crop = android.graphics.Bitmap.createBitmap(bitmap, left, top, w, h)

                // Rotate crop to upright and flip if front camera to get canonical face
                val matrix = android.graphics.Matrix()
                if (rotation != 0) {
                    matrix.postRotate(rotation.toFloat())
                }
                // Enrollment currently uses front camera by default
                matrix.postScale(-1f, 1f) 

                val finalFace = android.graphics.Bitmap.createBitmap(crop, 0, 0, crop.width, crop.height, matrix, true)
                val embedding = embeddingHelper.getEmbedding(finalFace)
                onEmbedding(embedding)
            }
        }
        .addOnFailureListener { /* silent fail */ }
        .addOnCompleteListener { imageProxy.close() }
}
