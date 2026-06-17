package com.humanoidai.ui.screens

import android.content.Context
import android.view.Surface
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.*
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.humanoidai.ml.FaceAnalyzer
import com.humanoidai.ml.FaceEmbeddingHelper
import com.humanoidai.ml.FaceRecognitionManager
import com.humanoidai.ui.components.*
import com.humanoidai.ui.theme.*
import java.util.concurrent.Executors

@Composable
fun EnvironmentScreen(navController: NavController, recognitionManager: FaceRecognitionManager) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // ML pipeline
    val embeddingHelper  = remember { FaceEmbeddingHelper(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    // Camera state
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager

// Track device rotation
    var deviceRotation by remember {
        mutableStateOf(windowManager.defaultDisplay.rotation)
    }
    // Detection state
    var detectedPersons by remember { mutableStateOf<List<DetectedPerson>>(emptyList()) }
    var roiOverlayView  by remember { mutableStateOf<RoiOverlayView?>(null) }
    val faceAnalyzer = remember {
        FaceAnalyzer(
            embeddingHelper    = embeddingHelper,
            recognitionManager = recognitionManager,
            onResults          = { persons -> detectedPersons = persons }
        )
    }
    // Push detections to overlay
    LaunchedEffect(detectedPersons) {
        roiOverlayView?.updatePersons(detectedPersons)
    }

    // Rebind camera when lens or previewView changes
    LaunchedEffect(lensFacing, previewView) {
        val pv = previewView ?: return@LaunchedEffect
        faceAnalyzer.isFrontCamera = lensFacing == CameraSelector.LENS_FACING_FRONT

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .setTargetRotation(Surface.ROTATION_0)
                .build()
                .also { it.setSurfaceProvider(pv.surfaceProvider) }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .setTargetRotation(Surface.ROTATION_0)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(
                        analysisExecutor,
                        faceAnalyzer
                    )
                }

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        val orientationListener = object : android.view.OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                val rotation = when {
                    orientation <= 45  || orientation > 315 -> Surface.ROTATION_0
                    orientation in 46..134                  -> Surface.ROTATION_270
                    orientation in 135..224                 -> Surface.ROTATION_180
                    else                                    -> Surface.ROTATION_90
                }
                faceAnalyzer.deviceRotation = rotation
            }
        }
        orientationListener.enable()
        onDispose {
            orientationListener.disable()
            analysisExecutor.shutdown()
            embeddingHelper.close()
        }
    }

    val primaryPerson = detectedPersons.firstOrNull { it.isPrimary }

    Scaffold(
        bottomBar = { BottomNavBar(navController) },
        containerColor = BackgroundDark
    ) { padding ->
        WithCameraPermission {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                // ---- Header ----
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ROI Monitor", fontSize = 20.sp, color = TextPrimary)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(AlertGreen, shape = RoundedCornerShape(4.dp))
                        )
                        Text("LIVE", fontSize = 11.sp, color = AlertGreen)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "${detectedPersons.size} face(s)",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        // Camera switch button
                        IconButton(
                            onClick = {
                                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                                    CameraSelector.LENS_FACING_FRONT
                                else
                                    CameraSelector.LENS_FACING_BACK
                                detectedPersons = emptyList()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "Switch Camera",
                                tint = AccentCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ---- Camera + ROI overlay ----
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(Color(0xFF060610), RoundedCornerShape(12.dp))
                ) {
                    // CameraX PreviewView
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).also { pv ->
                                previewView = pv
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // ROI Overlay
                    AndroidView(
                        factory = { ctx ->
                            RoiOverlayView(ctx).also { view ->
                                roiOverlayView = view
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Camera label
                    Text(
                        if (lensFacing == CameraSelector.LENS_FACING_BACK)
                            "REAR · FISHEYE CORRECTED"
                        else
                            "FRONT · FACE MODE",
                        color = AccentCyan.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))

                // ---- Primary subject card ----
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1F1E)),
                    border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.3f))
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Primary Focus Subject", fontSize = 11.sp, color = TextSecondary)
                            Text(
                                if (detectedPersons.isEmpty()) "NO FACE" else "ROI LOCKED",
                                fontSize = 10.sp,
                                color = if (detectedPersons.isEmpty()) TextSecondary else AccentCyan
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        if (primaryPerson != null) {
                            val isUnknown = primaryPerson.name == "UNKNOWN"
                            Text(
                                if (isUnknown) "Unknown Person" else primaryPerson.name,
                                fontSize = 16.sp,
                                color = if (isUnknown) Color(0xFFFF5C5C) else AccentCyan
                            )
                            Spacer(Modifier.height(6.dp))

                            Text(
                                "Confidence · ${(primaryPerson.confidence * 100).toInt()}%",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { primaryPerson.confidence },
                                modifier = Modifier.fillMaxWidth().height(5.dp),
                                color = if (isUnknown) Color(0xFFFF5C5C) else AccentCyan,
                                trackColor = Color(0xFF1E2436)
                            )
                            Spacer(Modifier.height(6.dp))

                            Text(
                                "Faces detected · ${detectedPersons.size}",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { (detectedPersons.size / 5f).coerceAtMost(1f) },
                                modifier = Modifier.fillMaxWidth().height(5.dp),
                                color = AlertOrange,
                                trackColor = Color(0xFF1E2436)
                            )
                        } else {
                            Text(
                                "Scanning for faces...",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
