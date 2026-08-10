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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.humanoidai.alerts.AlertEngine
import com.humanoidai.hearing.AudioDirectionState
import com.humanoidai.hearing.CompassSector
import com.humanoidai.vision.*
import com.humanoidai.ml.*
import com.humanoidai.ui.components.*
import com.humanoidai.ui.customization.AppearanceSettings
import com.humanoidai.ui.customization.AppearanceViewModel
import com.humanoidai.ui.customization.HUDStructure
import com.humanoidai.ui.layouts.HomeLayoutPreset
import com.humanoidai.ui.theme.*
import com.humanoidai.ui.widgets.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

import com.humanoidai.ui.layoutcustomization.domain.adaptive.*

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
    attentionManager: com.humanoidai.attention.AttentionManager,
    appearanceViewModel: AppearanceViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = AppearanceViewModel.Factory(LocalContext.current)
    ),
    layoutViewModel: com.humanoidai.ui.layoutcustomization.presentation.viewmodel.LayoutCustomizationViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val settings by appearanceViewModel.settings.collectAsState()
    val customization by layoutViewModel.state.collectAsState()
    val hudComponents by layoutViewModel.hudComponents.collectAsState()
    val roiDecisions by layoutViewModel.roiDecisions.collectAsState()
    val workspace by layoutViewModel.currentWorkspace.collectAsState()
    val intentState by layoutViewModel.currentIntent.collectAsState()
    val memoryState by layoutViewModel.memoryState.collectAsState()
    val decisionState by layoutViewModel.decisionState.collectAsState()
    val perceptionState by layoutViewModel.perceptionState.collectAsState()
    val currentContext by contextEngine.currentContext.collectAsState()






    val messages by aiManager.getMessages().collectAsState()
    val isListening by microphoneManager.isListening.collectAsState()
    val ambientNoise by microphoneManager.ambientNoise.collectAsState()
    val attentionFocus by attentionManager.currentFocus.collectAsState()
    val unreadCount by alertEngine.unreadCount.collectAsState()

    // Sync Appearance Structure with Layout Customization (Phase 2B Integration)
    LaunchedEffect(settings.hudStructure) {
        if (customization.selectedLayout != settings.hudStructure.name) {
            layoutViewModel.onEvent(com.humanoidai.ui.layoutcustomization.presentation.event.LayoutCustomizationEvent.ChangeSelectedLayout(settings.hudStructure.name))
        }
    }

    var inputText by remember { mutableStateOf("") }
    var aiStatusText by remember { mutableStateOf("OBSERVING") }
    var detectedPersons by remember { mutableStateOf<List<DetectedPerson>>(emptyList()) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_FRONT) }

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
            camera = com.humanoidai.ui.layoutcustomization.domain.adaptive.CameraState.ACTIVE,
            detection = detection,
            assistant = assistant,
            alertLevel = alertLevel
        )
        
        layoutViewModel.updateContext(contextSnapshot)
        
        // Multi-Modal Perception Update (Phase 4C)
        layoutViewModel.updateVisualPerception(
            com.humanoidai.ui.layoutcustomization.domain.perception.VisualPerception(
                primaryROI = roiDecisions.find { it.isPrimary },
                secondaryROIs = roiDecisions.filter { !it.isPrimary },
                confidence = com.humanoidai.ui.layoutcustomization.domain.perception.PerceptionConfidence.HIGH
            )
        )
        
        layoutViewModel.updateAudioPerception(
            com.humanoidai.ui.layoutcustomization.domain.perception.AudioPerception(
                voiceActive = isListening,
                intensity = ambientNoise,
                confidence = com.humanoidai.ui.layoutcustomization.domain.perception.PerceptionConfidence.MEDIUM
            )
        )
        
        layoutViewModel.updateEnvironmentPerception(
            com.humanoidai.ui.layoutcustomization.domain.perception.EnvironmentalPerception(
                batteryLevel = currentContext.batteryPercent,
                orientation = "PORTRAIT", // Placeholder
                confidence = com.humanoidai.ui.layoutcustomization.domain.perception.PerceptionConfidence.HIGH
            )
        )
    }



    val faceAnalyzer = remember {
        com.humanoidai.vision.FaceAnalyzer(
            embeddingHelper = FaceEmbeddingHelper(context),
            recognitionManager = recognitionManager,
            enrollmentManager = enrollmentManager,
            contextEngine = contextEngine,
            onResults = { persons ->
                detectedPersons = persons
                aiStatusText = if (persons.isEmpty()) "SCANNING" else "ENGAGED"
                
                // Smart ROI Integration (Phase 3B)
                val candidates = persons.mapIndexed { index, person ->
                    com.humanoidai.ui.layoutcustomization.domain.roi.ROICandidate(
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
                persons.find { it.isNewArrival && it.name != "UNKNOWN" }?.let { newPerson ->
                    voiceEngine.speak("Hello ${newPerson.name}, welcome back.")
                } ?: persons.find { it.isNewArrival && it.name == "UNKNOWN" }?.let {
                    voiceEngine.speak("Hello there. I don't recognize you yet.")
                }
            }
        )
    }

    LaunchedEffect(lensFacing, previewView) {
        val pv = previewView ?: return@LaunchedEffect
        
        // Update FaceAnalyzer with current camera orientation
        faceAnalyzer.isFrontCamera = lensFacing == CameraSelector.LENS_FACING_FRONT

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(pv.surfaceProvider) }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
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
            com.humanoidai.ui.layoutcustomization.domain.memory.MemoryEntry(
                id = "session_start_${System.currentTimeMillis()}",
                type = com.humanoidai.ui.layoutcustomization.domain.memory.MemoryType.SESSION,
                source = "environment_screen",
                payload = "Camera session initialized. AI Agent waking."
            )
        )
    }

    var radarSector by remember { mutableStateOf(CompassSector.UNKNOWN) }

    LaunchedEffect(ambientNoise) {
        if (ambientNoise > 40f) {
            // Cycle through sectors based on noise intensity to simulate "direction finding"
            val sectors = CompassSector.values().filter { it != CompassSector.UNKNOWN }
            radarSector = sectors[(System.currentTimeMillis() / 200 % sectors.size).toInt()]
        } else {
            radarSector = CompassSector.UNKNOWN
        }
    }

    SidePanelDrawer(navController, drawerState, unreadCount) {

        Scaffold(
            containerColor = Color.Black.copy(alpha = settings.backgroundOpacity)
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (settings.showGrid) {
                    DotGridBackground(Color(settings.accentColor).copy(alpha = 0.05f))
                }

                // Unified Modular HUD Renderer
                val alerts by alertEngine.alerts.collectAsState()
                
                com.humanoidai.ui.layoutcustomization.HUDRenderer(
                    components = hudComponents,
                    detectedPersons = detectedPersons,
                    settings = settings,
                    audioState = AudioDirectionState(
                        sector = radarSector,
                        amplitudeDb = ambientNoise
                    ),
                    messages = messages,
                    battery = currentContext.batteryPercent,
                    noise = ambientNoise.toInt(),
                    isListening = isListening,
                    status = attentionFocus.name,
                    inputText = inputText,
                    onInputChanged = { inputText = it },
                    onSend = {
                        val q = inputText
                        inputText = ""
                        scope.launch { aiManager.ask(q, ownerManager.getOwnerName(), "Home") }
                    },
                    onMicToggle = {
                        if (isListening) {
                            microphoneManager.stopListening()
                        } else {
                            microphoneManager.startListening(
                                onPartialResult = { inputText = it },
                                onFinalResult = { final ->
                                    if (final.isBlank()) return@startListening
                                    inputText = ""
                                    scope.launch {
                                        aiManager.ask(final, ownerManager.getOwnerName(), "Home") {
                                            microphoneManager.stopListening()
                                            microphoneManager.startPassiveListening { voiceEngine.speak("Acknowledged.") }
                                        }
                                    }
                                }
                            )
                        }
                    },
                    onAdd = {
                        // Quick Action: Layout Customization
                        navController.navigate(com.humanoidai.navigation.NavRoutes.LAYOUT_CUSTOMIZATION)
                    },
                    onMenu = { scope.launch { drawerState.open() } },
                    onSwitchCamera = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) 
                            CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                    },
                    onNavigate = { route -> navController.navigate(route) },
                    workspaceName = workspace.name,
                    intentName = intentState.activeIntent.category.name,
                    focusId = memoryState.activeFocusId,
                    activeDecisions = decisionState.activeDecisions.values.toList(),
                    perceptionState = perceptionState,
                    previewView = { previewView = it },
                    layoutPreset = HomeLayoutPreset.fromId(settings.layoutPreset),
                    scale = settings.uiScale.factor,
                    debugEnabled = false,
                    latestAlert = alerts.firstOrNull()?.title
                )
            }
        }
    }
}


