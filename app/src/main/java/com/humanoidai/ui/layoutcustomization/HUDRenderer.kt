package com.humanoidai.ui.layoutcustomization

import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.viewinterop.AndroidView
import com.humanoidai.ai.ChatMessage
import com.humanoidai.companion.CompanionState
import com.humanoidai.ui.layoutcustomization.domain.blueprint.WidgetAnchor
import com.humanoidai.ui.layoutcustomization.domain.blueprint.WidgetBlueprint
import com.humanoidai.ui.layoutcustomization.domain.hud.HUDComponentRegistry
import com.humanoidai.ui.layoutcustomization.domain.reasoning.Decision
import com.humanoidai.ui.layouts.HomeLayoutPreset
import com.humanoidai.ui.theme.*
import com.humanoidai.ui.screens.CameraAperture
import com.humanoidai.ui.screens.SecondaryRoiBlip
import com.humanoidai.ui.components.AuraLensOverlay
import com.humanoidai.vision.DetectedPerson
import com.humanoidai.ui.customization.AppearanceSettings
import com.humanoidai.hearing.AudioDirectionState
import com.humanoidai.hearing.CompassSector
import com.humanoidai.ui.components.AudioDirectionRadar
import com.humanoidai.ui.widgets.HudCrosshair
import com.humanoidai.ui.widgets.GlassPanel
import com.humanoidai.ui.screens.HudTopBar
import com.humanoidai.ui.screens.SystemControlBar
import com.humanoidai.navigation.NavRoutes
import com.humanoidai.ui.layoutcustomization.domain.perception.PerceptionState
import com.humanoidai.ui.widgets.TacticalBrackets
import com.humanoidai.platform.model.GestureType
import com.humanoidai.platform.model.GestureEvent
import com.humanoidai.platform.model.MovementState
import com.humanoidai.platform.model.SpatialContext
import com.humanoidai.platform.model.VisualTarget

/**
 * The modular HUD Renderer.
 * Consumes components from the Manager and draws them according to their blueprints.
 * Part of Phase 2B.
 */
