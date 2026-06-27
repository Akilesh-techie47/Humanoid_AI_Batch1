package com.humanoidai.ui.screens

import android.view.OrientationEventListener
import android.view.Surface
import android.view.WindowManager
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.humanoidai.alerts.AlertEngine
import com.humanoidai.vision.*
import com.humanoidai.ml.*
import com.humanoidai.ui.components.*
import com.humanoidai.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvironmentScreen(
    navController: NavController,
    recognitionManager: FaceRecognitionManager,
    ownerManager: OwnerEnrollmentManager,
    enrollmentManager: FaceEnrollmentManager,
    alertEngine: AlertEngine,
    aiManager: com.humanoidai.ai.AIManager,
    companionEngine: com.humanoidai.companion.CompanionEngine,
    contextEngine: com.humanoidai.context.ContextEngine,
    voiceEngine: com.humanoidai.voice.VoiceEngine,
    microphoneManager: com.humanoidai.hearing.SpeechRecognizerManager,
    attentionManager: com.humanoidai.attention.AttentionManager
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Permission Launcher
    val recordAudioPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, can start listening
        } else {
            // Handle permission denied
        }
    }

    // AI & Conversation State
    val currentContext by contextEngine.currentContext.collectAsState()
    val messages by aiManager.getMessages().collectAsState()
    val aiState by aiManager.aiState.collectAsState()
    val isListening by microphoneManager.isListening.collectAsState()
    val attentionFocus by attentionManager.currentFocus.collectAsState()
    
    var inputText by remember { mutableStateOf("") }
    var aiStatusText by remember { mutableStateOf("SCANNING ROOM") }
    var roomDescription by remember { mutableStateOf("Analyzing context...") }
    var lastGeminiAnalysisTime = remember { mutableLongStateOf(0L) }
    
    val listState = rememberLazyListState()

    // Humanoid Proactive Assistant
    val gemini = remember { GeminiContextHelper(com.humanoidai.BuildConfig.GEMINI_API_KEY) }
    val ownerName = remember { ownerManager.getOwnerName() }

    // Auto-scroll messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // ML pipeline
    val embeddingHelper = remember { FaceEmbeddingHelper(context) }
    val fisheyeCorrector = remember { FisheyeCorrector() }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    // State
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var detectedPersons by remember { mutableStateOf<List<DetectedPerson>>(emptyList()) }
    var isAlerting by remember { mutableStateOf(false) }

    var imageAnalysisUseCase by remember { mutableStateOf<ImageAnalysis?>(null) }
    var previewUseCase by remember { mutableStateOf<Preview?>(null) }

    val unreadCount by alertEngine.unreadCount.collectAsState()

    val faceAnalyzer = remember {
        com.humanoidai.vision.FaceAnalyzer(
            embeddingHelper = embeddingHelper,
            recognitionManager = recognitionManager,
            enrollmentManager = enrollmentManager,
            fisheyeCorrector = fisheyeCorrector,
            onResults = { persons ->
                detectedPersons = persons
                isAlerting = persons.any { it.name == "UNKNOWN" }
                alertEngine.processDetections(persons)
                
                // Keep AI Manager in sync with environmental context
                aiManager.updateEnvironment(persons, alertEngine.unreadCount.value)

                // Update AI status text
                aiStatusText = when {
                    persons.isEmpty() -> "SCANNING ROOM"
                    persons.any { it.name == ownerName } -> "OWNER PRESENT"
                    persons.any { it.name == "UNKNOWN" } -> "MONITORING VISITOR"
                    else -> "SUBJECTS DETECTED"
                }

                // ---- Proactive Voice Interaction (80% Refinement) ----
                val arrivingPerson = persons.find { it.isNewArrival }
                
                // Run Multimodal AI analysis every 15 seconds if someone is present
                if (persons.isNotEmpty() && (System.currentTimeMillis() - lastGeminiAnalysisTime.longValue > 15_000)) {
                    lastGeminiAnalysisTime.longValue = System.currentTimeMillis()
                    scope.launch {
                        val bitmap = previewView?.bitmap ?: return@launch
                        val description = gemini.interpretRoom(bitmap, ownerName)
                        roomDescription = description
                        // Proactively speak the AI's deep understanding
                        if (arrivingPerson == null) { // Don't interrupt greetings
                           voiceEngine.speak(description)
                        }
                    }
                }
                
                when {
                    arrivingPerson != null && arrivingPerson.name == ownerName -> {
                        val calendar = java.util.Calendar.getInstance()
                        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                        val greeting = when {
                            hour < 12 -> "Good morning"
                            hour < 17 -> "Good afternoon"
                            else -> "Good evening"
                        }
                        val unread = alertEngine.unreadCount.value
                        val alertSummary = if (unread > 0) ". You have $unread unread alerts since your last session." else ". No new security events to report."
                        voiceEngine.speak("$greeting $ownerName. System is online and monitoring $alertSummary")
                    }
                    arrivingPerson != null && arrivingPerson.name == "UNKNOWN" -> {
                        voiceEngine.speak("Identity unknown. Initiating enhanced environment logging.")
                    }
                    persons.size > 2 -> {
                        voiceEngine.speak("Multiple subjects in field of view. Optimizing analyzer for group tracking.")
                    }
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        // Keep screen on while in Environment (on the table use case)
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    LaunchedEffect(lensFacing, previewView) {
        val pv = previewView ?: return@LaunchedEffect
        faceAnalyzer.isFrontCamera = lensFacing == CameraSelector.LENS_FACING_FRONT
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            
            val preview = Preview.Builder().build().also { 
                it.setSurfaceProvider(pv.surfaceProvider)
                previewUseCase = it
            }
            
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()
                .also { 
                    it.setAnalyzer(analysisExecutor, faceAnalyzer)
                    imageAnalysisUseCase = it
                }

            val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, analysis)
            } catch (e: Exception) { e.printStackTrace() }
        }, ContextCompat.getMainExecutor(context))
    }

    DisposableEffect(context) {
        val orientationEventListener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                val rotation = when (orientation) {
                    in 45 until 135 -> Surface.ROTATION_270
                    in 135 until 225 -> Surface.ROTATION_180
                    in 225 until 315 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }
                imageAnalysisUseCase?.targetRotation = rotation
                previewUseCase?.targetRotation = rotation
            }
        }
        orientationEventListener.enable()

        onDispose {
            orientationEventListener.disable()
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            voiceEngine.shutdown()
            analysisExecutor.shutdown()
            embeddingHelper.close()
            fisheyeCorrector.release()
        }
    }

    SidePanelDrawer(
        navController = navController,
        drawerState = drawerState,
        unreadCount = unreadCount
    ) {
        Scaffold(
            containerColor = Color(0xFF020408),
            topBar = {
                Surface(color = Color.Black.copy(alpha = 0.8f), border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.05f))) {
                    Row(
                        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { scope.launch { drawerState.open() } }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Menu, "Menu", tint = Color.White)
                        }
                        Spacer(Modifier.width(16.dp))
                        Text("HUMANOID AI", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AccentCyan)
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.size(6.dp).background(if(isAlerting) Color.Red else AlertGreen, CircleShape))
                        
                        Spacer(Modifier.weight(1f))
                        
                        Text(
                            text = attentionFocus.name,
                            fontSize = 10.sp,
                            color = AccentCyan,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        
                        Spacer(Modifier.width(16.dp))
                        
                        IconButton(onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                                CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                        }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Cameraswitch, "Switch", tint = Color.White)
                        }
                    }
                }
            },
            bottomBar = {
                UnifiedChatSection(
                    messages = messages,
                    listState = listState,
                    inputText = inputText,
                    onInputChanged = { inputText = it },
                    isListening = isListening,
                    roomDescription = roomDescription,
                    onSend = {
                        val q = inputText
                        inputText = ""
                        scope.launch { aiManager.ask(q, ownerName, "Home") }
                    },
                    onMicClick = {
                        if (com.humanoidai.permission.PermissionManager.hasRecordAudioPermission(context)) {
                            if (isListening) {
                                microphoneManager.stopListening()
                            } else {
                                microphoneManager.startListening(
                                    onPartialResult = { inputText = it },
                                    onFinalResult = { final ->
                                        inputText = ""
                                        scope.launch {
                                            aiManager.ask(final, ownerName, "Home")
                                            delay(2500)
                                            microphoneManager.startPassiveListening { voiceEngine.speak("I'm listening.") }
                                        }
                                    }
                                )
                            }
                        } else {
                            recordAudioPermissionLauncher.launch(com.humanoidai.permission.PermissionManager.RECORD_AUDIO_PERMISSION)
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Background Hub Elements
                Column(modifier = Modifier.fillMaxSize()) {
                    Spacer(Modifier.height(40.dp))
                    
                    // Central Camera Hub
                    Box(modifier = Modifier.fillMaxWidth().weight(1.2f), contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .clip(CircleShape)
                                .background(Color.Black)
                                .border(2.dp, if (isAlerting) Color.Red else AccentCyan.copy(alpha = 0.3f), CircleShape)
                        ) {
                            AndroidView(
                                factory = { ctx -> PreviewView(ctx).also { previewView = it } },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        
                        // ROI Cards
                        val primary = detectedPersons.find { it.isPrimary }
                        
                        primary?.let {
                            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 0.dp)) {
                                PersonRoiCard(it, 1.0f, detectedPersons.size, it.name == ownerName, TrapezoidShape(15f))
                            }
                        }
                    }

                    // Side Panels & Stats
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Panel: Person Count & State
                        Column(horizontalAlignment = Alignment.Start) {
                            Text("PERSONS", fontSize = 9.sp, color = TextSecondary)
                            Text("${detectedPersons.size}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(Modifier.height(8.dp))
                            Text(aiStatusText, fontSize = 10.sp, color = AccentCyan.copy(alpha = 0.8f))
                        }

                        // Right Panel: Environment Stats
                        Column(horizontalAlignment = Alignment.End) {
                            StatMiniItem("BATTERY", "${currentContext.batteryPercent}%", Icons.Default.BatteryChargingFull)
                            Spacer(Modifier.height(12.dp))
                            StatMiniItem("NOISE", "${currentContext.noiseLevel.toInt()} dB", Icons.Default.GraphicEq)
                            Spacer(Modifier.height(12.dp))
                            StatMiniItem("LIGHT", "NORMAL", Icons.Default.LightMode)
                        }
                    }
                    
                    Spacer(Modifier.height(80.dp)) // Leave room for chat
                }
            }
        }
    }
}

