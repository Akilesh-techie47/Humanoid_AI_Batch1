package com.humanoidai.ui.screens

import android.util.Log
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.humanoidai.ai.AIManager
import com.humanoidai.ai.AIState
import com.humanoidai.ai.ChatMessage
import com.humanoidai.alerts.AlertEngine
import com.humanoidai.alerts.AlertItem
import com.humanoidai.attention.AttentionManager
import com.humanoidai.companion.CompanionEngine
import com.humanoidai.companion.CompanionState
import com.humanoidai.context.ContextEngine
import com.humanoidai.hearing.AudioDirectionState
import com.humanoidai.hearing.CompassSector
import com.humanoidai.hearing.MicState
import com.humanoidai.hearing.SpeechRecognizerManager
import com.humanoidai.vision.*
import com.humanoidai.ml.*
import com.humanoidai.navigation.NavRoutes
import com.humanoidai.ui.components.*
import com.humanoidai.ui.customization.AppearanceSettings
import com.humanoidai.ui.customization.AppearanceViewModel
import com.humanoidai.ui.customization.HUDStructure
import com.humanoidai.ui.layoutcustomization.HUDRenderer
import com.humanoidai.ui.layouts.HomeLayoutPreset
import com.humanoidai.ui.theme.*
import com.humanoidai.ui.widgets.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

