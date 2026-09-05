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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.humanoidai.memory.LongTermMemory
import com.humanoidai.memory.repository.AnalyticsSummary
import com.humanoidai.ui.components.SidePanelDrawer
import com.humanoidai.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(navController: NavController) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    
    val memory = remember { LongTermMemory.getInstance(context) }
    var summary by remember { mutableStateOf<AnalyticsSummary?>(null) }

    LaunchedEffect(Unit) {
        summary = memory.getAnalyticsSummary()
    }

    val weeklyDetections = listOf(3, 7, 5, 9, 4, 6, 2) // Still hardcoded weekly, but today is real
    val days = listOf("M", "T", "W", "T", "F", "S", "S")
    val maxVal = weeklyDetections.max().toFloat()

    SidePanelDrawer(
        navController = navController,
        drawerState = drawerState
    ) {
        Scaffold(
            containerColor = BackgroundDark,
            topBar = {
                Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch { drawerState.open() } 
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark.copy(alpha = 0.4f))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape)
                ) {
                    Icon(Icons.Default.Menu, "Menu", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                
                Spacer(Modifier.width(16.dp))
                
                Text("SECURITY ANALYTICS", fontSize = 15.sp, fontWeight = FontWeight.Black, color = AccentCyan, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
            }
        }
    ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryCard("${summary?.totalDetectionsToday ?: 0}", "Today's Detections", Icons.Default.Person, AccentCyan, Modifier.weight(1f))
                    SummaryCard("${summary?.totalAlertsToday ?: 0}", "Today's Alerts", Icons.Default.Notifications, Color(0xFFFF5C5C), Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryCard("${summary?.registeredUserCount ?: 0}", "Total Enrolled", Icons.Default.CheckCircle, Color(0xFF66BB6A), Modifier.weight(1f))
                    SummaryCard("Active", "System State", Icons.Default.SmartToy, Color(0xFFFFA726), Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("Weekly Detections", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        weeklyDetections.forEachIndexed { index, value ->
                            val barHeightFraction = value / maxVal
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                Text(value.toString(), fontSize = 10.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(28.dp)
                                        .fillMaxHeight(barHeightFraction)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(AccentCyan.copy(alpha = 0.8f))
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(days[index], fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("Detection Breakdown", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                BreakdownRow("Known Persons", 28, 36, AccentCyan)
                Spacer(modifier = Modifier.height(8.dp))
                BreakdownRow("Unknown Persons", 8, 36, Color(0xFFFF5C5C))

                Spacer(modifier = Modifier.height(24.dp))
                Text("Alert Breakdown", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                BreakdownRow("High Severity", 3, 8, Color(0xFFFF5C5C))
                Spacer(modifier = Modifier.height(8.dp))
                BreakdownRow("Medium Severity", 2, 8, Color(0xFFFFA726))
                Spacer(modifier = Modifier.height(8.dp))
                BreakdownRow("Low Severity", 3, 8, Color(0xFF66BB6A))

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SummaryCard(
    value: String,
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceDark.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text(label.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun BreakdownRow(label: String, value: Int, total: Int, color: Color) {
    val fraction = if (total > 0) value.toFloat() / total else 0f
    val percent = (fraction * 100).toInt()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = SurfaceDark.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.02f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.8f))
                Text("$value ($percent%)", fontSize = 12.sp, color = color, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(BackgroundDark)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
}