@Composable
fun StatMiniItem(label: String, value: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.End) {
            Text(label, fontSize = 8.sp, color = TextSecondary)
            Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(Modifier.width(8.dp))
        Icon(icon, null, tint = AccentCyan, modifier = Modifier.size(14.dp))
    }
}

@Composable
fun UnifiedChatSection(
    messages: List<com.humanoidai.ai.ChatMessage>,
    listState: LazyListState,
    inputText: String,
    onInputChanged: (String) -> Unit,
    isListening: Boolean,
    roomDescription: String,
    onSend: () -> Unit,
    onMicClick: () -> Unit
) {
    Column(modifier = Modifier.background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))) {
        // Message Scroll
        if (messages.isNotEmpty()) {
            Box(modifier = Modifier.heightIn(max = 140.dp).fillMaxWidth().padding(horizontal = 16.dp)) {
                LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(messages) { msg -> UnifiedChatBubble(msg.text, msg.isUser) }
                }
            }
        }

        // Context Bar
        Box(modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.6f)).padding(vertical = 4.dp)) {
            Text(
                "AI VISION: $roomDescription",
                color = AccentCyan, fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Input Bar
        UnifiedChatInputBar(
            value = inputText,
            onValueChange = onInputChanged,
            onSend = onSend,
            isListening = isListening,
            onMicClick = onMicClick
        )
    }
}

