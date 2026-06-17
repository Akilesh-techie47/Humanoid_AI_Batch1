package com.humanoidai.ui.screens

import kotlinx.coroutines.delay
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
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
    microphoneManager: com.humanoidai.hearing.MicrophoneManager,
    attentionManager: com.humanoidai.attention.AttentionManager
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // AI & Conversation State
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
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                        ) {
                            Text("HUMANOID AI", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccentCyan)
                            Spacer(Modifier.width(8.dp))
                            Box(Modifier.size(6.dp).background(if(isAlerting) Color.Red else AlertGreen, CircleShape))
                            
                            Spacer(Modifier.weight(1f))
                            
                            Column(horizontalAlignment = Alignment.End) {
                                Text("FOCUS", fontSize = 9.sp, color = TextSecondary)
                                Text(attentionFocus.name, fontSize = 10.sp, color = AccentCyan, fontWeight = FontWeight.Bold)
                            }
                            
                            Spacer(Modifier.width(12.dp))
                            
                            Column(horizontalAlignment = Alignment.End) {
                                Text("STATUS", fontSize = 9.sp, color = TextSecondary)
                                Text(aiStatusText, fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, "Menu", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                                CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                        }) {
                            Icon(Icons.Default.Cameraswitch, "Switch", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            },
        bottomBar = {
            Column(modifier = Modifier.background(Color.Black.copy(alpha = 0.5f))) {
                // Message List (Condensed)
                if (messages.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                            .padding(horizontal = 16.dp)
                            .background(Color.Black.copy(alpha = 0.2f))
                    ) {
                        LazyColumn(
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(messages) { msg ->
                                UnifiedChatBubble(msg.text, msg.isUser)
                            }
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Text(
                        text = "AI VISION: $roomDescription",
                        color = AccentCyan,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                UnifiedChatInputBar(
                    value = inputText,
                    onValueChange = { inputText = it },
                    onSend = {
                        val q = inputText
                        inputText = ""
                        scope.launch {
                            aiManager.ask(q, ownerName, "Home")
                        }
                    },
                    isListening = isListening,
                    onMicClick = {
                        if (isListening) {
                            microphoneManager.stopListening()
                        } else {
                            microphoneManager.startListening(
                                onPartialResult = { inputText = it },
                                onFinalResult = { final ->
                                    inputText = ""
                                    scope.launch {
                                        aiManager.ask(final, ownerName, "Home")
                                        
                                        // Auto-restart passive listening after a short delay to allow TTS to finish
                                        delay(3000)
                                        microphoneManager.startPassiveListening {
                                            voiceEngine.speak("I'm listening.")
                                        }
                                    }
                                }
                            )
                        }
                    }
                )
            }
        }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ---- The Central Hub ----
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        // 1. Circular Camera Feed
                        Box(
                            modifier = Modifier
                                .padding(top = 20.dp)
                                .size(240.dp)
                                .clip(CircleShape)
                                .background(Color.Black)
                                .border(1.5.dp, if (isAlerting) Color.Red else AccentCyan.copy(alpha = 0.4f), CircleShape)
                        ) {
                            AndroidView(
                                factory = { ctx -> PreviewView(ctx).also { previewView = it } },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // ROI Data extraction
                        val ownerName = ownerManager.getOwnerName()
                        val primaryPerson = detectedPersons.find { it.isPrimary }
                        val others = detectedPersons.filter { !it.isPrimary }.take(2)

                        // 2. Main Person ROI (Straight Down)
                        primaryPerson?.let {
                            Box(
                                modifier = Modifier
                                    .padding(top = 260.dp)
                                    .align(Alignment.TopCenter)
                            ) {
                                PersonRoiCard(
                                    person = it,
                                    priority = 1.0f,
                                    totalCount = detectedPersons.size,
                                    isOwner = it.name == ownerName,
                                    shape = TrapezoidShape(percent = 15f)
                                )
                            }
                        }

                        // 3. Secondary Person (Left Wing)
                        if (others.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 70.dp, end = 210.dp)
                                    .align(Alignment.TopCenter)
                                    .graphicsLayer { rotationZ = -15f }
                            ) {
                                PersonRoiCard(
                                    person = others[0],
                                    priority = 0.65f,
                                    totalCount = detectedPersons.size,
                                    isOwner = others[0].name == ownerName,
                                    shape = SlantedTrapezoidShape(slantPercent = 20f, isLeft = true)
                                )
                            }
                        }

                        // 4. Secondary Person (Right Wing)
                        if (others.size > 1) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 70.dp, start = 210.dp)
                                    .align(Alignment.TopCenter)
                                    .graphicsLayer { rotationZ = 15f }
                            ) {
                                PersonRoiCard(
                                    person = others[1],
                                    priority = 0.65f,
                                    totalCount = detectedPersons.size,
                                    isOwner = others[1].name == ownerName,
                                    shape = SlantedTrapezoidShape(slantPercent = 20f, isLeft = false)
                                )
                            }
                        }
                    }
                }
            }
        }
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
fun HudChatBottomSection(persons: List<DetectedPerson>) {
    Column(modifier = Modifier.background(Color(0xFF05070A))) {
        // Suggestion Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ContextChip(text = "${persons.size} People", icon = Icons.Default.Groups, color = AccentCyan)
            ContextChip(text = "Ask about this person", icon = Icons.Default.ChatBubbleOutline, color = Color.White.copy(alpha = 0.6f))
            ContextChip(text = "Summarize room", icon = Icons.Default.AutoAwesome, color = Color.White.copy(alpha = 0.6f))
        }

        // Chat Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                .background(Color(0xFF0D141F).copy(alpha = 0.8f), RoundedCornerShape(28.dp))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(28.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Add, null, tint = Color.White.copy(alpha = 0.6f))
                }
                
                Text(
                    "Ask me anything...", 
                    color = TextSecondary, 
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                
                IconButton(
                    onClick = {}, 
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AccentCyan)
                ) {
                    Icon(Icons.Default.Mic, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                }
            }
        }
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
