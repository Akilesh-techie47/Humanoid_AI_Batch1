package com.humanoidai.ui.screens

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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.humanoidai.memory.LongTermMemory
import com.humanoidai.memory.repository.AnalyticsSummary
import com.humanoidai.memory.entities.InteractionEntity
import com.humanoidai.memory.entities.AlertHistoryEntity
import com.humanoidai.navigation.NavRoutes
import com.humanoidai.ui.components.SidePanelDrawer
import com.humanoidai.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    val memory = remember { LongTermMemory.getInstance(context) }
    var summary by remember { mutableStateOf<AnalyticsSummary?>(null) }
    var recentInteractions by remember { mutableStateOf<List<InteractionEntity>>(emptyList()) }

    LaunchedEffect(Unit) {
        summary = memory.getAnalyticsSummary()
        recentInteractions = memory.getRecentInteractions(10)
    }

    SidePanelDrawer(
        navController = navController,
        drawerState = drawerState
    ) {
        Scaffold(
            containerColor = BackgroundDark,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text("DASHBOARD", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccentCyan)
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, "Menu", tint = Color.White)
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
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Welcome Back", fontSize = 24.sp,
                            fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("System Analysis Active", fontSize = 13.sp, color = SuccessGreen, fontWeight = FontWeight.Medium)
                    }
                    Surface(
                        color = AccentCyan.copy(alpha = 0.1f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.3f))
                    ) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null,
                            tint = AccentCyan, modifier = Modifier.size(44.dp).padding(4.dp))
                    }
                }

                Spacer(Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatusCard("Detections", "${summary?.totalDetectionsToday ?: 0}", Icons.Default.People, SuccessGreen, Modifier.weight(1f))
                    StatusCard("Alerts", "${summary?.totalAlertsToday ?: 0}", Icons.Default.Warning, WarningOrange, Modifier.weight(1f))
                    StatusCard("Registry", "${summary?.registeredUserCount ?: 0}", Icons.Default.SmartToy, AccentCyan, Modifier.weight(1f))
                }

                Spacer(Modifier.height(20.dp))

                // Quick Actions
                Text("Quick Actions", fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickActionCard("Vision HUD", Icons.Default.Videocam,
                        Modifier.weight(1f)) { navController.navigate(NavRoutes.ENVIRONMENT) }
                    QuickActionCard("Identity", Icons.Default.Face,
                        Modifier.weight(1f)) { navController.navigate(NavRoutes.RECOGNITION) }
                    QuickActionCard("History", Icons.Default.History,
                        Modifier.weight(1f)) { navController.navigate(NavRoutes.HISTORY) }
                }

                Spacer(Modifier.height(20.dp))

                // Recent Events
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timeline, null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Interaction History", fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
                Spacer(Modifier.height(10.dp))

                if (recentInteractions.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No recent interactions logged.", color = TextSecondary, fontSize = 13.sp)
                    }
                } else {
                    recentInteractions.take(5).forEach { interaction ->
                        EventRow(
                            interaction.eventType.replace("_", " "),
                            formatTime(interaction.timestamp),
                            if (interaction.wasOwnerPresent) SuccessGreen else WarningOrange
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        else -> "${diff / 86_400_000}d ago"
    }
}

@Composable
fun StatusCard(label: String, value: String, icon: ImageVector,
               color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = SurfaceDark.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text(label.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 1.sp)
        }
    }
}

@Composable
fun QuickActionCard(label: String, icon: ImageVector,
                    modifier: Modifier, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Surface(
        modifier = modifier.clickable { 
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick() 
        },
        shape = RoundedCornerShape(12.dp),
        color = SurfaceDark.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AccentCyan.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.White)
        }
    }
}

@Composable
fun EventRow(title: String, time: String, dotColor: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceDark.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.02f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(dotColor, shape = CircleShape)
                    .graphicsLayer {
                        // Subtle glow
                        shadowElevation = 4f
                    }
            )
            Spacer(Modifier.width(16.dp))
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text(time, fontSize = 11.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
        }
    }
}