import com.humanoidai.ui.layoutcustomization.domain.adaptive.*
import com.humanoidai.ui.layoutcustomization.domain.adaptive.CameraState
import com.humanoidai.ui.layoutcustomization.domain.memory.MemoryEntry
import com.humanoidai.ui.layoutcustomization.domain.memory.MemoryType
import com.humanoidai.ui.layoutcustomization.domain.perception.AudioPerception
import com.humanoidai.ui.layoutcustomization.domain.perception.EnvironmentalPerception
import com.humanoidai.ui.layoutcustomization.domain.perception.PerceptionConfidence
import com.humanoidai.ui.layoutcustomization.domain.perception.VisualPerception
import com.humanoidai.ui.layoutcustomization.domain.roi.ROICandidate
import com.humanoidai.ui.layoutcustomization.presentation.event.LayoutCustomizationEvent
import com.humanoidai.ui.layoutcustomization.presentation.viewmodel.LayoutCustomizationViewModel
import com.humanoidai.voice.VoiceEngine
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvironmentScreen(

    navController: NavController,
    recognitionManager: FaceRecognitionManager,
    ownerManager: OwnerEnrollmentManager,
    enrollmentManager: FaceEnrollmentManager,
    alertEngine: AlertEngine,
    aiManager: AIManager,
    companionEngine: CompanionEngine,
    contextEngine: ContextEngine,
    voiceEngine: VoiceEngine,
    microphoneManager: SpeechRecognizerManager,
    attentionManager: AttentionManager,
    appearanceViewModel: AppearanceViewModel = viewModel(
        factory = AppearanceViewModel.Factory(LocalContext.current),
    ),
    layoutViewModel: LayoutCustomizationViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val settings by appearanceViewModel.settings.collectAsState()
    val customization by layoutViewModel.state.collectAsState()
    val hudComponents by layoutViewModel.hudComponents.collectAsState()
    val haptic = LocalHapticFeedback.current
    val roiDecisions by layoutViewModel.roiDecisions.collectAsState()
    val workspace by layoutViewModel.currentWorkspace.collectAsState()
    val intentState by layoutViewModel.currentIntent.collectAsState()
    val decisionState by layoutViewModel.decisionState.collectAsState()
    val perceptionState by layoutViewModel.perceptionState.collectAsState()
    val currentContext by contextEngine.currentContext.collectAsState()
    val companionState by companionEngine.state.collectAsState()







    val messages by aiManager.getMessages().collectAsState()
    val micState by microphoneManager.state.collectAsState()
    val isListeningActual by microphoneManager.isListening.collectAsState()
    
    // Interaction 2.0: Instant feedback for listening
    val isListening = isListeningActual || micState == MicState.STARTING
    
    val ambientNoise by microphoneManager.ambientNoise.collectAsState()
    val attentionFocus by attentionManager.currentFocus.collectAsState()
    val unreadCount by alertEngine.unreadCount.collectAsState()

    // Sync Appearance Structure with Layout Customization (Phase 2B Integration)
    LaunchedEffect(settings.hudStructure) {
        if (customization.selectedLayout != settings.hudStructure.name) {
            layoutViewModel.onEvent(LayoutCustomizationEvent.ChangeSelectedLayout(settings.hudStructure.name))
        }
    }

    var inputText by remember { mutableStateOf("") }
    var detectedPersons by remember { mutableStateOf<List<DetectedPerson>>(emptyList()) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_FRONT) }

    val aiStatusText = remember(companionState, detectedPersons) {
        when (companionState) {
            CompanionState.LISTENING -> "LISTENING"
            CompanionState.THINKING -> "THINKING"
            CompanionState.SPEAKING -> "SPEAKING"
            CompanionState.WAKING -> "WAKING"
            else -> if (detectedPersons.isEmpty()) "SCANNING" else "ENGAGED"
        }
    }

    // Adaptive HUD Context Synchronization (Phase 3A)
    LaunchedEffect(detectedPersons, isListening, attentionFocus, unreadCount, ambientNoise, currentContext) {
        val detection = when {
            detectedPersons.isEmpty() -> DetectionState.NONE
            detectedPersons.size == 1 -> DetectionState.SINGLE_OBJECT
            else -> DetectionState.MULTIPLE_OBJECTS
        }
        
        val assistant = when {
            isListening -> AssistantState.LISTENING
            attentionFocus.name == "THINKING" -> AssistantState.THINKING
            attentionFocus.name == "SPEAKING" -> AssistantState.SPEAKING
            else -> AssistantState.IDLE
        }
        
        val alertLevel = when {
            unreadCount == 0 -> AlertLevel.NONE
            unreadCount < 3 -> AlertLevel.LOW
            unreadCount < 5 -> AlertLevel.MEDIUM
            else -> AlertLevel.HIGH
        }

        val contextSnapshot = HUDContext(
            camera = CameraState.ACTIVE,
            detection = detection,
            assistant = assistant,
            alertLevel = alertLevel
        )
        
        layoutViewModel.updateContext(contextSnapshot)
        
        // Multi-Modal Perception Update (Phase 4C)
        layoutViewModel.updateVisualPerception(
            VisualPerception(
                primaryROI = roiDecisions.find { it.isPrimary },
                secondaryROIs = roiDecisions.filter { !it.isPrimary },
                confidence = PerceptionConfidence.HIGH
            )
        )
        
        layoutViewModel.updateAudioPerception(
            AudioPerception(
                voiceActive = isListening,
                intensity = ambientNoise,
                confidence = PerceptionConfidence.MEDIUM
            )
        )
        
        layoutViewModel.updateEnvironmentPerception(
            EnvironmentalPerception(
                batteryLevel = currentContext.batteryPercent,
                orientation = "PORTRAIT", // Placeholder
                confidence = PerceptionConfidence.HIGH
            )
        )
    }



    val faceAnalyzer = remember {
        FaceAnalyzer(
            embeddingHelper = FaceEmbeddingHelper(context),
            recognitionManager = recognitionManager,
            enrollmentManager = enrollmentManager,
            contextEngine = contextEngine,
            onResults = { persons ->
                detectedPersons = persons
                
                // Smart ROI Integration (Phase 3B)
                val candidates = persons.mapIndexed { index, person ->
                    ROICandidate(
                        id = person.name + index, // Use name + index as temporary ID
                        type = "person",
                        confidence = person.confidence,
                        bounds = person.boundingBox
                    )
                }
                layoutViewModel.onDetectionsUpdated(candidates)
                
                // Real-time Alert Processing (Added)
                alertEngine.processDetections(persons)

                // Interactive Greeting Logic
                persons.find { 
                    it.isNewArrival && 
                    it.name != "UNKNOWN" && 
                    it.name != "STABLE" && 
                    it.name != "ANALYZING" &&
                    it.name != "IDENTIFYING"
                }?.let { newPerson ->
                    voiceEngine.speak("Hello ${newPerson.name}, welcome back.")
                } ?: persons.find { 
                    it.isNewArrival && 
                    (it.name == "UNKNOWN" || it.name == "ANALYZING" || it.name == "IDENTIFYING") 
                }?.let {
                    voiceEngine.speak("Hello there. I don't recognize you yet.")
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()
            } catch (_: Exception) {}
            
            faceAnalyzer.release()
        }
    }

    LaunchedEffect(lensFacing, previewView) {
        val pv = previewView ?: return@LaunchedEffect
        
        // Update FaceAnalyzer with current camera orientation
        faceAnalyzer.isFrontCamera = lensFacing == CameraSelector.LENS_FACING_FRONT

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.surfaceProvider = pv.surfaceProvider }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build().also { it.setAnalyzer(Executors.newSingleThreadExecutor(), faceAnalyzer) }
            
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.Builder().requireLensFacing(lensFacing).build(), preview, analysis)
            } catch (e: Exception) { e.printStackTrace() }
        }, ContextCompat.getMainExecutor(context))
    }

    LaunchedEffect(Unit) {
        companionEngine.wake()
        layoutViewModel.recordMemory(
            MemoryEntry(
                id = "session_start_${System.currentTimeMillis()}",
                type = MemoryType.SESSION,
                source = "environment_screen",
                payload = "Camera session initialized. AI Agent waking."
            )
        )
    }

    var radarSector by remember { mutableStateOf(CompassSector.UNKNOWN) }

    LaunchedEffect(ambientNoise) {
        radarSector = if (ambientNoise > 40f) {
            // Cycle through sectors based on noise intensity to simulate "direction finding"
            val sectors = CompassSector.entries.filter { it != CompassSector.UNKNOWN }
            sectors[(System.currentTimeMillis() / 200 % sectors.size).toInt()]
        } else {
            CompassSector.UNKNOWN
        }
    }

    val latestAlertState by alertEngine.latestAlert.collectAsState()
    val audioState = remember(radarSector, ambientNoise) {
        AudioDirectionState(
            sector = radarSector,
            amplitudeDb = ambientNoise
        )
    }

    SidePanelDrawer(navController, drawerState, unreadCount) {
        Scaffold(
            containerColor = Color.Transparent
        ) { innerPadding ->
            // innerPadding is used to satisfy Scaffold requirement, but we use absolute positioning for HUD
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding.run { PaddingValues(0.dp) })) {
                HUDRenderer(
                    components = hudComponents,
                    detectedPersons = detectedPersons,
                    settings = settings,
                    audioState = audioState,
                    messages = messages,
                    battery = currentContext.batteryPercent,
                    noise = currentContext.noiseLevel.toInt(),
                    status = aiStatusText,
                    inputText = inputText,
                    onInputChanged = { inputText = it },
                    onSend = {
                        val q = inputText.trim().lowercase()
                        Log.i("EnvironmentScreen", "onSend: $q")
                        inputText = ""
                        if (q.contains("what") && q.contains("miss")) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            navController.navigate(NavRoutes.COMMUNICATION_BRIEFING)
                        } else {
                            companionEngine.ask(q, ownerManager.getOwnerName(), "Home")
                        }
                    },
                    onMicToggle = {
                        if (isListening) {
                            microphoneManager.stopListening()
                        } else {
                            microphoneManager.startListening(
                                onPartialResult = { inputText = it },
                                onFinalResult = { final ->
                                    when {
                                        final == "RETRY_PROMPT" -> {
                                            voiceEngine.speak("I'm sorry, I didn't catch that.")
                                        }
                                        final.isNotBlank() -> {
                                            val query = final.lowercase()
                                            if (query.contains("what") && query.contains("miss")) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                navController.navigate(NavRoutes.COMMUNICATION_BRIEFING)
                                            } else {
                                                inputText = ""
                                                companionEngine.ask(final, ownerManager.getOwnerName(), "Home")
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    },
                    onAdd = {
                        navController.navigate(NavRoutes.LAYOUT_CUSTOMIZATION)
                    },
                    onMenu = { scope.launch { drawerState.open() } },
                    onSwitchCamera = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                            CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                    },
                    onNavigate = { navController.navigate(it) },
                    latestAlert = latestAlertState?.title,
                    workspaceName = workspace.name,
                    intentName = intentState.activeIntent.category.name,
                    focusId = decisionState.activeDecisions.keys.firstOrNull(),
                    activeDecisions = decisionState.activeDecisions.values.toList(),
                    perceptionState = perceptionState,
                    previewView = { previewView = it },
                    layoutPreset = HomeLayoutPreset.fromId(customization.selectedPreset),
                    scale = settings.uiScale.factor,
                    debugEnabled = false,
                    companionState = companionState
                )

            }
        }
    }
}




