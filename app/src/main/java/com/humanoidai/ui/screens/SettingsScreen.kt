package com.humanoidai.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.annotation.SuppressLint
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.window.Dialog
import androidx.camera.view.PreviewView
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.humanoidai.ui.components.WithCameraPermission
import java.util.concurrent.Executors
import com.humanoidai.ml.FaceEmbeddingHelper
import androidx.navigation.NavController
import com.humanoidai.hearing.SpeechRecognizerManager
import com.humanoidai.ml.OwnerEnrollmentManager
import com.humanoidai.navigation.NavRoutes
import com.humanoidai.ui.components.SidePanelDrawer
import com.humanoidai.ui.theme.*
import kotlinx.coroutines.launch
import com.humanoidai.ui.customization.AppearanceViewModel
import com.humanoidai.ui.customization.HUDStructure
import com.humanoidai.ui.customization.UIScale

enum class SecurityMethod { PASSWORD, FACE_SCAN, UPDATE_PASSWORD }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController, 
    ownerManager: OwnerEnrollmentManager,
    recognitionManager: com.humanoidai.ml.FaceRecognitionManager,
    voiceEngine: com.humanoidai.voice.VoiceEngine,
    appearanceViewModel: AppearanceViewModel,
    microphoneManager: SpeechRecognizerManager,
    authViewModel: AuthViewModel = viewModel()
) {
    val settings by appearanceViewModel.settings.collectAsState()
    val haptic = LocalHapticFeedback.current
    
    // UI State
    var aiName by remember { mutableStateOf(ownerManager.getAiName()) }
    
    val currentLang = ownerManager.getPreferredLanguage()
    var preferredLanguage by remember { mutableStateOf(
        when(currentLang) {
            "en" -> "English"
            "ta" -> "Tamil"
            else -> "Automatic"
        }
    ) }
    
    var showSecurityPrompt by remember { mutableStateOf(false) }
    var securityMethod by remember { mutableStateOf<SecurityMethod?>(null) }

    val ownerName = ownerManager.getOwnerName()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    if (showSecurityPrompt) {
        if (securityMethod == SecurityMethod.UPDATE_PASSWORD) {
            UpdatePasswordDialog(
                authViewModel = authViewModel,
                onDismiss = { showSecurityPrompt = false }
            )
        } else {
            SecurityAccessDialog(
                method = securityMethod ?: SecurityMethod.PASSWORD,
                onDismiss = { showSecurityPrompt = false },
                onAuthenticated = {
                    showSecurityPrompt = false
                    if (securityMethod == SecurityMethod.FACE_SCAN) {
                        navController.navigate("owner_enrollment")
                    } else if (securityMethod == SecurityMethod.PASSWORD) {
                        // This was for "Update Password" but now we use UPDATE_PASSWORD state
                    }
                }
            )
        }
    }

    SidePanelDrawer(
        navController = navController,
        drawerState = drawerState
    ) {
        Scaffold(
            containerColor = BackgroundDark,
            topBar = {
                Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { 
                        scope.launch { drawerState.open() } 
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark.copy(alpha = 0.4f))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape)
                ) {
                    Icon(Icons.Default.Menu, "Menu", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                
                Spacer(Modifier.width(16.dp))
                
                Text("SETTINGS", fontSize = 15.sp, fontWeight = FontWeight.Black, color = AccentCyan, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
            }
        }
    ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                // ---- AI Persona ----
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader("AI Persona")
                
                OutlinedTextField(
                    value = aiName,
                    onValueChange = { 
                        aiName = it
                        ownerManager.setAiName(it)
                    },
                    label = { Text("AI Name / Wake Word", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = SurfaceDark
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // ---- HUD Customization ----
                SectionHeader("HUD Customization")
                
                SettingsItem(
                    icon = Icons.Default.Palette,
                    title = "Advanced Appearance",
                    subtitle = "Themes, colors and global scale",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        navController.navigate(NavRoutes.APPEARANCE)
                    }
                )

                SettingsItem(
                    icon = Icons.Default.Groups,
                    title = "Max ROI Count",
                    subtitle = "Face detection limit",
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val current = settings.maxRoi
                        appearanceViewModel.setMaxRoi(if (current >= 5) 1 else current + 1)
                    },
                    trailingContent = {
                        Text(
                            "${settings.maxRoi}", 
                            fontSize = 14.sp, 
                            color = AccentCyan, 
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                )

                // 2. HUD Structure (Replaces Layout Preset for more impact)
                SettingsItem(
                    icon = Icons.Default.DashboardCustomize,
                    title = "System HUD Structure",
                    subtitle = "Spatial arrangement of AI components",
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val structures = HUDStructure.entries
                        val nextIndex = (settings.hudStructure.ordinal + 1) % structures.size
                        appearanceViewModel.setStructure(structures[nextIndex])
                    },
                    trailingContent = {
                        Text(
                            settings.hudStructure.name.replace("_", " "), 
                            fontSize = 11.sp, 
                            color = AccentCyan, 
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 4. Show Sensors
                ToggleRow(Icons.Default.Analytics, "Show Sensor Status", settings.showSensorStatus) { 
                    appearanceViewModel.toggleComponent("sensors", it)
                }
                
                // 5. Show Confidence
                ToggleRow(Icons.Default.Label, "Show ROI Labels", settings.showConfidence) { 
                    appearanceViewModel.toggleComponent("confidence", it)
                }
                
                // 6. Glow Effects
                ToggleRow(Icons.Default.AutoFixHigh, "Dynamic Glow Effects", settings.glowEnabled) { 
                    appearanceViewModel.toggleComponent("glow", it)
                }
                
                // 7. Radar Sweep
                ToggleRow(Icons.Default.Radar, "Enable Audio Radar", settings.showRadar) { 
                    appearanceViewModel.toggleComponent("radar", it)
                }
                
                // 7. Grid Overlay
                ToggleRow(Icons.Default.Grid4x4, "Background Dot Grid", settings.showGrid) {
                    appearanceViewModel.toggleComponent("grid", it)
                }

                // ---- Account ----
                Spacer(modifier = Modifier.height(20.dp))
                SectionHeader("Account")
                
                SettingsItem(
                    icon = Icons.Default.Person,
                    title = "Profile Identity",
                    subtitle = "Signed in as $ownerName",
                    onClick = { /* No-op */ }
                )

                SettingsItem(
                    icon = Icons.Default.Password,
                    title = "Update Password",
                    subtitle = "Change your account access key",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        securityMethod = SecurityMethod.UPDATE_PASSWORD
                        showSecurityPrompt = true
                    }
                )
                
                SettingsItem(
                    icon = Icons.Default.RecordVoiceOver,
                    title = "Update Biometrics",
                    subtitle = "Re-scan face and voice fingerprints",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        securityMethod = SecurityMethod.FACE_SCAN
                        showSecurityPrompt = true
                    }
                )
                
                SettingsItem(
                    icon = Icons.Default.ExitToApp,
                    title = "Sign Out",
                    subtitle = "Securely end this AI session",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        ownerManager.clearOwner()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    trailingContent = {
                        Icon(Icons.Default.Logout, null, tint = ErrorRed.copy(alpha = 0.6f))
                    }
                )

                // ---- Assistant ----
                Spacer(modifier = Modifier.height(20.dp))
                SectionHeader("Assistant")
                
                SettingsItem(
                    icon = Icons.Default.Translate,
                    title = "System Language",
                    subtitle = preferredLanguage,
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val next = when(preferredLanguage) {
                            "Automatic" -> "English"
                            "English" -> "Tamil"
                            else -> "Automatic"
                        }
                        preferredLanguage = next
                        val code = when(next) {
                            "English" -> "en"
                            "Tamil" -> "ta"
                            else -> "auto"
                        }
                        ownerManager.setPreferredLanguage(code)
                        microphoneManager.setLanguage(code)
                    },
                    trailingContent = {
                        Icon(Icons.Default.Language, null, tint = AccentCyan.copy(alpha = 0.5f))
                    }
                )

                Spacer(Modifier.height(12.dp))

                ToggleRow(Icons.Default.Warning, "Show Alert Banners", settings.showAlertBanner) { 
                    appearanceViewModel.toggleComponent("alerts", it) 
                }
                ToggleRow(Icons.Default.AutoAwesome, "Show Live Context", settings.showLiveContext) { 
                    appearanceViewModel.toggleComponent("context", it)
                }

                // ---- About ----
                Spacer(modifier = Modifier.height(20.dp))
                SectionHeader("About")
                
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "App Version",
                    subtitle = "1.2.0 (Production Assembly)",
                    onClick = {}
                )
                
                SettingsItem(
                    icon = Icons.Default.Build,
                    title = "Build Identity",
                    subtitle = "CEA v1.5-Assembly",
                    onClick = {}
                )

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
fun UpdatePasswordDialog(
    authViewModel: AuthViewModel,
    onDismiss: () -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val authState by authViewModel.authState.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceDark,
            border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.LockReset, null, tint = AccentCyan, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text("Update Password", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Verify your identity and set a new key", color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
                
                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Current Password") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = TextSecondary,
                        cursorColor = AccentCyan
                    )
                )

                Spacer(Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = AccentCyan)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = TextSecondary,
                        cursorColor = AccentCyan
                    )
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm New Password") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = TextSecondary,
                        cursorColor = AccentCyan
                    )
                )

                if (newPassword.isNotEmpty() && confirmPassword.isNotEmpty() && newPassword != confirmPassword) {
                    Text("Passwords do not match", color = ErrorRed, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }

                AuthStatusText(authState)

                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
                    Button(
                        onClick = {
                            if (newPassword == confirmPassword) {
                                authViewModel.updateAccountPassword(newPassword) {
                                    onDismiss()
                                }
                            }
                        },
                        enabled = currentPassword.isNotBlank() && newPassword.isNotBlank() && newPassword == confirmPassword && authState !is AuthState.Loading,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                    ) {
                        Text("Update", color = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthStatusText(state: AuthState) {
    when (state) {
        is AuthState.Error -> {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = state.message,
                color = ErrorRed,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
        else -> {}
    }
}

@Composable
fun SecurityAccessDialog(
    method: SecurityMethod,
    onDismiss: () -> Unit,
    onAuthenticated: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceDark,
            border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    if (method == SecurityMethod.FACE_SCAN) Icons.Default.Face else Icons.Default.Lock,
                    null, tint = AccentCyan, modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Security Verification", 
                    color = Color.White, 
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    if (method == SecurityMethod.FACE_SCAN) "Scanning for owner..." else "Enter password to proceed",
                    color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center
                )
                
                Spacer(Modifier.height(24.dp))
                
                if (method == SecurityMethod.PASSWORD) {
                    var pass by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = pass,
                        onValueChange = { pass = it },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    FaceAuthScan(onAuthenticated = onAuthenticated)
                }
                
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
                    if (method == SecurityMethod.PASSWORD) {
                        TextButton(onClick = onAuthenticated) { Text("Verify", color = AccentCyan) }
                    }
                }
            }
        }
    }
}

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun FaceAuthScan(onAuthenticated: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val recognitionManager = remember { com.humanoidai.ml.FaceRecognitionManager() }
    val ownerManager = remember { OwnerEnrollmentManager(context) }
    val embeddingHelper = remember { FaceEmbeddingHelper(context) }
    
    var isVerifying by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Load only the owner for verification
        ownerManager.getMasterEmbedding()?.let { recognitionManager.registerFace(ownerManager.getOwnerName(), it) }
    }

    WithCameraPermission {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .border(2.dp, AccentCyan, CircleShape),
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
                            
                            val detector = com.google.mlkit.vision.face.FaceDetection.getClient()
                            val executor = Executors.newSingleThreadExecutor()

                            analysis.setAnalyzer(executor) { imageProxy ->
                                if (isVerifying) { imageProxy.close(); return@setAnalyzer }
                                
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
                                                val crop = android.graphics.Bitmap.createBitmap(
                                                    rotated, 
                                                    box.left.coerceAtLeast(0), 
                                                    box.top.coerceAtLeast(0), 
                                                    box.width().coerceAtLeast(1), 
                                                    box.height().coerceAtLeast(1)
                                                )
                                                val embedding = embeddingHelper.getEmbedding(crop)
                                                val match = recognitionManager.findMatch(embedding)
                                                if (match.first == ownerManager.getOwnerName()) {
                                                    isVerifying = true
                                                    onAuthenticated()
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
            if (!isVerifying) {
                CircularProgressIndicator(
                    modifier = Modifier.size(180.dp),
                    color = AccentCyan.copy(alpha = 0.3f),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = AccentCyan.copy(alpha = 0.8f),
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    trailingContent: @Composable () -> Unit = { Icon(Icons.Default.ChevronRight, null, tint = TextSecondary.copy(alpha = 0.5f)) }
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = SurfaceDark.copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AccentCyan.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = AccentCyan, modifier = Modifier.size(18.dp))
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                if (subtitle != null) {
                    Text(subtitle, color = TextSecondary, fontSize = 11.sp)
                }
            }
            
            trailingContent()
        }
    }
}

@Composable
private fun ToggleRow(icon: ImageVector, label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val haptic = LocalHapticFeedback.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = SurfaceDark.copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(label, fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f))
            Switch(
                checked = checked,
                onCheckedChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCheckedChange(it)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = AccentCyan,
                    uncheckedThumbColor = TextSecondary.copy(alpha = 0.5f),
                    uncheckedTrackColor = SurfaceDark.copy(alpha = 0.4f)
                ),
                modifier = Modifier.scale(0.85f)
            )
        }
    }
}