@Composable
fun HUDRenderer(
    components: List<WidgetBlueprint>,
    detectedPersons: List<DetectedPerson>,
    settings: AppearanceSettings,
    audioState: AudioDirectionState,
    messages: List<ChatMessage>,
    battery: Int,
    noise: Int,
    status: String,
    inputText: String,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onMicToggle: () -> Unit,
    onAdd: () -> Unit,
    onMenu: () -> Unit,
    onSwitchCamera: () -> Unit,
    onNavigate: (String) -> Unit,
    latestAlert: String? = null,
    workspaceName: String = "IDLE",
    intentName: String = "IDLE",
    focusId: String? = null,
    activeDecisions: List<Decision> = emptyList(),
    perceptionState: PerceptionState? = null,
    previewView: (PreviewView) -> Unit,
    rearPreviewView: (PreviewView) -> Unit = {},
    layoutPreset: HomeLayoutPreset,
    scale: Float, // UI Global Scale
    debugEnabled: Boolean = false,
    companionState: CompanionState = CompanionState.OBSERVING,
    lastGesture: GestureEvent? = null,
    spatialContext: SpatialContext? = null,
    lockedTarget: VisualTarget? = null,
    allTargets: List<VisualTarget> = emptyList(),
    isRearCameraActive: Boolean = true,
    rearCameraMessage: String? = null
) {
    val isThinking = companionState == CompanionState.THINKING
    val isSpeaking = companionState == CompanionState.SPEAKING
    val isListening = companionState == CompanionState.LISTENING

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing) // P0: Ensure safe areas
    ) {
        // Phase 8: Static Background Elements
        if (settings.showGrid) {
            HudGridOverlay(accent = Color(settings.accentColor))
        }

        // Phase 7: Spatial AR Overlay
        AuraLensOverlay(
            targets = allTargets,
            lockedTarget = lockedTarget,
            spatialContext = spatialContext,
            scale = scale
        )

        // Draw standard components with animations
        components.forEach { widget ->
            val alignment = when (widget.anchor) {
                WidgetAnchor.TOP_START -> Alignment.TopStart
                WidgetAnchor.TOP_CENTER -> Alignment.TopCenter
                WidgetAnchor.TOP_END -> Alignment.TopEnd
                WidgetAnchor.CENTER_START -> Alignment.CenterStart
                WidgetAnchor.CENTER -> Alignment.Center
                WidgetAnchor.CENTER_END -> Alignment.CenterEnd
                WidgetAnchor.BOTTOM_START -> Alignment.BottomStart
                WidgetAnchor.BOTTOM_CENTER -> Alignment.BottomCenter
                WidgetAnchor.BOTTOM_END -> Alignment.BottomEnd
                else -> Alignment.Center
            }

            // P0: Structural Separation Logic
            val topPadding = if (widget.anchor == WidgetAnchor.TOP_CENTER && widget.id != HUDComponentRegistry.STATUS_BAR) {
                12.dp
            } else {
                0.dp
            }

            val fillWidthModifier = if (widget.id == HUDComponentRegistry.STATUS_BAR || widget.id == HUDComponentRegistry.ASSISTANT) {
                Modifier.fillMaxWidth()
            } else {
                Modifier
            }

            val isKeyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
            val isWidgetVisible = widget.isVisible && (!isKeyboardOpen || widget.id == HUDComponentRegistry.ASSISTANT || widget.id == HUDComponentRegistry.CAMERA)

            AnimatedVisibility(
                visible = isWidgetVisible,
                enter = fadeIn(tween(400)) + scaleIn(tween(400, easing = FastOutSlowInEasing), initialScale = 0.9f),
                exit = fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.9f),
                modifier = Modifier
                    .then(fillWidthModifier)
                    .align(alignment)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(top = topPadding)
                    .offset(x = widget.offsetX, y = widget.offsetY)
                    .zIndex(widget.priority.toFloat())
                    .graphicsLayer {
                        alpha = widget.alpha
                        scaleX = widget.scale
                        scaleY = widget.scale
                    }
            ) {
                Box(modifier = Modifier.animateContentSize()) {
                    when (widget.id) {
                        HUDComponentRegistry.CAMERA -> {
                            CameraAperture(
                                size = if (isKeyboardOpen) 100.dp else widget.width.coerceAtMost(320.dp),
                                preset = layoutPreset,
                                preview = previewView,
                                rearPreview = rearPreviewView,
                                scale = if (isKeyboardOpen) 0.5f else (scale * widget.scale),
                                accent = Color(settings.accentColor),
                                glowEnabled = settings.glowEnabled,
                                isThinking = isThinking,
                                isSpeaking = isSpeaking,
                                battery = battery,
                                noise = noise,
                                person = detectedPersons.firstOrNull(),
                                lockedTarget = lockedTarget,
                                isCompact = isKeyboardOpen,
                                isRearCameraActive = isRearCameraActive,
                                rearCameraMessage = rearCameraMessage
                            )
                        }

                        HUDComponentRegistry.PRIMARY_ROI -> {
                            if (settings.showRoiBox) {
                                PrimaryRoiOverlay(
                                    person = detectedPersons.firstOrNull(),
                                    settings = settings,
                                    scale = scale * widget.scale
                                )
                            }
                        }
                        HUDComponentRegistry.STATUS_BAR -> {
                            // Handled by fixed top header
                        }
                        HUDComponentRegistry.ASSISTANT -> {
                            SystemControlBar(
                                settings = settings,
                                inputText = inputText,
                                onInputChanged = onInputChanged,
                                onSend = onSend,
                                onMicToggle = onMicToggle,
                                onAdd = onAdd,
                                isListening = isListening,
                                messages = messages,
                                battery = battery,
                                noise = noise
                            )
                        }
                        HUDComponentRegistry.RADAR -> {
                            if (settings.showRadar) {
                                Box(modifier = Modifier.graphicsLayer {
                                    rotationZ = if (audioState.sector != CompassSector.UNKNOWN) 5f else 0f
                                }) {
                                    AudioDirectionRadar(state = audioState)
                                }
                            }
                        }
                        HUDComponentRegistry.ALERTS -> {
                            if (settings.showAlertBanner && latestAlert != null && !latestAlert.contains("STABLE", ignoreCase = true)) {
                                AlertBanner(
                                    message = latestAlert,
                                    scale = scale * widget.scale,
                                    onClick = { onNavigate(NavRoutes.ALERTS) }
                                )
                            }
                        }
                        HUDComponentRegistry.FPS_COUNTER -> {
                            // Production clean: hidden unless debugging
                        }
                        HUDComponentRegistry.RECORDING_INDICATOR -> {
                            if (isListening) {
                                RecordingIndicator(scale = scale * widget.scale)
                            }
                        }
                        HUDComponentRegistry.STATUS_INDICATORS -> {
                            // Redundant as it's now inside CameraAperture
                        }
                        HUDComponentRegistry.NOTIFICATIONS -> {
                            if (settings.showLiveContext) {
                                LiveContextSummary(
                                    scale = scale * widget.scale,
                                    onClick = { onNavigate(NavRoutes.ALERTS) }
                                )
                            }
                        }
                        HUDComponentRegistry.SECONDARY_ROI -> {
                            FourRoiContainer(
                                persons = detectedPersons,
                                scale = scale * widget.scale,
                                settings = settings,
                                cameraSize = components.find { it.id == HUDComponentRegistry.CAMERA }?.width ?: 280.dp
                            )
                        }
                    }
                }
                
                // Show gesture feedback globally in its zone (Phase 5)
                if (widget.id == HUDComponentRegistry.NOTIFICATIONS) {
                    GestureIndicator(
                        gesture = lastGesture,
                        scale = scale,
                        accent = Color(settings.accentColor)
                    )
                }

                if (debugEnabled) {
                    DebugOverlay(widget)
                }
            }
        }
        
        if (debugEnabled) {
            WorkspaceDebugTag(workspaceName, intentName, focusId, activeDecisions, perceptionState)
        }

        // Fixed Top Status Bar (At absolute top of Z-stack)
        if (settings.showAiStatus) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .zIndex(1000f)
            ) {
                HudTopBar(
                    settings = settings,
                    status = status,
                    isThinking = isThinking,
                    onMenu = onMenu,
                    onSwitchCamera = onSwitchCamera,
                    onSettings = { onNavigate(NavRoutes.SETTINGS) }
                )
            }
        }
    }
}