@Composable
fun HudTopBar(
    settings: AppearanceSettings, 
    status: String, 
    onMenu: () -> Unit, 
    onSwitchCamera: () -> Unit,
    onSettings: () -> Unit
) {
    val accent = Color(settings.accentColor)
    val scale = settings.uiScale.factor
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = (20 * scale).dp, vertical = (10 * scale).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMenu, modifier = Modifier.size((48 * scale).dp)) {
            Icon(Icons.Default.Menu, null, tint = Color.White)
        }
        Spacer(Modifier.width((16 * scale).dp))
        GlassPanel(settings = settings) {
            Row(modifier = Modifier.padding(horizontal = (12 * scale).dp, vertical = (6 * scale).dp), verticalAlignment = Alignment.CenterVertically) {
                Text(status, color = accent, fontWeight = FontWeight.Bold, fontSize = (12 * scale).sp, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.width((8 * scale).dp))
                Box(Modifier.size((6 * scale).dp).background(SuccessGreen, CircleShape))
            }
        }
        Spacer(Modifier.weight(1f))
        
        IconButton(onClick = onSwitchCamera, modifier = Modifier.size((48 * scale).dp)) {
            Icon(Icons.Default.Cameraswitch, null, tint = Color.White)
        }
        
        Spacer(Modifier.width((8 * scale).dp))

        IconButton(onClick = onSettings, modifier = Modifier.size((48 * scale).dp)) {
            Icon(Icons.Default.Settings, null, tint = Color.White)
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
    messages: List<com.humanoidai.ai.ChatMessage>,
    battery: Int,
    noise: Int
) {
    val accent = Color(settings.accentColor)
    val scale = settings.uiScale.factor
    Column(modifier = Modifier.padding(bottom = (20 * scale).dp)) {
        if (inputText.isNotEmpty() || messages.isNotEmpty()) {
            Box(modifier = Modifier.heightIn(max = (120 * scale).dp).fillMaxWidth().padding(horizontal = (24 * scale).dp)) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy((4 * scale).dp)) {
                    items(messages) { msg ->
                        Text(
                            "${if (msg.isUser) "YOU" else "AI"} > ${msg.text}",
                            color = if (msg.isUser) Color.White.copy(alpha = 0.6f) else accent,
                            fontSize = (11 * scale).sp, fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height((16 * scale).dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = (24 * scale).dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((12 * scale).dp)
        ) {
            IconButton(onClick = onAdd, modifier = Modifier.size((48 * scale).dp).background(SurfaceDark.copy(alpha = 0.5f), CircleShape)) {
                Icon(Icons.Default.Add, null, tint = Color.White)
            }
            
            GlassPanel(modifier = Modifier.weight(1f).height((50 * scale).dp), settings = settings) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = (16 * scale).dp)) {
                    BasicTextField(
                        value = inputText,
                        onValueChange = onInputChanged,
                        textStyle = TextStyle(color = Color.White, fontSize = (14 * scale).sp),
                        modifier = Modifier.weight(1f),
                        cursorBrush = SolidColor(accent),
                        decorationBox = { if (inputText.isEmpty()) Text("Command...", color = Color.White.copy(alpha = 0.2f), fontSize = (14 * scale).sp) ; it() }
                    )
                    if (inputText.isNotEmpty()) {
                        IconButton(onClick = onSend) { Icon(Icons.Default.ArrowForward, null, tint = accent) }
                    }
                }
            }

            IconButton(
                onClick = onMicToggle, 
                modifier = Modifier
                    .size((48 * scale).dp)
                    .background(if (isListening) accent else accent.copy(alpha = 0.1f), CircleShape)
                    .border(1.dp, accent, CircleShape)
            ) {
                Icon(
                    if (isListening) Icons.Default.MicNone else Icons.Default.Mic, 
                    null, 
                    tint = if (isListening) Color.Black else accent
                )
            }
        }
    }
}

