package com.humanoidai.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.biometric.BiometricPrompt
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.humanoidai.ml.FaceEmbeddingHelper
import com.humanoidai.ml.FaceEnrollmentManager
import com.humanoidai.ml.FaceRecognitionManager
import com.humanoidai.ml.OwnerEnrollmentManager
import com.humanoidai.navigation.NavRoutes
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
    val haptic = LocalHapticFeedback.current

    var statusText by remember { mutableStateOf("Verifying Biometrics...") }
    var subStatus by remember { mutableStateOf("Position your face inside the circle") }
    var isVerifying by remember { mutableStateOf(false) }
    var showPasswordInput by remember { mutableStateOf(false) }
    var faceScanActive by remember { mutableStateOf(true) }

    val showFingerprint = {
        val activity = context.findActivity()
        if (activity != null) {
            val executor = ContextCompat.getMainExecutor(context)
            val biometricPrompt = BiometricPrompt(
                activity, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                            subStatus = errString.toString()
                        }
                        faceScanActive = true
                    }

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        if (!isVerifying) {
                            isVerifying = true
                            faceScanActive = false
                            statusText = "Identity Verified"
                            subStatus = "Access granted"
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onAccessGranted()
                        }
                    }
                }
            )

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
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "scan_alpha"
    )

    LaunchedEffect(Unit) {
        enrollmentManager.loadAllInto(recognitionManager, ownerManager)
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

    val configuration = LocalConfiguration.current
    val circleSize = (configuration.screenWidthDp.dp * 0.72f).coerceIn(240.dp, 300.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(
                    text = "Aura 360",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = AccentCyan,
                    letterSpacing = 1.5.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Biometric Lock",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Circular Camera Scanner
            Box(
                modifier = Modifier
                    .size(circleSize)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                // Pulse scanning border
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            width = 3.dp,
                            color = if (isVerifying) SuccessGreen else AccentCyan.copy(alpha = scanAlpha),
                            shape = CircleShape
                        )
                )

                WithCameraPermission {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .clip(CircleShape),
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
                                            val rotated = android.graphics.Bitmap.createBitmap(
                                                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                                            )

                                            val input = com.google.mlkit.vision.common.InputImage.fromBitmap(rotated, 0)
                                            detector.process(input)
                                                .addOnSuccessListener { faces ->
                                                    if (faces.isNotEmpty()) {
                                                        val face = faces[0]
                                                        val box = face.boundingBox
                                                        try {
                                                            val crop = android.graphics.Bitmap.createBitmap(
                                                                rotated,
                                                                box.left.coerceAtLeast(0),
                                                                box.top.coerceAtLeast(0),
                                                                box.width().coerceAtLeast(1),
                                                                box.height().coerceAtLeast(1)
                                                            )
                                                            val embedding = embeddingHelper.getEmbedding(crop)
                                                            val (name, _) = recognitionManager.findMatch(embedding)

                                                            if (name != "UNKNOWN" && !name.startsWith("PROBABLE_")) {
                                                                isVerifying = true
                                                                faceScanActive = false
                                                                statusText = "Welcome back, $name"
                                                                subStatus = "Access granted"
                                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                onAccessGranted()
                                                            }
                                                        } catch (e: Exception) {
                                                            Log.e("BiometricVerification", "Embedding error: ${e.message}")
                                                        }
                                                    }
                                                }
                                                .addOnCompleteListener { imageProxy.close() }
                                        }

                                        try {
                                            provider.unbindAll()
                                            val selector = try {
                                                CameraSelector.DEFAULT_FRONT_CAMERA
                                            } catch (e: Exception) {
                                                CameraSelector.DEFAULT_BACK_CAMERA
                                            }
                                            provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
                                        } catch (e: Exception) {
                                            Log.e("BiometricVerification", "Failed to bind camera", e)
                                        }
                                    }, ContextCompat.getMainExecutor(ctx))
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        if (!isVerifying && faceScanActive) {
                            AuraBiometricRadarScan(scanColor = AccentCyan)
                        }

                        if (isVerifying) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(SuccessGreen.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                AuraSuccessCheckmark(
                                    size = 72.dp,
                                    color = SuccessGreen
                                )
                            }
                        }
                    }
                }
            }

            // Status message
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // Bottom Actions (Fingerprint, Password, Face, Sign out)
            Surface(
                shape = RoundedCornerShape(CornerRadiusExtraLarge),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showFingerprint() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Fingerprint,
                            contentDescription = "Fingerprint Authentication",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = { showPasswordInput = true; faceScanActive = false },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "Password PIN",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = { faceScanActive = true; showPasswordInput = false },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Face,
                            contentDescription = "Face Scan",
                            tint = if (faceScanActive) AccentCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            authViewModel.signOut()
                            navController.navigate(NavRoutes.LOGIN) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Sign Out",
                            tint = ErrorRed,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Master Password Dialog fallback
        if (showPasswordInput) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable { showPasswordInput = false; faceScanActive = true },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(CornerRadiusLarge),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .clickable(enabled = false) {}
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Enter Master PIN",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Provide your emergency security PIN to unlock",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(20.dp))

                        var pass by remember { mutableStateOf("") }
                        AuraTextField(
                            value = pass,
                            onValueChange = { pass = it },
                            label = "Password",
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true
                        )
                        Spacer(Modifier.height(20.dp))
                        AuraButton(
                            text = "Unlock",
                            onClick = {
                                if (pass == ownerManager.getMasterPassword()) {
                                    showPasswordInput = false
                                    onAccessGranted()
                                } else {
                                    subStatus = "Incorrect Password"
                                }
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                showPasswordInput = false
                                faceScanActive = true
                            }
                        ) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