@Composable
fun HudGridOverlay(accent: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val step = 40.dp.toPx()
        val gridColor = accent.copy(alpha = 0.05f)
        for (x in 0..(size.width / step).toInt()) {
            drawLine(gridColor, Offset(x * step, 0f), Offset(x * step, size.height), 0.5.dp.toPx())
        }
        for (y in 0..(size.height / step).toInt()) {
            drawLine(gridColor, Offset(0f, y * step), Offset(size.width, y * step), 0.5.dp.toPx())
        }
    }
}

@Composable
fun PrimaryRoiOverlay(
    person: DetectedPerson?,
    settings: AppearanceSettings,
    scale: Float
) {
    if (person == null) return
    
    val accent = Color(settings.accentColor)
    val box = person.boundingBox
    
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val w = this.maxWidth
        val h = this.maxHeight
        
        val left = w * box.left
        val top = h * box.top
        val width = w * box.width()
        val height = h * box.height()
        
        Box(
            modifier = Modifier
                .size(width, height)
                .offset(left, top)
        ) {
            if (settings.showBrackets) {
                TacticalBrackets(
                    modifier = Modifier.fillMaxSize(),
                    color = if (person.name == "UNKNOWN") Color.Red else accent,
                    size = Dp.Unspecified
                )
            }
            
            if (settings.showConfidence) {
                Text(
                    text = "${person.name} [${(person.confidence * 100).toInt()}%]",
                    color = if (person.name == "UNKNOWN") Color.Red else accent,
                    fontSize = (10 * scale).sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                )
            }
        }
    }
}


