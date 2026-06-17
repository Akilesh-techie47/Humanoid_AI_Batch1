package com.humanoidai.ui.screens

import android.graphics.RectF
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
fun EnvironmentScreen(navController: NavController) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // ---- ML pipeline setup ----
    val embeddingHelper    = remember { FaceEmbeddingHelper(context) }
    val recognitionManager = remember { FaceRecognitionManager() }
    val analysisExecutor   = remember { Executors.newSingleThreadExecutor() }

    // Live detected persons — updated by FaceAnalyzer on every processed frame
    var detectedPersons by remember { mutableStateOf<List<DetectedPerson>>(emptyList()) }

    // Reference to RoiOverlayView so we can push updates to it
    var roiOverlayView by remember { mutableStateOf<RoiOverlayView?>(null) }

    // Keep overlay in sync with detectedPersons
    LaunchedEffect(detectedPersons) {
        roiOverlayView?.updatePersons(detectedPersons)
    }

    // Clean up executor on dispose
    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
            embeddingHelper.close()
        }
    }

    // Primary person (first/largest detected face)
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
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "${detectedPersons.size} face(s)",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
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
                    // CameraX Preview + ImageAnalysis
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).also { previewView ->
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                cameraProviderFuture.addListener({
                                    val cameraProvider = cameraProviderFuture.get()

                                    // Use case 1: Preview
                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }

                                    // Use case 2: ImageAnalysis for face detection
                                    val imageAnalysis = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                                        .build()
                                        .also { analysis ->
                                            analysis.setAnalyzer(
                                                analysisExecutor,
                                                FaceAnalyzer(
                                                    embeddingHelper    = embeddingHelper,
                                                    recognitionManager = recognitionManager,
                                                    onResults          = { persons ->
                                                        detectedPersons = persons
                                                    }
                                                )
                                            )
                                        }

                                    try {
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            CameraSelector.DEFAULT_BACK_CAMERA,
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

                    // ROI Overlay on top of camera
                    AndroidView(
                        factory = { ctx ->
                            RoiOverlayView(ctx).also { view ->
                                roiOverlayView = view
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Corner label
                    Text(
                        "FISHEYE · CORRECTED",
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
