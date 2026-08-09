package com.humanoidai.ui.layoutcustomization

import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.humanoidai.ui.layoutcustomization.domain.blueprint.WidgetAnchor
import com.humanoidai.ui.layoutcustomization.domain.blueprint.WidgetBlueprint
import com.humanoidai.ui.layoutcustomization.domain.hud.HUDComponentRegistry
import com.humanoidai.ui.layoutcustomization.domain.hud.HUDComponentState
import com.humanoidai.ui.layoutcustomization.domain.reasoning.Decision
import com.humanoidai.ui.layoutcustomization.domain.roi.ROIDecision
import com.humanoidai.ui.layoutcustomization.domain.roi.ROIState
import com.humanoidai.ui.layouts.HomeLayoutPreset
import com.humanoidai.ui.theme.AccentCyan
import com.humanoidai.ui.theme.SuccessGreen
import com.humanoidai.ui.theme.TextSecondary
import com.humanoidai.ui.screens.CameraAperture
import com.humanoidai.ui.screens.SecondaryRoiBlip
import com.humanoidai.vision.DetectedPerson
import kotlin.math.roundToInt
import com.humanoidai.ui.customization.AppearanceSettings
import com.humanoidai.hearing.AudioDirectionState
import com.humanoidai.ui.components.AudioDirectionRadar
import com.humanoidai.ui.widgets.HudCrosshair
import com.humanoidai.ui.widgets.GlassPanel
import com.humanoidai.ui.screens.HudTopBar
import com.humanoidai.ui.screens.SystemControlBar
import com.humanoidai.navigation.NavRoutes

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
    messages: List<com.humanoidai.ai.ChatMessage>,
    battery: Int,
    noise: Int,
    isListening: Boolean,
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
    perceptionState: com.humanoidai.ui.layoutcustomization.domain.perception.PerceptionState? = null,
    previewView: (PreviewView) -> Unit,
    layoutPreset: HomeLayoutPreset,
    scale: Float, // UI Global Scale
    debugEnabled: Boolean = false,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Draw standard components
        components.filter { it.isVisible }.forEach { widget ->
            HUDComponentWrapper(
                widget = widget,
                debugEnabled = debugEnabled
            ) {
                when (widget.id) {
                    HUDComponentRegistry.CAMERA -> {
                        CameraAperture(
                            size = widget.width,
                            preset = layoutPreset,
                            preview = previewView,
                            scale = scale * widget.scale,
                            accent = Color(settings.accentColor),
                            glowEnabled = settings.glowEnabled
                        )
                    }
                    HUDComponentRegistry.STATUS_BAR -> {
                        if (settings.showAiStatus) {
                            HudTopBar(
                                settings = settings,
                                status = status,
                                onMenu = onMenu,
                                onSwitchCamera = onSwitchCamera,
                                onSettings = { onNavigate(NavRoutes.SETTINGS) }
                            )
                        }
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
                            AudioDirectionRadar(state = audioState)
                        }
                    }
                    HUDComponentRegistry.ALERTS -> {
                        if (settings.showAlertBanner) {
                            AlertBanner(message = latestAlert ?: "SYSTEM STABLE", scale = scale)
                        }
                    }
                    HUDComponentRegistry.FPS_COUNTER -> {
                        if (settings.showFpsCounter) {
                            DebugTag("FPS: 60", color = Color.Green)
                        }
                    }
                    HUDComponentRegistry.RECORDING_INDICATOR -> {
                        if (isListening) {
                            RecordingIndicator(scale = scale)
                        }
                    }
                    HUDComponentRegistry.STATUS_INDICATORS -> {
                        if (settings.showSensorStatus) {
                            SensorStatus(battery = battery, noise = noise, scale = scale)
                        }
                    }
                    HUDComponentRegistry.NOTIFICATIONS -> {
                        if (settings.showLiveContext) {
                            LiveContextSummary(scale = scale)
                        }
                    }
                }
            }
        }

        // Secondary ROIs - Vertical Sidebar (Polished with Glass)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = (16 * scale).dp, top = (100 * scale).dp, bottom = (100 * scale).dp), // Added top/bottom padding to avoid overlap
            contentAlignment = Alignment.CenterStart
        ) {
            GlassPanel(
                settings = settings,
                modifier = Modifier
                    .wrapContentWidth()
                    .fillMaxHeight(0.6f) // Limit height to middle section
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy((12 * scale).dp),
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = (4 * scale).dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    detectedPersons.take(settings.maxRoi).forEach { person ->
                        SecondaryRoiBlip(
                            person = person,
                            scale = scale,
                            accent = Color(settings.accentColor),
                            showConfidence = settings.showConfidence
                        )
                    }
                }
            }
        }
        
        if (debugEnabled) {
            WorkspaceDebugTag(workspaceName, intentName, focusId, activeDecisions, perceptionState)
        }
    }
}

@Composable
fun AlertBanner(message: String, scale: Float) {
    GlassPanel(
        settings = AppearanceSettings(), // Fallback or pass settings
        modifier = Modifier.width((240 * scale).dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, null, tint = Color.Red, modifier = Modifier.size((16 * scale).dp))
            Spacer(Modifier.width((8 * scale).dp))
            Text(message, color = Color.White, fontSize = (10 * scale).sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SensorStatus(battery: Int, noise: Int, scale: Float) {
    Column(horizontalAlignment = Alignment.End) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("BAT", color = TextSecondary, fontSize = (8 * scale).sp)
            Spacer(Modifier.width(4.dp))
            Text("$battery%", color = Color.White, fontSize = (10 * scale).sp, fontWeight = FontWeight.Bold)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("NSE", color = TextSecondary, fontSize = (8 * scale).sp)
            Spacer(Modifier.width(4.dp))
            Text("${noise}dB", color = Color.White, fontSize = (10 * scale).sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LiveContextSummary(scale: Float) {
    GlassPanel(
        settings = AppearanceSettings(),
        modifier = Modifier.width((180 * scale).dp)
    ) {
        Column {
            Text("ENVIRONMENT SUMMARY", color = AccentCyan, fontSize = (8 * scale).sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Stable / Home / Daytime", color = Color.White.copy(alpha = 0.7f), fontSize = (9 * scale).sp)
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
    perception: com.humanoidai.ui.layoutcustomization.domain.perception.PerceptionState? = null
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
fun DebugTag(text: String, color: Color = AccentCyan) {
    Text(
        text,
        color = color,
        fontSize = 8.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
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
        // For camera-relative anchors in Phase 2B, we default to Center for now
        // since the Engine/Resolver should have translated these to absolute anchors
        // or offsets before rendering.
        else -> Alignment.Center
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp) // Base margin
    ) {
        Box(
            modifier = Modifier
                .align(alignment)
                .offset(x = widget.offsetX, y = widget.offsetY)
                .graphicsLayer(alpha = widget.alpha)
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
        
        if (widget.id == HUDComponentRegistry.PRIMARY_ROI) {
            DebugText("ROI_AUTO: ACTIVE")
        }

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