@Composable
fun HudTopBar(
    settings: AppearanceSettings, 
    status: String, 
    isThinking: Boolean,
    onMenu: () -> Unit, 
    onSwitchCamera: () -> Unit,
    onSettings: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val accent = Color(settings.accentColor)
    val scale = settings.uiScale.factor
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = (12 * scale).dp, vertical = (2 * scale).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Compact Menu
        IconButton(
            onClick = { onMenu() }, 
            modifier = Modifier.size((40 * scale).dp)
        ) {
            Icon(Icons.Default.Menu, "Menu", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
        }
        
        Spacer(Modifier.weight(1f))
        
        // Central Identity (No GlassPanel for cleaner look - Phase 10)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "HUMANOID AI",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = (12 * scale).sp,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                if (isThinking) "THINKING..." else status,
                color = if (isThinking) WarningOrange else accent.copy(alpha = 0.7f),
                fontSize = (8 * scale).sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        
        Spacer(Modifier.weight(1f))
        
        // Compact Actions
        Row(
            horizontalArrangement = Arrangement.spacedBy((4 * scale).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSwitchCamera() 
                }, 
                modifier = Modifier.size((40 * scale).dp)
            ) {
                Icon(Icons.Default.Cameraswitch, "Switch", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
            }

            IconButton(
                onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSettings() 
                }, 
                modifier = Modifier.size((40 * scale).dp)
            ) {
                Icon(Icons.Default.Settings, "Settings", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
            }
        }
    }
}