@Composable
fun FourRoiContainer(
    persons: List<DetectedPerson>,
    scale: Float,
    settings: AppearanceSettings,
    cameraSize: Dp
) {
    val maxRoi = 4
    // Show people other than the primary one in the 4 corners
    val activePersons = persons.drop(1).take(maxRoi)
    if (activePersons.isEmpty()) return

    val accent = Color(settings.accentColor)
    val offsetDistance = (cameraSize / 2) + (60 * scale).dp

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        activePersons.forEachIndexed { index, person ->
            val offsetX = when (index) {
                0 -> -offsetDistance // Left
                1 -> offsetDistance  // Top Right
                2 -> offsetDistance  // Bottom Right
                3 -> -offsetDistance // Bottom Left
                else -> 0.dp
            }
            val offsetY = when (index) {
                0 -> 0.dp            // Left (Center)
                1 -> -offsetDistance // Top Right
                2 -> offsetDistance  // Bottom Right
                3 -> offsetDistance  // Bottom Left
                else -> 0.dp
            }

            Box(
                modifier = Modifier
                    .offset(x = offsetX, y = offsetY)
                    .animateContentSize()
            ) {
                SecondaryRoiBlip(
                    person = person,
                    scale = scale,
                    accent = accent,
                    showConfidence = settings.showConfidence
                )
            }
        }
    }
}

@Composable
fun SidebarBlips(
    persons: List<DetectedPerson>,
    scale: Float,
    settings: AppearanceSettings
) {
    Column(
        verticalArrangement = Arrangement.spacedBy((12 * scale).dp),
        modifier = Modifier
            .width((72 * scale).dp)
            .padding(vertical = (8 * scale).dp),
        horizontalAlignment = Alignment.Start
    ) {
        persons.take(settings.maxRoi).forEach { person ->
            SecondaryRoiBlip(
                person = person,
                scale = scale,
                accent = Color(settings.accentColor),
                showConfidence = settings.showConfidence
            )
        }
    }
}

