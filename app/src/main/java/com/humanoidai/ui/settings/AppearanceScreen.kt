package com.humanoidai.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.humanoidai.ui.customization.AppearanceViewModel
import com.humanoidai.ui.layouts.HomeLayoutPreset
import com.humanoidai.ui.theme.*
import com.humanoidai.ui.widgets.HexagonShape

import com.humanoidai.ui.customization.HUDStructure

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    navController: NavController,
    viewModel: AppearanceViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = AppearanceViewModel.Factory(LocalContext.current)
    )
) {
    val settings by viewModel.settings.collectAsState()
    val haptic = LocalHapticFeedback.current
    
    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("APPEARANCE", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccentCyan) },
                navigationIcon = { 
                    IconButton(onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        navController.popBackStack() 
                    }) { 
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) 
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
            SectionHeader("Camera HUD Theme")
            Text("10 visual personalities — exactly as the reference images. Tap to apply instantly.", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(bottom = 16.dp))

            HudThemes.all.chunked(2).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { theme ->
                        HudThemeCard(
                            theme = theme,
                            selected = settings.themeId == theme.id,
                            onClick = { viewModel.setHudTheme(theme.id) }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(24.dp))
            SectionHeader("Technical Structure")
            StructureSelector(settings.hudStructure) { viewModel.setStructure(it) }

            Spacer(Modifier.height(24.dp))
            SectionHeader("Personalization")
            ActionRow(Icons.Default.Dashboard, "Layout Customization", AccentCyan) {
                navController.navigate(com.humanoidai.navigation.NavRoutes.LAYOUT_CUSTOMIZATION)
            }

            Spacer(Modifier.height(24.dp))
            SectionHeader("System Toggles")
            SettingsToggle("Directional Radar", settings.showRadar) { viewModel.toggleComponent("radar", it) }
            SettingsToggle("Technical Dot Grid", settings.showGrid) { viewModel.toggleComponent("grid", it) }
            
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun PresetCard(preset: HomeLayoutPreset, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(120.dp)
            .width(160.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) preset.primaryColor.copy(alpha = 0.1f) else SurfaceDark)
            .border(if (selected) 2.dp else 1.dp, if (selected) preset.primaryColor else Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column {
            // Visual Preview logic
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(when(preset.roiShape) {
                        "hexagon" -> com.humanoidai.ui.components.HexagonShape()
                        "square" -> RoundedCornerShape(8.dp)
                        else -> CircleShape
                    })
                    .background(preset.primaryColor.copy(alpha = 0.2f))
                    .border(1.dp, preset.primaryColor, when(preset.roiShape) {
                        "hexagon" -> com.humanoidai.ui.components.HexagonShape()
                        "square" -> RoundedCornerShape(8.dp)
                        else -> CircleShape
                    }),
                contentAlignment = Alignment.Center
            ) {
                Text("ROI", color = preset.primaryColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(Modifier.weight(1f))
            
            Text(preset.name, color = if (selected) Color.White else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(preset.roiShape.uppercase(), color = preset.primaryColor.copy(alpha = 0.7f), fontSize = 8.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun HudThemeCard(theme: HudTheme, selected: Boolean, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val shape = theme.roiShape.asComposeShape()
    Box(
        modifier = Modifier
            .height(118.dp)
            .width(160.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) theme.accent.copy(alpha = 0.14f) else SurfaceDark)
            .border(if (selected) 2.dp else 1.dp, if (selected) theme.accent else Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .clickable { 
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick() 
            }
            .padding(12.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(shape)
                    .background(theme.accent.copy(alpha = 0.18f))
                    .border(1.4.dp, theme.accent, shape),
                contentAlignment = Alignment.Center
            ) {
                Text("ROI", color = theme.accent, fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            }
            Spacer(Modifier.weight(1f))
            Text(theme.name, color = if (selected) Color.White else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(theme.subtitle, color = theme.accent.copy(alpha = 0.75f), fontSize = 7.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
        if (selected) {
            Box(
                modifier = Modifier.align(Alignment.TopEnd).size(8.dp).clip(CircleShape).background(theme.accent)
            )
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(title, color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp, start = 4.dp))
}

@Composable
fun SettingsToggle(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(SurfaceDark, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChecked, colors = SwitchDefaults.colors(checkedThumbColor = AccentCyan, checkedTrackColor = AccentCyan.copy(alpha = 0.3f)))
    }
}

@Composable
private fun ActionRow(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(SurfaceDark, RoundedCornerShape(12.dp))
            .clickable { 
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick() 
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, label, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = TextSecondary)
    }
}

@Composable
fun StructureSelector(current: HUDStructure, onSelected: (HUDStructure) -> Unit) {
    val haptic = LocalHapticFeedback.current
    val options = HUDStructure.entries
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.toList().chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { structure ->
                    val active = current == structure
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (active) AccentCyan.copy(alpha = 0.2f) else SurfaceDark)
                            .border(1.dp, if (active) AccentCyan else Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .clickable { 
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelected(structure) 
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            structure.name.replace("_", " "),
                            color = if (active) AccentCyan else Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