@Composable
fun PersonRoiCard(
    person: DetectedPerson,
    priority: Float,
    totalCount: Int,
    isOwner: Boolean = false,
    shape: Shape = RoundedCornerShape(16.dp)
) {
    val isPrimary = priority > 0.8f
    val baseSize = if (isPrimary) 110.dp else 80.dp
    
    val ownerColor = Color(0xFFFFD700)
    val color = when {
        isOwner -> ownerColor
        person.name == "UNKNOWN" -> Color.Red
        else -> AccentCyan
    }
    
    val size by animateDpAsState(targetValue = baseSize, label = "size")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .background(color.copy(alpha = 0.08f), shape)
                .border(1.dp, color.copy(alpha = 0.4f), shape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isOwner) Icons.Default.Stars else Icons.Default.Person,
                null,
                tint = color.copy(alpha = 0.8f),
                modifier = Modifier.size(size * 0.4f)
            )
        }
        
        Spacer(Modifier.height(6.dp))
        Text(
            if (isOwner) "OWNER" else person.name,
            fontSize = if (isPrimary) 12.sp else 9.sp,
            color = if (isOwner) ownerColor else Color.White,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            "${(person.confidence * 100).toInt()}%",
            fontSize = 9.sp,
            color = TextSecondary,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun ContextChip(text: String, icon: ImageVector, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = color.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, fontSize = 10.sp, color = Color.White)
        }
    }
}

