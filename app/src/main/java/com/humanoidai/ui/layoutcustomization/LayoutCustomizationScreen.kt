package com.humanoidai.ui.layoutcustomization

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.humanoidai.ui.customization.HUDStructure
import com.humanoidai.ui.layoutcustomization.presentation.event.LayoutCustomizationEvent
import com.humanoidai.ui.layoutcustomization.presentation.viewmodel.LayoutCustomizationViewModel
import com.humanoidai.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayoutCustomizationScreen(
    navController: NavController,
    viewModel: LayoutCustomizationViewModel
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "LAYOUT CUSTOMIZATION",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentCyan,
                        fontFamily = FontFamily.Monospace
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Background Dot Grid (simulated with radial gradient)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(AccentCyan.copy(alpha = 0.05f), Color.Transparent),
                            radius = 1500f
                        )
                    )
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
            ) {
                item {
                    Text(
                        "SYSTEM GEOMETRY ENGINE",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                item {
                    CustomizationSection(
                        title = "ARCHITECTURE",
                        icon = Icons.Default.Dashboard
                    ) {
                        Column {
                            SettingLabel("Structural Preset")
                            SelectionGrid(
                                options = HUDStructure.entries.map { it.name.replace("_", " ") },
                                selectedOption = state.selectedLayout.replace("_", " "),
                                onOptionSelected = { 
                                    viewModel.onEvent(LayoutCustomizationEvent.ChangeSelectedLayout(it.replace(" ", "_")))
                                }
                            )

                            InfoText("Changes the core spatial arrangement of all HUD components.")
                        }
                    }
                }

                item {
                    CustomizationSection(
                        title = "CAMERA",
                        icon = Icons.Default.Videocam
                    ) {

                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column {
                                SettingLabel("Camera Scale")
                                SelectionGrid(
                                    options = listOf("Compact", "Standard", "Large", "Immersive"),
                                    selectedOption = when {
                                        state.camera.scale <= 0.85f -> "Compact"
                                        state.camera.scale >= 1.3f -> "Immersive"
                                        state.camera.scale >= 1.15f -> "Large"
                                        else -> "Standard"
                                    },
                                    onOptionSelected = {
                                        val scale = when (it) {
                                            "Compact" -> 0.85f
                                            "Large" -> 1.15f
                                            "Immersive" -> 1.3f
                                            else -> 1.0f
                                        }
                                        viewModel.onEvent(LayoutCustomizationEvent.ChangeCameraScale(scale))
                                    }
                                )
                            }

                            Column {
                                SettingLabel("Camera Offset")
                                SelectionGrid(
                                    options = listOf("Slightly Up", "Center", "Slightly Down"),
                                    selectedOption = when {
                                        state.camera.offsetY <= -12f -> "Slightly Up"
                                        state.camera.offsetY >= 12f -> "Slightly Down"
                                        else -> "Center"
                                    },
                                    onOptionSelected = {
                                        val offset = when (it) {
                                            "Slightly Up" -> -24f
                                            "Slightly Down" -> 24f
                                            else -> 0f
                                        }
                                        viewModel.onEvent(LayoutCustomizationEvent.ChangeCameraOffset(0f, offset))
                                    }
                                )
                                InfoText("Horizontal movement is not allowed to maintain facial symmetry.")
                            }
                        }
                    }
                }

                item {
                    CustomizationSection(
                        title = "ROI",
                        icon = Icons.Default.FilterCenterFocus
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column {
                                SettingLabel("Primary ROI Position")
                                ROIPositionSelector(
                                    selectedPosition = if (state.roi.primaryAutoPosition) "Auto" else getROIPosString(state.roi.primaryPositionX, state.roi.primaryPositionY),
                                    onPositionSelected = { pos ->
                                        if (pos == "Auto") {
                                            viewModel.onEvent(LayoutCustomizationEvent.ChangePrimaryROIPosition(0f, 0f, true))
                                        } else {
                                            val (x, y) = mapPosToCoords(pos)
                                            viewModel.onEvent(LayoutCustomizationEvent.ChangePrimaryROIPosition(x, y, false))
                                        }
                                    }
                                )
                            }

                            Column {
                                SettingLabel("Secondary ROI Position")
                                ROIPositionSelector(
                                    selectedPosition = if (state.roi.secondaryAutoPosition) "Auto" else getROIPosString(state.roi.secondaryPositionX, state.roi.secondaryPositionY),
                                    onPositionSelected = { pos ->
                                        if (pos == "Auto") {
                                            viewModel.onEvent(LayoutCustomizationEvent.ChangeSecondaryROIPosition(0f, 0f, true))
                                        } else {
                                            val (x, y) = mapPosToCoords(pos)
                                            viewModel.onEvent(LayoutCustomizationEvent.ChangeSecondaryROIPosition(x, y, false))
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    CustomizationSection(
                        title = "WIDGETS",
                        icon = Icons.Default.Widgets
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            Column {
                                SettingLabel("Widget Density")
                                SelectionGrid(
                                    options = listOf("Minimal", "Compact", "Balanced", "Expanded", "Command Center"),
                                    selectedOption = state.widget.density,
                                    onOptionSelected = { viewModel.onEvent(LayoutCustomizationEvent.ChangeWidgetDensity(it)) }
                                )
                            }

                            Column {
                                SettingLabel("Widget Arrangement")
                                SelectionGrid(
                                    options = listOf("Compact", "Balanced", "Spacious"),
                                    selectedOption = state.widget.arrangement,
                                    onOptionSelected = { viewModel.onEvent(LayoutCustomizationEvent.ChangeWidgetArrangement(it)) }
                                )
                            }

                            Column {
                                SettingLabel("Widget Visibility")
                                val widgets = listOf(
                                    "Primary ROI", "Secondary ROI", "AI Assistant", "AI Status",
                                    "Notification Stack", "Alert Panel", "Object Details",
                                    "Emotion Indicator", "Distance Indicator"
                                )
                                widgets.forEach { widget ->
                                    VisibilityToggle(
                                        label = widget,
                                        checked = state.widget.visibilityMap[widget] ?: true,
                                        onCheckedChange = { viewModel.onEvent(LayoutCustomizationEvent.ToggleWidgetVisibility(widget, it)) }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    CustomizationSection(
                        title = "MOTION",
                        icon = Icons.Default.Animation
                    ) {
                        Column {
                            SettingLabel("Motion Profile")
                            SelectionGrid(
                                options = listOf("Instant", "Minimal", "Smooth", "Cinematic", "Professional"),
                                selectedOption = state.motion.profile.capitalize(Locale.ROOT),
                                onOptionSelected = { viewModel.onEvent(LayoutCustomizationEvent.ChangeMotionProfile(it.lowercase(Locale.ROOT))) }
                            )
                        }
                    }
                }

                item {
                    CustomizationSection(
                        title = "PROFILES",
                        icon = Icons.Default.Save
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy((12 * 1.0f).dp)) {
                            ActionButton(
                                "Capture Layout Snapshot", 
                                Icons.Default.Save,
                                onClick = {
                                    // Visual feedback
                                    viewModel.onEvent(LayoutCustomizationEvent.ChangeSelectedLayout(state.selectedLayout))
                                }
                            )
                            ActionButton(
                                "Load Stored Profile", 
                                Icons.Default.FileUpload,
                                onClick = {
                                    // For now just re-triggers state
                                    viewModel.onEvent(LayoutCustomizationEvent.ChangeSelectedLayout(state.selectedLayout))
                                }
                            )
                            ActionButton(
                                "Reset Layout",
                                Icons.Default.Refresh,
                                color = ErrorRed,
                                onClick = { viewModel.onEvent(LayoutCustomizationEvent.RestoreDefaults) }
                            )
                            InfoText("State is automatically persisted to secure storage.")
                        }
                    }
                }

            }
        }
    }
}

@Composable
fun CustomizationSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "rotation")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = SurfaceDark.copy(alpha = 0.4f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(AccentCyan.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, "Section Icon", tint = AccentCyan, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f),
                    letterSpacing = 0.5.sp
                )
                Icon(
                    Icons.Default.ExpandMore,
                    "Expand/Collapse",
                    tint = TextSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.rotate(rotation).size(20.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
                ) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(bottom = 16.dp))
                    content()
                }
            }
        }
    }
}

