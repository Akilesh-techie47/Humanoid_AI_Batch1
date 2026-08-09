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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.annotation.SuppressLint
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.window.Dialog
import androidx.camera.view.PreviewView
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.humanoidai.ui.components.WithCameraPermission
import java.util.concurrent.Executors
import com.humanoidai.ml.FaceEmbeddingHelper
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.humanoidai.ml.OwnerEnrollmentManager
import com.humanoidai.ui.components.SidePanelDrawer
import com.humanoidai.ui.theme.*
import kotlinx.coroutines.launch
import com.humanoidai.ui.customization.AppearanceViewModel
import com.humanoidai.ui.customization.HUDStructure
import com.humanoidai.ui.customization.UIScale

enum class SecurityMethod { PASSWORD, FACE_SCAN }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController, 
    ownerManager: OwnerEnrollmentManager,
    recognitionManager: com.humanoidai.ml.FaceRecognitionManager,
    voiceEngine: com.humanoidai.voice.VoiceEngine,
    appearanceViewModel: AppearanceViewModel,
    microphoneManager: com.humanoidai.hearing.SpeechRecognizerManager
) {
    val settings by appearanceViewModel.settings.collectAsState()
    
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

    val currentUser = FirebaseAuth.getInstance().currentUser
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    if (showSecurityPrompt) {
        SecurityAccessDialog(
            method = securityMethod ?: SecurityMethod.PASSWORD,
            onDismiss = { showSecurityPrompt = false },
            onAuthenticated = {
                showSecurityPrompt = false
                navController.navigate("owner_enrollment")
            }
        )
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
                        Text("SETTINGS", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccentCyan)
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
                
                ActionRow(Icons.Default.Palette, "Advanced Appearance", AccentCyan) {
                    navController.navigate(com.humanoidai.navigation.NavRoutes.APPEARANCE)
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                // 1. Max ROI Count (Linked to Settings)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark, RoundedCornerShape(10.dp))
                        .clickable { 
                            val current = settings.maxRoi
                            appearanceViewModel.setMaxRoi(if (current >= 5) 1 else current + 1)
                        }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Groups, null, tint = AccentCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Max ROI Count", fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                    Text("${settings.maxRoi}", fontSize = 13.sp, color = AccentCyan, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))

                // 2. HUD Structure (Replaces Layout Preset for more impact)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark, RoundedCornerShape(10.dp))
                        .clickable { 
                            val structures = HUDStructure.values()
                            val nextIndex = (settings.hudStructure.ordinal + 1) % structures.size
                            appearanceViewModel.setStructure(structures[nextIndex])
                        }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DashboardCustomize, null, tint = AccentCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("System HUD Structure", fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                    Text(settings.hudStructure.name.replace("_", " "), fontSize = 13.sp, color = AccentCyan, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))

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
                InfoRow(Icons.Default.Person, "Signed in as", currentUser?.email ?: currentUser?.phoneNumber ?: "Unknown")
                
                Spacer(modifier = Modifier.height(8.dp))
                
                ActionRow(Icons.Default.RecordVoiceOver, "Update Biometrics", AccentPurple) {
                    securityMethod = SecurityMethod.FACE_SCAN
                    showSecurityPrompt = true
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                ActionRow(Icons.Default.ExitToApp, "Sign Out", Color(0xFFFF5C5C)) {
                    FirebaseAuth.getInstance().signOut()
                    ownerManager.clearOwner()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }

                // ---- Assistant ----
                Spacer(modifier = Modifier.height(20.dp))
                SectionHeader("Assistant")
                ToggleRow(Icons.Default.Warning, "Show Alert Banners", settings.showAlertBanner) { 
                    appearanceViewModel.toggleComponent("alerts", it) 
                }
                ToggleRow(Icons.Default.AutoAwesome, "Show Live Context", settings.showLiveContext) { 
                    appearanceViewModel.toggleComponent("context", it)
                }

                Spacer(modifier = Modifier.height(12.dp))
                
                // Language Selection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark, RoundedCornerShape(10.dp))
                        .clickable { 
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
                        }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Translate, null, tint = AccentCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Interaction Language", fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                    Text(preferredLanguage, fontSize = 13.sp, color = AccentCyan, fontWeight = FontWeight.Bold)
                }

                // ---- About ----
                Spacer(modifier = Modifier.height(20.dp))
                SectionHeader("About")
                InfoRow(Icons.Default.Info, "App Version", "1.2.0 (CEA)")
                InfoRow(Icons.Default.Build, "Build", "CEA v1.4-Custom")

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
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
        title,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = AccentCyan,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun ToggleRow(icon: ImageVector, label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = AccentCyan,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = SurfaceDark
            )
        )
    }
    Spacer(modifier = Modifier.height(6.dp))
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, color = TextSecondary)
    }
    Spacer(modifier = Modifier.height(6.dp))
}

@Composable
private fun ActionRow(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, fontSize = 14.sp, color = color, fontWeight = FontWeight.SemiBold)
    }
}