class TrapezoidShape(val percent: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            val offset = size.width * (percent / 100f)
            moveTo(offset, 0f)
            lineTo(size.width - offset, 0f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}

class SlantedTrapezoidShape(val slantPercent: Float, val isLeft: Boolean) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            val offset = size.width * (slantPercent / 100f)
            if (isLeft) {
                moveTo(0f, 0f)
                lineTo(size.width - offset, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
            } else {
                moveTo(offset, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
            }
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
fun UnifiedChatBubble(text: String, isUser: Boolean) {
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bgColor = if (isUser) AccentCyan.copy(alpha = 0.8f) else SurfaceDark.copy(alpha = 0.8f)
    val textColor = if (isUser) Color.Black else TextPrimary
    val shape = if (isUser) RoundedCornerShape(12.dp, 12.dp, 2.dp, 12.dp) else RoundedCornerShape(12.dp, 12.dp, 12.dp, 2.dp)

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalAlignment = alignment) {
        Box(
            modifier = Modifier
                .widthIn(max = 240.dp)
                .background(bgColor, shape)
                .border(0.5.dp, Color.White.copy(alpha = 0.1f), shape)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(text = text, color = textColor, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
fun UnifiedChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    isListening: Boolean,
    onMicClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Command Humanoid...", color = TextSecondary, fontSize = 12.sp) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentCyan.copy(alpha = 0.5f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                focusedContainerColor = Color(0xFF18181E),
                unfocusedContainerColor = Color(0xFF18181E)
            ),
            maxLines = 2,
            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
        )

        IconButton(
            onClick = onMicClick,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isListening) Color.Red else AccentCyan)
        ) {
            Icon(if (isListening) Icons.Default.MicOff else Icons.Default.Mic, null, tint = Color.Black)
        }

        if (value.isNotBlank()) {
            IconButton(
                onClick = onSend,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AccentCyan)
            ) {
                Icon(Icons.Default.Send, null, tint = Color.Black, modifier = Modifier.size(18.dp))
            }
        }
    }
}
