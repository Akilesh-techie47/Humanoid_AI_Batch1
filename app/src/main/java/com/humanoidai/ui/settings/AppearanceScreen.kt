package com.humanoidai.ui.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.humanoidai.ai.OllamaClient
import kotlinx.coroutines.launch
import com.humanoidai.ui.customization.AppearanceViewModel
import com.humanoidai.ui.customization.UIScale
import com.humanoidai.ui.customization.AIMode
import com.humanoidai.ui.theme.*
import com.humanoidai.navigation.NavRoutes
import com.humanoidai.ui.customization.AppearanceSettings

import com.humanoidai.ui.components.ArmsunFooter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(navController: NavController, viewModel: AppearanceViewModel) {
    val settings by viewModel.settings.collectAsState()
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "APPEARANCE", 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold, 
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                SectionHeader("System Theme")
                AuraPanel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (settings.isDarkMode) "Dark Mode" else "Light Mode",
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = settings.isDarkMode,
                            onCheckedChange = { viewModel.setDarkMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.primary,
                                uncheckedTrackColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
                SectionHeader("Theme Engine")
                
                ActionRow(Icons.Default.Dashboard, "Layout Customization") {
                    navController.navigate(NavRoutes.LAYOUT_CUSTOMIZATION)
                }
                
                Spacer(Modifier.height(24.dp))

                SectionHeader("Visual Calibration")
                
                ScaleSelector(settings.uiScale) { viewModel.setScale(it) }
                
                Spacer(Modifier.height(16.dp))
                
                ThemeGrid(selectedId = settings.themeId) { viewModel.setHudTheme(it) }

                Spacer(Modifier.height(24.dp))
                
                SectionHeader("Core Surface")
                
                ToggleRow("Global Glow FX", settings.glowEnabled) { viewModel.toggleComponent("glow", it) }
                ToggleRow("Scan Animation", settings.cameraScanAnim) { viewModel.toggleComponent("scan", it) }
                ToggleRow("Audio Radar", settings.showRadar) { viewModel.toggleComponent("radar", it) }
                ToggleRow("Background Grid", settings.showGrid) { viewModel.toggleComponent("grid", it) }
                
                Spacer(Modifier.height(80.dp))
            }
            
            ArmsunFooter(modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun ActionRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    AuraPanel(modifier = Modifier.clickable { onClick() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    AuraPanel(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
            Switch(
                checked = checked, 
                onCheckedChange = onChecked, 
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedTrackColor = MaterialTheme.colorScheme.background
                )
            )
        }
    }
}

@Composable
private fun ThemeGrid(selectedId: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HudThemes.all.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { theme ->
                    ThemeCard(
                        theme = theme,
                        active = theme.id == selectedId,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(theme.id) }
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ThemeCard(theme: HudTheme, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) theme.accent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
            .border(1.dp, if (active) theme.accent else MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column {
            Text(theme.name, style = MaterialTheme.typography.labelLarge, color = if (active) theme.accent else MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(theme.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun ScaleSelector(current: UIScale, onSelect: (UIScale) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        UIScale.entries.forEach { scale ->
            val active = scale == current
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface)
                    .border(1.dp, if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .clickable { onSelect(scale) },
                contentAlignment = Alignment.Center
            ) {
                Text(scale.name, style = MaterialTheme.typography.labelMedium, color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
            }
        }
    }
}