@Composable
fun GestureIndicator(gesture: GestureEvent?, scale: Float, accent: Color) {
    if (gesture == null || gesture.type == GestureType.NONE) return
    
    val infiniteTransition = rememberInfiniteTransition(label = "gesture")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(400), RepeatMode.Reverse),
        label = "alpha"
    )

    Surface(
        color = Color.Black.copy(alpha = 0.7f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
        modifier = Modifier.padding(16.dp).graphicsLayer(alpha = alpha)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when(gesture.type) {
                    GestureType.POINT -> Icons.Default.Place
                    GestureType.SWIPE_LEFT -> Icons.AutoMirrored.Filled.ArrowBack
                    GestureType.SWIPE_RIGHT -> Icons.AutoMirrored.Filled.ArrowForward
                    GestureType.SWIPE_UP -> Icons.Default.ArrowUpward
                    GestureType.SWIPE_DOWN -> Icons.Default.ArrowDownward
                    else -> Icons.Default.PanTool
                },
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size((18 * scale).dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = gesture.type.name.replace("_", " "),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun AlertBanner(message: String, scale: Float, onClick: () -> Unit = {}) {
    val isStable = message.contains("STABLE", ignoreCase = true)
    val color = if (isStable) SuccessGreen else ErrorRed
    
    // Pill-style alert matching attached image
    Surface(
        modifier = Modifier
            .padding(top = 10.dp)
            .clickable { onClick() },
        color = Color.Black.copy(alpha = 0.8f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.5.dp, color.copy(alpha = 0.6f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Icon(
                if (isStable) Icons.Default.CheckCircle else Icons.Default.Warning, 
                null, 
                tint = color, 
                modifier = Modifier.size((18 * scale).dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                message.uppercase(), 
                color = Color.White, 
                fontSize = (11 * scale).sp, 
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun SensorStatus(battery: Int, noise: Int, scale: Float) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(4.dp)
    ) {
        StatusTag("BAT", "$battery%", if (battery < 20) ErrorRed else SuccessGreen, scale)
        StatusTag("NSE", "${noise}dB", if (noise > 75) ErrorRed else AccentCyan, scale)
    }
}

@Composable
private fun StatusTag(label: String, value: String, color: Color, scale: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextSecondary, fontSize = (8 * scale).sp, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.width(6.dp))
        Text(value, color = color, fontSize = (10 * scale).sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun LiveContextSummary(scale: Float, onClick: () -> Unit = {}) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        shape = RoundedCornerShape(CornerRadiusMedium),
        border = BorderStroke(BorderWidthThin, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier
            .widthIn(max = (180 * scale).dp)
            .clickable { onClick() }
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(
                "Active Context", 
                color = AccentCyan, 
                fontSize = (10 * scale).sp, 
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Home • Normal Activity", 
                color = MaterialTheme.colorScheme.onSurface, 
                fontSize = (11 * scale).sp,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
fun RecordingIndicator(scale: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "recording")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "alpha"
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size((8 * scale).dp).graphicsLayer(alpha = alpha).background(Color.Red, CircleShape))
        Spacer(Modifier.width((6 * scale).dp))
        Text("REC", color = Color.Red, fontSize = (10 * scale).sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun WorkspaceDebugTag(
    name: String, 
    intent: String, 
    focus: String? = null, 
    decisions: List<Decision> = emptyList(),
    perception: PerceptionState? = null
) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.TopEnd) {
        Column(horizontalAlignment = Alignment.End) {
            DebugTag("WORKSPACE: ${name.uppercase()}")
            Spacer(Modifier.height(4.dp))
            DebugTag("INTENT: ${intent.uppercase()}", color = Color.Yellow)
            focus?.let {
                Spacer(Modifier.height(4.dp))
                DebugTag("COGNITIVE FOCUS: $it", color = Color.Magenta)
            }
            if (decisions.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                DebugTag("ACTIVE DECISIONS: ${decisions.size}", color = SuccessGreen)
            }
            perception?.let {
                Spacer(Modifier.height(4.dp))
                DebugTag("SENSOR SYNC: ${if (it.visual.primaryROI != null) "V+" else "V-"}${if (it.audio.voiceActive) "A+" else "A-"}", color = Color.LightGray)
            }
        }
    }
}

@Composable
fun DebugTag(text: String, color: Color = MaterialTheme.colorScheme.primary) {
    Text(
        text,
        color = color,
        fontSize = 8.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
            .border(1.dp, color, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
fun HUDComponentWrapper(
    widget: WidgetBlueprint,
    debugEnabled: Boolean,
    content: @Composable () -> Unit
) {
    val alignment = when (widget.anchor) {
        WidgetAnchor.TOP_START -> Alignment.TopStart
        WidgetAnchor.TOP_CENTER -> Alignment.TopCenter
        WidgetAnchor.TOP_END -> Alignment.TopEnd
        WidgetAnchor.CENTER_START -> Alignment.CenterStart
        WidgetAnchor.CENTER -> Alignment.Center
        WidgetAnchor.CENTER_END -> Alignment.CenterEnd
        WidgetAnchor.BOTTOM_START -> Alignment.BottomStart
        WidgetAnchor.BOTTOM_CENTER -> Alignment.BottomCenter
        WidgetAnchor.BOTTOM_END -> Alignment.BottomEnd
        else -> Alignment.Center
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .align(alignment)
                .offset(x = widget.offsetX, y = widget.offsetY)
                .graphicsLayer {
                    alpha = widget.alpha
                    scaleX = widget.scale
                    scaleY = widget.scale
                }
        ) {
            content()
            
            if (debugEnabled) {
                DebugOverlay(widget)
            }
        }
    }
}

@Composable
fun DebugOverlay(widget: WidgetBlueprint) {
    Column(
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
            .border(1.dp, AccentCyan, RoundedCornerShape(4.dp))
            .padding(4.dp)
    ) {
        DebugText("ID: ${widget.id}")
        DebugText("PRIO: ${widget.priority}")
        DebugText("STATE: ${widget.state}")
        DebugText("ZONE: ${widget.allowedZones.firstOrNull() ?: "NONE"}")
        DebugText("SIZE: ${widget.width}x${widget.height}")
    }
}

@Composable
fun DebugText(text: String) {
    Text(
        text = text,
        color = AccentCyan,
        fontSize = 7.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold
    )
}