@Composable
fun StatusText(label: String, value: String) {
    Column {
        Text(label, fontSize = 8.sp, color = TextSecondary)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun CameraAperture(
    size: androidx.compose.ui.unit.Dp, 
    preset: HomeLayoutPreset, 
    preview: (PreviewView) -> Unit, 
    scale: Float, 
    accent: Color,
    glowEnabled: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "core_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = if (glowEnabled) 0.8f else 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(contentAlignment = Alignment.Center) {
        // Outer Prominent Ring
        if (glowEnabled) {
            Box(
                modifier = Modifier
                    .size(size + (20 * scale).dp)
                    .border(
                        width = (1 * scale).dp,
                        color = accent.copy(alpha = pulseAlpha),
                        shape = CircleShape
                    )
            )
        }
        
        val roiShape = when(preset.roiShape) {
            "hexagon" -> com.humanoidai.ui.components.HexagonShape()
            "square" -> RoundedCornerShape((16 * scale).dp)
            else -> CircleShape
        }
        
        Box(modifier = Modifier.size(size).clip(roiShape).border(2.dp, accent, roiShape)) {
            AndroidView(factory = { ctx -> PreviewView(ctx).also(preview) }, modifier = Modifier.fillMaxSize())
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size((size.value * 0.45).dp)
                    .border(1.dp, accent.copy(alpha = 0.3f), roiShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "CORE",
                    color = accent.copy(alpha = 0.5f),
                    fontSize = (9 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
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
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
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
                    text = String.format("%.2f", person.confidence),
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

