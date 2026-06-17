package com.humanoidai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
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
import com.humanoidai.ui.theme.*

// -----------------------------------------------------------------
// AnalyticsScreen
// Placeholder charts — real data from sensor/ML pipeline in Phase 4+
// -----------------------------------------------------------------
@Composable
fun AnalyticsScreen(navController: NavController) {

    // Placeholder weekly detection data (Mon–Sun)
    val weeklyDetections = listOf(3, 7, 5, 9, 4, 6, 2)
    val days = listOf("M", "T", "W", "T", "F", "S", "S")
    val maxVal = weeklyDetections.max().toFloat()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text("Analytics", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

        // ---- Summary cards ----
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryCard("36", "Detections", Icons.Default.Person, AccentCyan, Modifier.weight(1f))
            SummaryCard("8", "Alerts", Icons.Default.Notifications, Color(0xFFFF5C5C), Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryCard("28", "Known", Icons.Default.CheckCircle, Color(0xFF66BB6A), Modifier.weight(1f))
            SummaryCard("8", "Unknown", Icons.Default.Warning, Color(0xFFFFA726), Modifier.weight(1f))
        }

        // ---- Bar chart ----
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

        // ---- Detection breakdown ----
        Spacer(modifier = Modifier.height(24.dp))
        Text("Detection Breakdown", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(modifier = Modifier.height(12.dp))

        BreakdownRow("Known Persons", 28, 36, AccentCyan)
        Spacer(modifier = Modifier.height(8.dp))
        BreakdownRow("Unknown Persons", 8, 36, Color(0xFFFF5C5C))

        // ---- Alert breakdown ----
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

// -----------------------------------------------------------------
// Summary card
// -----------------------------------------------------------------
@Composable
private fun SummaryCard(
    value: String,
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(SurfaceDark, RoundedCornerShape(12.dp))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 12.sp, color = TextSecondary)
    }
}

// -----------------------------------------------------------------
// Progress bar breakdown row
// -----------------------------------------------------------------
@Composable
private fun BreakdownRow(label: String, value: Int, total: Int, color: Color) {
    val fraction = if (total > 0) value.toFloat() / total else 0f
    val percent = (fraction * 100).toInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 13.sp, color = TextPrimary)
            Text("$value ($percent%)", fontSize = 13.sp, color = color, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(BackgroundDark)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}