@Composable
fun SystemControlBar(
    settings: AppearanceSettings,
    inputText: String,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onMicToggle: () -> Unit,
    onAdd: () -> Unit,
    isListening: Boolean,
    messages: List<ChatMessage>,
    battery: Int,
    noise: Int
) {
    val haptic = LocalHapticFeedback.current
    val accent = Color(settings.accentColor)
    val scale = settings.uiScale.factor
    
    Column(modifier = Modifier.padding(bottom = (8 * scale).dp)) {
        // Chat History (Subtle and clean)
        if (messages.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .heightIn(max = (140 * scale).dp)
                    .fillMaxWidth()
                    .padding(horizontal = (16 * scale).dp)
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy((4 * scale).dp),
                    reverseLayout = true
                ) {
                    items(messages.reversed()) { msg ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                if (msg.isUser) "YOU › " else "AI  › ",
                                color = if (msg.isUser) Color.White.copy(alpha = 0.3f) else accent.copy(alpha = 0.6f),
                                fontSize = (10 * scale).sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                msg.text,
                                color = if (msg.isUser) Color.White.copy(alpha = 0.8f) else Color.White,
                                fontSize = (11 * scale).sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height((12 * scale).dp))
        }

        // Primary Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = (8 * scale).dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((12 * scale).dp)
        ) {
            // Context Action (Add/Plus)
            IconButton(
                onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onAdd() 
                },
                modifier = Modifier
                    .size((48 * scale).dp)
                    .clip(CircleShape)
                    .background(SurfaceDark.copy(alpha = 0.4f))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape)
            ) {
                Icon(Icons.Default.Add, "Add Resource", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
            }
            
            // Central Command Panel
            GlassPanel(
                modifier = Modifier
                    .weight(1f)
                    .height((54 * scale).dp),
                settings = settings
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize().padding(horizontal = (16 * scale).dp)
                ) {
                    BasicTextField(
                        value = inputText,
                        onValueChange = onInputChanged,
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = (14 * scale).sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        ),
                        modifier = Modifier.weight(1f),
                        cursorBrush = SolidColor(accent),
                        decorationBox = { innerTextField ->
                            Box {
                                if (inputText.isEmpty()) {
                                    Text(
                                        "INITIALIZE COMMAND...",
                                        color = Color.White.copy(alpha = 0.2f),
                                        fontSize = (11 * scale).sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    
                    if (inputText.isNotEmpty()) {
                        IconButton(
                            onClick = { 
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSend() 
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = accent)
                        }
                    } else {
                        // Telemetry removed from main command bar to reduce clutter (Phase 10 Cleanup)
                    }
                }
            }

            // High-Visibility Microphone
            IconButton(
                onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onMicToggle() 
                },
                modifier = Modifier
                    .size((56 * scale).dp)
                    .clip(CircleShape)
                    .background(if (isListening) accent else accent.copy(alpha = 0.08f))
                    .border(2.dp, if (isListening) accent else accent.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    if (isListening) Icons.Default.MicNone else Icons.Default.Mic, 
                    "Microphone Interaction", 
                    tint = if (isListening) Color.Black else accent,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
private fun TelemetryIcon(icon: ImageVector, value: String, color: Color, scale: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color.copy(alpha = 0.6f), modifier = Modifier.size((14 * scale).dp))
        Spacer(Modifier.width(4.dp))
        Text(
            value,
            color = Color.White.copy(alpha = 0.4f),
            fontSize = (9 * scale).sp,
            fontFamily = FontFamily.Monospace
        )
    }
}



@Composable
fun CameraAperture(
    size: Dp,
    preset: HomeLayoutPreset, 
    preview: (PreviewView) -> Unit, 
    scale: Float, 
    accent: Color,
    glowEnabled: Boolean = true,
    isThinking: Boolean = false,
    isSpeaking: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "core_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = if (isThinking || isSpeaking) 1.0f else (if (glowEnabled) 0.8f else 0.4f),
        animationSpec = infiniteRepeatable(
            animation = tween(if (isThinking) 400 else (if (isSpeaking) 800 else 1500), easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    val thinkingRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isThinking) 2000 else 4000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    Box(contentAlignment = Alignment.Center) {
        // Outer Prominent Ring
        if (glowEnabled || isThinking || isSpeaking) {
            Box(
                modifier = Modifier
                    .size(size + (20 * scale).dp)
                    .then(if (isThinking || isSpeaking) Modifier.rotate(thinkingRotation) else Modifier)
                    .drawBehind {
                        if (isThinking || isSpeaking) {
                            drawArc(
                                color = if (isThinking) WarningOrange else accent,
                                startAngle = 0f, sweepAngle = 90f, useCenter = false,
                                style = Stroke(width = (if (isSpeaking) 3.dp else 2.dp).toPx(), cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = if (isThinking) WarningOrange else accent,
                                startAngle = 180f, sweepAngle = 90f, useCenter = false,
                                style = Stroke(width = (if (isSpeaking) 3.dp else 2.dp).toPx(), cap = StrokeCap.Round)
                            )
                        } else {
                            drawCircle(
                                color = accent.copy(alpha = pulseAlpha * 0.2f),
                                radius = (size.toPx() + (20 * scale).dp.toPx()) / 2
                            )
                        }
                    }
                    .border(
                        width = (1 * scale).dp,
                        color = if (isThinking) WarningOrange else (if (isSpeaking) accent else accent.copy(alpha = pulseAlpha)),
                        shape = CircleShape
                    )
            )
        }

        
        val roiShape = when(preset.roiShape) {
            "hexagon" -> HexagonShape()
            "square" -> RoundedCornerShape((16 * scale).dp)
            else -> CircleShape
        }
        
        Box(modifier = Modifier.size(size).clip(roiShape).border(2.dp, accent, roiShape)) {
            AndroidView(factory = { ctx -> PreviewView(ctx).also(preview) }, modifier = Modifier.fillMaxSize())
            // Technical metrics removed from camera center to keep vision clear (Phase 10 Cleanup)
        }
        HudCrosshair(accent.copy(alpha = 0.4f))
    }
}

@Composable
fun SecondaryRoiBlip(person: DetectedPerson, scale: Float, accent: Color, showConfidence: Boolean = true) {
    val color = if (person.name == "UNKNOWN") ErrorRed else accent
    // Adjusted for Vertical Sidebar layout
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width((70 * scale).dp)
    ) {
        Box(
            Modifier
                .size((54 * scale).dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.05f))
                .border(1.dp, color.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (person.faceBitmap != null) {
                Image(
                    bitmap = person.faceBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.Person, null, tint = color.copy(alpha = 0.4f), modifier = Modifier.size((20 * scale).dp))
            }
        }
        Spacer(Modifier.height((4 * scale).dp))
        Text(
            text = if (person.name == "UNKNOWN") "UNKNOWN" else person.name.uppercase(),
            color = Color.White,
            fontSize = (8 * scale).sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
        if (showConfidence) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = java.lang.String.format(Locale.getDefault(), "%.2f", person.confidence),
                    color = TextSecondary.copy(alpha = 0.8f),
                    fontSize = (7 * scale).sp,
                    fontFamily = FontFamily.Monospace
                )
                if (person.isLookingAtCamera) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Default.RemoveRedEye,
                        null,
                        tint = accent.copy(alpha = 0.6f),
                        modifier = Modifier.size((8 * scale).dp)
                    )
                }
            }
            Text(
                text = person.distanceCategory,
                color = if (person.distanceCategory == "NEAR") Color.Yellow else TextSecondary.copy(alpha = 0.5f),
                fontSize = (6 * scale).sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DotGridBackground(color: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val dotSize = 1.dp.toPx()
        val spacing = 20.dp.toPx()
        for (x in 0..size.width.toInt() step spacing.toInt()) {
            for (y in 0..size.height.toInt() step spacing.toInt()) {
                drawCircle(color, dotSize, Offset(x.toFloat(), y.toFloat()))
            }
        }
    }
}

