package com.humanoidai.ui.screens

import android.util.Log
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.graphics.RectF
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
import androidx.compose.ui.platform.LocalConfiguration
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
import com.humanoidai.platform.bridge.AuraEvent
import com.humanoidai.platform.bridge.AuraEventBus
import com.humanoidai.ui.components.*
import com.humanoidai.ui.customization.AppearanceSettings
import com.humanoidai.ui.customization.AppearanceViewModel
import com.humanoidai.ui.customization.HUDStructure
import com.humanoidai.ui.layoutcustomization.AlertBanner
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
import androidx.compose.ui.tooling.preview.Preview as ComposePreview

import com.humanoidai.platform.manager.AuraCapabilityManager
import com.humanoidai.platform.bridge.LocalAuraCapabilityBridge
import com.humanoidai.platform.capability.CameraCapability
import com.humanoidai.platform.model.Capability
import com.humanoidai.platform.model.CapabilityStatus
import com.humanoidai.platform.model.GestureEvent
import com.humanoidai.platform.model.VisualTarget
import com.humanoidai.platform.runtime.AuraAppContext
import com.humanoidai.platform.orchestration.AuraMultimodalOrchestrator

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
    layoutViewModel: LayoutCustomizationViewModel,
    capabilityManager: AuraCapabilityManager,
    multimodalOrchestrator: AuraMultimodalOrchestrator
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val bridge = LocalAuraCapabilityBridge.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var isDrawerOpen by remember { mutableStateOf(false) }
    val appId = "aura.core.hud"

    val multimodalOrchestrator = remember(aiManager, bridge) { 
        AuraMultimodalOrchestrator(aiManager, bridge) 
    }

    // Contextual Capability Acquisition for Core HUD
    LaunchedEffect(Unit) {
        AuraAppContext.enterForeground(appId)
        
        bridge.requestCapability(appId, Capability.CAMERA) { 
            if (it) AuraAppContext.onCapabilityActivated(Capability.CAMERA)
        }
        bridge.requestCapability(appId, Capability.VISION) {
            if (it) AuraAppContext.onCapabilityActivated(Capability.VISION)
        }
        bridge.requestCapability(appId, Capability.AI) {
            if (it) AuraAppContext.onCapabilityActivated(Capability.AI)
        }
        bridge.requestCapability(appId, Capability.GESTURE) {
            if (it) AuraAppContext.onCapabilityActivated(Capability.GESTURE)
        }
        bridge.requestCapability(appId, Capability.SENSORS) {
            if (it) AuraAppContext.onCapabilityActivated(Capability.SENSORS)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            capabilityManager.releaseAll(appId)
            AuraAppContext.clear()
        }
    }

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

    val mmIntent by multimodalOrchestrator.lastMultimodalIntent.collectAsState()
    val lockedTarget by multimodalOrchestrator.lockedTarget.collectAsState()
    val spatialContext by multimodalOrchestrator.spatialContext.collectAsState()
    val allTargets by multimodalOrchestrator.latestTargets.collectAsState()

    var lastGesture by remember { mutableStateOf<GestureEvent?>(null) }
    LaunchedEffect(Unit) {
        AuraEventBus.events.collect { event ->
            when (event) {
                is AuraEvent.GestureDetected -> {
                    lastGesture = event.gesture
                }
                is AuraEvent.NavigationRequested -> {
                    if (event.direction == "BACK") {
                        navController.popBackStack()
                    }
                }
                else -> {}
            }
        }
    }

    var ownerName by remember { mutableStateOf("Owner") }
    LaunchedEffect(Unit) {
        ownerName = ownerManager.getOwnerName()
    }

    val messages by aiManager.getMessages().collectAsState()
    val micState by microphoneManager.state.collectAsState()
    val isListeningActual by microphoneManager.isListening.collectAsState()
    val partialTranscript by microphoneManager.partialTranscript.collectAsState()
    
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

    // Interaction 2.0: Sync speech transcript to input field globally
    LaunchedEffect(partialTranscript) {
        if (partialTranscript.isNotEmpty()) {
            inputText = partialTranscript
        }
    }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var rearPreviewView by remember { mutableStateOf<PreviewView?>(null) }
    var isRearCameraActive by remember { mutableStateOf(false) }
    var rearCameraMessage by remember { mutableStateOf<String?>("HW CONCURRENCY UNSUPPORTED") }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_FRONT) }
    val cameraStatus by capabilityManager.getStatus(Capability.CAMERA).collectAsState()

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



    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

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
            },
            gestureRecognizer = GestureRecognizer(), // Should be shared
            objectRecognizer = ObjectRecognizer() // Should be shared
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
            analysisExecutor.shutdown()
        }
    }

    LaunchedEffect(previewView, rearPreviewView, cameraStatus) {
        val fv = previewView ?: return@LaunchedEffect
        val rv = rearPreviewView
        if (cameraStatus != CapabilityStatus.ACTIVE && cameraStatus != CapabilityStatus.AVAILABLE) {
            Log.d("EnvironmentScreen", "Waiting for camera capability status to become AVAILABLE or ACTIVE. Current status: $cameraStatus")
            return@LaunchedEffect
        }
        val cameraCap = capabilityManager.getCapability<CameraCapability>(Capability.CAMERA)
            ?: bridge.getCapability<CameraCapability>(Capability.CAMERA)
        if (cameraCap == null) {
            Log.w("EnvironmentScreen", "Camera capability status is $cameraStatus but getCapability returned null")
            return@LaunchedEffect
        }

        // Camera2 Hardware Diagnostic Probe
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            if (cameraManager != null) {
                val ids = cameraManager.cameraIdList
                Log.i("CameraDiagnostic", "CameraManager found ${ids.size} camera IDs: ${ids.joinToString()}")
                for (id in ids) {
                    val chars = cameraManager.getCameraCharacteristics(id)
                    val facing = chars.get(CameraCharacteristics.LENS_FACING)
                    val facingName = when (facing) {
                        CameraCharacteristics.LENS_FACING_FRONT -> "FRONT"
                        CameraCharacteristics.LENS_FACING_BACK -> "BACK"
                        CameraCharacteristics.LENS_FACING_EXTERNAL -> "EXTERNAL"
                        else -> "UNKNOWN ($facing)"
                    }
                    Log.i("CameraDiagnostic", "Camera ID: $id -> LENS_FACING: $facingName")
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val concurrentSet = cameraManager.concurrentCameraIds
                    Log.i("CameraDiagnostic", "Concurrent camera ID sets count: ${concurrentSet.size}")
                    for (set in concurrentSet) {
                        Log.i("CameraDiagnostic", "Concurrent camera ID combination: ${set.joinToString()}")
                    }
                } else {
                    Log.i("CameraDiagnostic", "Android SDK version < R (API 30). Concurrent camera API not present in OS framework.")
                }
            }
        } catch (e: Exception) {
            Log.e("CameraDiagnostic", "Error probing CameraManager", e)
        }
        
        Log.i("EnvironmentScreen", "Initializing dual camera pipelines: Front camera (Main) and Rear camera (PiP)...")
        faceAnalyzer.isFrontCamera = true

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()

                val frontSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()
                val frontPreview = cameraCap.getPreview().also { it.surfaceProvider = fv.surfaceProvider }
                val analysis = cameraCap.getImageAnalysis().also { it.setAnalyzer(analysisExecutor, faceAnalyzer) }

                // Bind front camera (Main Circle)
                val frontCamera = cameraProvider.bindToLifecycle(lifecycleOwner, frontSelector, frontPreview, analysis)
                frontCamera.cameraInfo.cameraState.observe(lifecycleOwner) { state ->
                    Log.i("CameraDiagnostic", "Front Camera State: ${state.type}, Error: ${state.error?.code}")
                }

                // Bind rear camera (Secondary Circle) if rearPreviewView is attached
                if (rv != null) {
                    val rearSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
                    val rearPreview = Preview.Builder().build().also { it.surfaceProvider = rv.surfaceProvider }
                    try {
                        val rearCamera = cameraProvider.bindToLifecycle(lifecycleOwner, rearSelector, rearPreview)
                        rearCamera.cameraInfo.cameraState.observe(lifecycleOwner) { state ->
                            Log.i("CameraDiagnostic", "Rear Camera State: ${state.type}, Error: ${state.error?.code}")
                            if (state.error != null) {
                                isRearCameraActive = false
                                rearCameraMessage = "HW UNSUPPORTED"
                                Log.w("EnvironmentScreen", "Rear Camera asynchronous error detected: ${state.error?.code}")
                            } else if (state.type.name == "OPEN") {
                                isRearCameraActive = true
                                rearCameraMessage = null
                            }
                        }
                    } catch (e: Exception) {
                        isRearCameraActive = false
                        rearCameraMessage = "HW UNSUPPORTED"
                        Log.w("EnvironmentScreen", "Confirmed Redmi Note 8 hardware HAL limitation: Device hardware does not support concurrent front and rear camera streaming in camera driver: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("EnvironmentScreen", "Failed to bind camera pipelines", e)
            }
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

    SidePanelDrawer(
        navController = navController,
        drawerState = drawerState,
        isOpen = isDrawerOpen,
        onDrawerClose = { isDrawerOpen = false },
        unreadCount = unreadCount
    ) {
        WithCameraPermission {
            Scaffold(
                containerColor = Color.Transparent
            ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
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
                        when {
                            q.contains("alert") -> navController.navigate(NavRoutes.ALERTS)
                            q.contains("assistant") || q.contains("chat") -> navController.navigate(NavRoutes.ASSISTANT)
                            q.contains("app") || q.contains("hub") -> navController.navigate(NavRoutes.APPS_HUB)
                            q.contains("setting") -> navController.navigate(NavRoutes.SETTINGS)
                            q.contains("enroll") -> navController.navigate(NavRoutes.ENROLLMENT)
                            q.contains("owner") -> navController.navigate(NavRoutes.OWNER_ENROLLMENT)
                            q.contains("recogniz") -> navController.navigate(NavRoutes.RECOGNITION)
                            q.contains("history") -> navController.navigate(NavRoutes.HISTORY)
                            q.contains("analytic") -> navController.navigate(NavRoutes.ANALYTICS)
                            q.contains("dashboard") -> navController.navigate(NavRoutes.DASHBOARD)
                            q.contains("privacy") -> navController.navigate(NavRoutes.PRIVACY_DASHBOARD)
                            q.contains("education") -> navController.navigate(NavRoutes.AURA_EDUCATION)
                            q.contains("finance") -> navController.navigate(NavRoutes.AURA_FINANCE)
                            q.contains("what") && q.contains("miss") -> {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                navController.navigate(NavRoutes.COMMUNICATION_BRIEFING)
                            }
                            else -> companionEngine.ask(q, ownerName, "Aura 360")
                        }
                    },
                    onMicToggle = {
                        if (isListening) {
                            microphoneManager.stopListening()
                        } else {
                            microphoneManager.startListening(
                                onPartialResult = { /* Handled by LaunchedEffect globally */ },
                                onFinalResult = { final ->
                                    when {
                                        final == "RETRY_PROMPT" -> {
                                            voiceEngine.speak("I'm sorry, I didn't catch that.")
                                        }
                                        final.lowercase().contains("what is this") || final.lowercase().contains("what am i looking at") -> {
                                            scope.launch {
                                                // Trigger visual grounding fusion
                                                val lastTarget = detectedPersons.firstOrNull()?.let {
                                                    VisualTarget(it.name, it.label, it.confidence, it.boundingBox)
                                                }
                                                multimodalOrchestrator.processVisualGrounding(final, ownerName, null, lastTarget)
                                            }
                                        }
                                        final.isNotBlank() -> {
                                            val query = final.lowercase()
                                            if (query.contains("what") && query.contains("miss")) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                navController.navigate(NavRoutes.COMMUNICATION_BRIEFING)
                                            } else {
                                                inputText = ""
                                                companionEngine.ask(final, ownerName, "Aura 360")
                                            }
                                        }
                                    }
                                },
                                onError = { /* Handled by MicrophoneManager internally */ }
                            )
                        }
                    },
                    onAdd = {
                        navController.navigate(NavRoutes.LAYOUT_CUSTOMIZATION)
                    },
                    onMenu = { 
                        Log.i("EnvironmentScreen", "onMenu triggered")
                        isDrawerOpen = true
                    },
                    onSwitchCamera = {
                        Log.i("EnvironmentScreen", "onSwitchCamera triggered")
                        val cameraCap = bridge.getCapability<CameraCapability>(Capability.CAMERA)
                        cameraCap?.switchLens()
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                            CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                    },
                    onNavigate = { 
                        Log.i("EnvironmentScreen", "onNavigate triggered to: $it")
                        navController.navigate(it) 
                    },
                    latestAlert = latestAlertState?.title,
                    workspaceName = workspace.name,
                    intentName = intentState.activeIntent.category.name,
                    focusId = decisionState.activeDecisions.keys.firstOrNull(),
                    activeDecisions = decisionState.activeDecisions.values.toList(),
                    perceptionState = perceptionState,
                    previewView = { previewView = it },
                    rearPreviewView = { rearPreviewView = it },
                    isRearCameraActive = isRearCameraActive,
                    rearCameraMessage = rearCameraMessage,
                    layoutPreset = HomeLayoutPreset.fromId(customization.selectedPreset),
                    scale = settings.uiScale.factor,
                    debugEnabled = false,
                    companionState = companionState,
                    lastGesture = lastGesture,
                    spatialContext = spatialContext,
                    lockedTarget = lockedTarget,
                    allTargets = allTargets
                )
                
                ArmsunFooter(modifier = Modifier.align(Alignment.BottomCenter))

            }
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
    val scale = settings.uiScale.factor
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = (12 * scale).dp, vertical = (4 * scale).dp)
    ) {
        // Compact Menu
        Box(
            modifier = Modifier
                .size((48 * scale).dp)
                .clip(CircleShape)
                .clickable {
                    Log.i("HudTopBar", "Menu clicked")
                    onMenu()
                }
                .align(Alignment.CenterStart),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Menu, "Menu", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(24.dp))
        }
        
        // Central Identity
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Text(
                "Aura 360",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (16 * scale).sp,
                letterSpacing = 0.5.sp
            )
            Text(
                status.ifBlank { "Online" },
                color = AccentCyan.copy(alpha = 0.85f),
                fontSize = (10 * scale).sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        // Compact Actions
        Row(
            horizontalArrangement = Arrangement.spacedBy((8 * scale).dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Box(
                modifier = Modifier
                    .size((48 * scale).dp)
                    .clip(CircleShape)
                    .clickable {
                        Log.i("HudTopBar", "Switch clicked")
                        onSwitchCamera()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Cameraswitch, "Switch", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(22.dp))
            }
            
            Box(
                modifier = Modifier
                    .size((48 * scale).dp)
                    .clip(CircleShape)
                    .clickable {
                        Log.i("HudTopBar", "Settings clicked")
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSettings()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Settings, "Settings", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(22.dp))
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
    
    Column(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(bottom = (8 * scale).dp)
    ) {
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
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            contentAlignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart
                        ) {
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 12.dp,
                                    topEnd = 12.dp,
                                    bottomStart = if (msg.isUser) 12.dp else 4.dp,
                                    bottomEnd = if (msg.isUser) 4.dp else 12.dp
                                ),
                                color = if (msg.isUser) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.75f),
                                border = BorderStroke(1.dp, if (msg.isUser) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f) else accent.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = msg.text,
                                    color = if (msg.isUser) MaterialTheme.colorScheme.onPrimaryContainer else Color.White,
                                    fontSize = (11 * scale).sp,
                                    lineHeight = 15.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
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
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), CircleShape)
            ) {
                Icon(Icons.Default.Add, "Add Resource", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
            }
            
            // Central Command Panel
            Box(modifier = Modifier.weight(1f)) {
                GlassPanel(
                    modifier = Modifier
                        .fillMaxWidth()
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
                                            "Ask Aura 360...",
                                            color = Color.White.copy(alpha = 0.35f),
                                            fontSize = (13 * scale).sp,
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
                        }
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
                    .size((54 * scale).dp)
                    .clip(CircleShape)
                    .background(if (isListening) accent else accent.copy(alpha = 0.12f))
                    .border(1.5.dp, if (isListening) accent else accent.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(
                    if (isListening) Icons.Default.MicNone else Icons.Default.Mic, 
                    "Microphone Interaction", 
                    tint = if (isListening) Color.Black else accent,
                    modifier = Modifier.size(24.dp)
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
    rearPreview: (PreviewView) -> Unit,
    scale: Float, 
    accent: Color,
    glowEnabled: Boolean = true,
    isThinking: Boolean = false,
    isSpeaking: Boolean = false,
    battery: Int = 0,
    noise: Int = 0,
    person: DetectedPerson? = null,
    lockedTarget: VisualTarget? = null,
    isCompact: Boolean = false,
    isRearCameraActive: Boolean = true,
    rearCameraMessage: String? = null
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val effectiveSize = if (isCompact) 100.dp else minOf(screenWidth * 0.85f, screenHeight * 0.45f, if (size > 0.dp) size else 320.dp).coerceAtLeast(280.dp)

    val infiniteTransition = rememberInfiniteTransition(label = "core_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isThinking) 600 else 1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(contentAlignment = Alignment.Center) {
        // Static HUD Ring (Matching attached image)
        Canvas(modifier = Modifier.size(effectiveSize + (100 * scale).dp)) {
            val strokeWidth = 1.2.dp.toPx()
            val ringColor = Color.White
            val radius = effectiveSize.toPx() / 2 + 1.dp.toPx()
            val cx = this.size.width / 2
            val cy = this.size.height / 2

            // Outer Circle Border (Thin white line)
            drawCircle(
                color = ringColor.copy(alpha = 0.3f),
                radius = radius,
                style = Stroke(width = strokeWidth)
            )
            
            // Side "Wings" (Long horizontal lines matching image)
            val lineLen = 36.dp.toPx()
            val gap = 12.dp.toPx()
            
            // Left Line
            drawLine(
                color = ringColor.copy(alpha = 0.5f),
                start = Offset(cx - radius - gap, cy),
                end = Offset(cx - radius - gap - lineLen, cy),
                strokeWidth = 1.2.dp.toPx()
            )
            
            // Right Line
            drawLine(
                color = ringColor.copy(alpha = 0.5f),
                start = Offset(cx + radius + gap, cy),
                end = Offset(cx + radius + gap + lineLen, cy),
                strokeWidth = 1.2.dp.toPx()
            )
        }

        // Inner Glow when thinking/speaking
        if (isThinking || isSpeaking) {
            Box(
                modifier = Modifier
                    .size(effectiveSize + (8 * scale).dp)
                    .drawBehind {
                        drawCircle(
                            color = accent.copy(alpha = pulseAlpha * 0.2f),
                            radius = effectiveSize.toPx() / 2 + (4 * scale).dp.toPx()
                        )
                    }
                    .border(1.dp, accent.copy(alpha = pulseAlpha * 0.4f), CircleShape)
            )
        }
        
        val roiShape = CircleShape
        
        Box(
            modifier = Modifier
                .size(effectiveSize)
                .clip(roiShape)
                .border(1.5.dp, Color.White.copy(alpha = 0.2f), roiShape),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx -> PreviewView(ctx).also(preview) }, 
                modifier = Modifier.fillMaxSize()
            )
            
            // ROI Brackets & Metadata (Matching attached image)
            if (lockedTarget != null) {
                TacticalBrackets(
                    modifier = Modifier.size(effectiveSize * 0.7f),
                    color = SuccessGreen
                )
                Text(
                    text = "LOCKED: ${lockedTarget.label}",
                    color = SuccessGreen,
                    fontSize = (11 * scale).sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp)
                )
            } else if (person != null) {
                val boxColor = if (person.name == "UNKNOWN") Color.Red else accent
                
                // Red Brackets around the face
                TacticalBrackets(
                    modifier = Modifier.size(effectiveSize * 0.5f),
                    color = boxColor
                )
                
                // UNKNOWN [50%] Label (Positioned to the left of the brackets)
                Text(
                    text = "${person.name} [${(person.confidence * 100).toInt()}%]",
                    color = boxColor,
                    fontSize = (10 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 10.dp)
                )
            }
        }

        // Secondary Circle: Rear-facing camera live preview or Device HW Limitation Notice
        if (!isCompact) {
            val circleBorder = if (isRearCameraActive) accent else WarningOrange
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .clip(CircleShape)
                    .border(2.dp, circleBorder, CircleShape)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (isRearCameraActive) {
                    AndroidView(
                        factory = { ctx -> PreviewView(ctx).also(rearPreview) },
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Text(
                            text = "REAR",
                            color = Color.White,
                            fontSize = (8 * scale).sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                } else {
                    // Explicit device-level hardware limitation notice
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "HW Dual Stream Unsupported",
                            tint = WarningOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "REAR FEED",
                            color = Color.White,
                            fontSize = (8 * scale).sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = rearCameraMessage ?: "HW UNSUPPORTED",
                            color = WarningOrange,
                            fontSize = (6.5 * scale).sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
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
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = (8 * scale).sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
        if (showConfidence) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (person.isLookingAtCamera) {
                    Icon(
                        Icons.Default.RemoveRedEye,
                        "Maintaining eye contact",
                        tint = accent.copy(alpha = 0.8f),
                        modifier = Modifier.size((10 * scale).dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Active",
                        color = accent.copy(alpha = 0.8f),
                        fontSize = (7 * scale).sp,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = person.distanceCategory,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = (7 * scale).sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
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

