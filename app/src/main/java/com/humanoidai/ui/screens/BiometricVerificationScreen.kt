package com.humanoidai.ui.screens

import android.annotation.SuppressLint
import androidx.biometric.BiometricPrompt
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import android.content.Context
import android.content.ContextWrapper
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.humanoidai.ml.*
import com.humanoidai.navigation.NavRoutes
import com.humanoidai.security.TrustFramework
import com.humanoidai.security.UserAccessLevel
import com.humanoidai.ui.components.WithCameraPermission
import com.humanoidai.ui.theme.*
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

fun Context.findActivity(): FragmentActivity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun BiometricVerificationScreen(
    navController: NavController,
    ownerManager: OwnerEnrollmentManager,
    recognitionManager: FaceRecognitionManager,
    onAccessGranted: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val embeddingHelper = remember { FaceEmbeddingHelper(context) }
    val enrollmentManager = remember { FaceEnrollmentManager(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val detector = remember { com.google.mlkit.vision.face.FaceDetection.getClient() }
    
    var statusText by remember { mutableStateOf("SCANNING BIOMETRICS...") }
    var subStatus by remember { mutableStateOf("Position your face for scanning") }
    var isVerifying by remember { mutableStateOf(false) }
    var showPasswordInput by remember { mutableStateOf(false) }
    var faceScanActive by remember { mutableStateOf(true) }

    val showFingerprint = {
        val activity = context.findActivity()
        if (activity != null) {
            val executor = ContextCompat.getMainExecutor(context)
            val biometricPrompt = BiometricPrompt(activity, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                            subStatus = errString.toString()
                        }
                        faceScanActive = true // Re-enable face scan if fingerprint fails
                    }

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        if (!isVerifying) {
                            isVerifying = true
                            faceScanActive = false
                            statusText = "FINGERPRINT VERIFIED"
                            subStatus = "OWNER ACCESS GRANTED"
                            onAccessGranted()
                        }
                    }
                })

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Security Check")
                .setSubtitle("Authenticate using fingerprint")
                .setNegativeButtonText("Back to Face Scan")
                .build()

            biometricPrompt.authenticate(promptInfo)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "scan_pulse")
    val scanAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "scan_alpha"
    )

    LaunchedEffect(Unit) {
        // Load Owner and all secondary faces for differentiation
        enrollmentManager.loadAllInto(recognitionManager, ownerManager)
        
        // Wait 3 seconds for Face Scan before showing Fingerprint popup
        delay(3000)
        if (!isVerifying && !showPasswordInput) {
            showFingerprint()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
            detector.close()
            embeddingHelper.close()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "SYSTEM GATEWAY",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = AccentCyan,
                letterSpacing = 2.sp
            )
            
            Spacer(Modifier.height(40.dp))

            WithCameraPermission {
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(CircleShape)
                        .border(2.dp, if (isVerifying) SuccessGreen else AccentCyan, CircleShape),
                    contentAlignment = Alignment.Center
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
                                        .build()
                                    
                                    analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                        if (isVerifying || showPasswordInput || !faceScanActive) { 
                                            imageProxy.close()
                                            return@setAnalyzer 
                                        }

                                        val bitmap = imageProxy.toBitmap()
                                        val matrix = android.graphics.Matrix()
                                        matrix.postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                                        matrix.postScale(-1f, 1f)
                                        val rotated = android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                                        
                                        val input = com.google.mlkit.vision.common.InputImage.fromBitmap(rotated, 0)
                                        detector.process(input)
                                            .addOnSuccessListener { faces ->
                                                if (faces.isNotEmpty()) {
                                                    val face = faces[0]
                                                    val box = face.boundingBox
                                                    try {
                                                        val crop = android.graphics.Bitmap.createBitmap(rotated, box.left.coerceAtLeast(0), box.top.coerceAtLeast(0), box.width().coerceAtLeast(1), box.height().coerceAtLeast(1))
                                                        val embedding = embeddingHelper.getEmbedding(crop)
                                                        val (name, confidence) = recognitionManager.findMatch(embedding)

                                                        if (name != "UNKNOWN" && !name.startsWith("PROBABLE_")) {
                                                            isVerifying = true
                                                            faceScanActive = false
                                                            statusText = "USER VERIFIED"
                                                            subStatus = "SYSTEM ACCESS GRANTED"
                                                            onAccessGranted()
                                                        }
                                                    } catch (e: Exception) {}
                                                }
                                            }
                                            .addOnCompleteListener { imageProxy.close() }
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
                    
            if (isVerifying) {
                Box(Modifier.fillMaxSize().background(SuccessGreen.copy(alpha = 0.2f)))
            } else if (faceScanActive) {
                // Pulsing Scan Ring
                Box(
                    Modifier
                        .fillMaxSize()
                        .border(8.dp, AccentCyan.copy(alpha = scanAlpha), CircleShape)
                )
            }
            
            if (showPasswordInput) {
                Box(
                    modifier = Modifier.fillMaxSize().background(BackgroundDark.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        var pass by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = pass,
                            onValueChange = { pass = it },
                            label = { Text("Master Password") },
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (pass == "admin") { // Replace with real check
                                    onAccessGranted()
                                } else {
                                    subStatus = "Incorrect Password"
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Verify Access")
                        }
                        TextButton(onClick = { showPasswordInput = false }) {
                            Text("Cancel", color = AccentCyan)
                        }
                    }
                }
            }
        }
    }

            Spacer(Modifier.height(48.dp))

            Text(
                statusText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Text(
                subStatus,
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(60.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { showFingerprint() },
                    modifier = Modifier.size(64.dp).background(SurfaceDark, CircleShape).border(1.dp, AccentCyan.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(Icons.Default.Fingerprint, null, tint = AccentCyan, modifier = Modifier.size(32.dp))
                }
                
                IconButton(
                    onClick = { showPasswordInput = true },
                    modifier = Modifier.size(64.dp).background(SurfaceDark, CircleShape).border(1.dp, AccentCyan.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(Icons.Default.Lock, null, tint = AccentCyan, modifier = Modifier.size(24.dp))
                }

                IconButton(
                    onClick = { 
                        // Sign out and go back to login
                        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                        navController.navigate(NavRoutes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    modifier = Modifier.size(64.dp).background(SurfaceDark, CircleShape).border(1.dp, Color.Red.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(Icons.Default.ExitToApp, null, tint = Color.Red, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}