@Composable
fun SelectionGrid(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { option ->
                    val isSelected = option.equals(selectedOption, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) AccentCyan.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.25f))
                            .border(
                                1.dp,
                                if (isSelected) AccentCyan else Color.White.copy(alpha = 0.08f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { 
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onOptionSelected(option) 
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            option.uppercase(),
                            color = if (isSelected) AccentCyan else TextSecondary.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}


@Composable
fun SettingLabel(text: String) {
    Text(
        text.uppercase(),
        color = AccentCyan.copy(alpha = 0.7f),
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun VisibilityToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f),
            letterSpacing = 0.2.sp
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = AccentCyan,
                uncheckedThumbColor = TextSecondary.copy(alpha = 0.6f),
                uncheckedTrackColor = SurfaceDark.copy(alpha = 0.6f)
            ),
            modifier = Modifier.scale(0.75f)
        )
    }
}


@Composable
fun ActionButton(
    text: String,
    icon: ImageVector,
    color: Color = AccentCyan,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (enabled) color.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f))
            .border(1.dp, if (enabled) color.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, text, tint = if (enabled) color else TextSecondary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            text.uppercase(),
            color = if (enabled) Color.White else TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun InfoText(text: String) {
    Text(
        text,
        color = TextSecondary.copy(alpha = 0.6f),
        fontSize = 9.sp,
        lineHeight = 12.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
fun ROIPositionSelector(
    selectedPosition: String,
    onPositionSelected: (String) -> Unit
) {
    val positions = listOf(
        "Auto",
        "Top Left", "Top Center", "Top Right",
        "Left Center", "Right Center",
        "Bottom Left", "Bottom Center", "Bottom Right"
    )
    
    SelectionGrid(
        options = positions,
        selectedOption = selectedPosition,
        onOptionSelected = onPositionSelected
    )
}

private fun getROIPosString(x: Float, y: Float): String {
    return when {
        x < 0.3f && y < 0.3f -> "Top Left"
        x > 0.7f && y < 0.3f -> "Top Right"
        y < 0.3f -> "Top Center"
        x < 0.3f && y > 0.7f -> "Bottom Left"
        x > 0.7f && y > 0.7f -> "Bottom Right"
        y > 0.7f -> "Bottom Center"
        x < 0.3f -> "Left Center"
        x > 0.7f -> "Right Center"
        else -> "Center"
    }
}

private fun mapPosToCoords(pos: String): Pair<Float, Float> {
    return when (pos) {
        "Top Left" -> 0.1f to 0.1f
        "Top Center" -> 0.5f to 0.1f
        "Top Right" -> 0.9f to 0.1f
        "Left Center" -> 0.1f to 0.5f
        "Right Center" -> 0.9f to 0.5f
        "Bottom Left" -> 0.1f to 0.9f
        "Bottom Center" -> 0.5f to 0.9f
        "Bottom Right" -> 0.9f to 0.9f
        else -> 0.5f to 0.5f
    }
}

private fun String.capitalize(locale: Locale): String =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
