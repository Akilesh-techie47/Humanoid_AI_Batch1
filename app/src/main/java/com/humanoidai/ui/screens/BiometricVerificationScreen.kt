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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.core.content.ContextCompat
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
    authViewModel: AuthViewModel,
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
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation"
    )

    val scannerRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "scanner_rotation"
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

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "SYSTEM GATEWAY",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 2.sp
            )
            
            Spacer(Modifier.height(40.dp))

            WithCameraPermission {
                Box(
                    modifier = Modifier
                        .size(320.dp) // Large camera round as requested
                        .clip(CircleShape)
                        .border(2.dp, if (isVerifying) SuccessGreen else MaterialTheme.colorScheme.primary, CircleShape),
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
                // 1. Main Rotating Aura Ring (Slow)
                Canvas(modifier = Modifier.fillMaxSize().rotate(rotation)) {
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

                // 2. NEW: Fast Rotating "Scanning Section" (Laser Sweep)
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
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
                }

                // Pulsing Scan Overlay
                Box(
                    Modifier
                        .fillMaxSize()
                        .border(8.dp, MaterialTheme.colorScheme.primary.copy(alpha = scanAlpha), CircleShape)
                )
            }
            
            if (showPasswordInput) {
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        var pass by remember { mutableStateOf("") }
                        AuraTextField(
                            value = pass,
                            onValueChange = { pass = it },
                            label = "Master Password",
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password
                            )
                        )
                        Spacer(Modifier.height(16.dp))
                        AuraButton(
                            text = "Verify Access",
                            onClick = {
                                if (pass == ownerManager.getMasterPassword()) {
                                    onAccessGranted()
                                } else {
                                    subStatus = "Incorrect Password"
                                }
                            }
                        )
                        TextButton(onClick = { showPasswordInput = false; faceScanActive = true }) {
                            Text("Cancel", color = MaterialTheme.colorScheme.primary)
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(60.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { showFingerprint() },
                    modifier = Modifier.size(64.dp).background(MaterialTheme.colorScheme.surface, CircleShape).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                ) {
                    Icon(Icons.Default.Fingerprint, "Fingerprint Authentication", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                }
                
                IconButton(
                    onClick = { showPasswordInput = true; faceScanActive = false },
                    modifier = Modifier.size(64.dp).background(MaterialTheme.colorScheme.surface, CircleShape).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                ) {
                    Icon(Icons.Default.Lock, "Password Authentication", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }

                IconButton(
                    onClick = { 
                        faceScanActive = true; showPasswordInput = false
                    },
                    modifier = Modifier.size(64.dp).background(MaterialTheme.colorScheme.surface, CircleShape).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                ) {
                    Icon(Icons.Default.Face, "Face Authentication", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }

                IconButton(
                    onClick = { 
                        authViewModel.signOut()
                        navController.navigate(NavRoutes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    modifier = Modifier.size(64.dp).background(MaterialTheme.colorScheme.surface, CircleShape).border(1.dp, Color.Red.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(Icons.Default.ExitToApp, "Sign Out", tint = Color.Red, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}
