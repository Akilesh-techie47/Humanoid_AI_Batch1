package com.humanoidai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.humanoidai.ui.theme.*

// -----------------------------------------------------------------
// Data model
// -----------------------------------------------------------------
enum class AlertSeverity { HIGH, MEDIUM, LOW }

data class AlertItem(
    val id: Int,
    val title: String,
    val description: String,
    val time: String,
    val severity: AlertSeverity,
    val isRead: Boolean = false
)

// -----------------------------------------------------------------
// AlertsScreen
// -----------------------------------------------------------------
@Composable
fun AlertsScreen(navController: NavController) {

    // Placeholder alerts — will be replaced by real ML detections in Phase 2+
    val alerts = remember {
        listOf(
            AlertItem(1, "Unknown Person Detected", "Unrecognized face detected near entry zone", "2 min ago", AlertSeverity.HIGH),
            AlertItem(2, "Unusual Activity", "Prolonged stationary presence detected", "15 min ago", AlertSeverity.MEDIUM),
            AlertItem(3, "Camera Obstructed", "ROI visibility dropped below threshold", "1 hr ago", AlertSeverity.HIGH),
            AlertItem(4, "Known Person Arrived", "Recognized: Person A entered the frame", "2 hr ago", AlertSeverity.LOW),
            AlertItem(5, "System Ready", "All sensors initialized successfully", "3 hr ago", AlertSeverity.LOW, isRead = true),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(horizontal = 16.dp)
    ) {
        // Header
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Alerts", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(
                "${alerts.count { !it.isRead }} new",
                fontSize = 13.sp,
                color = AccentCyan
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (alerts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No alerts", color = TextSecondary)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(alerts) { alert ->
                    AlertCard(alert)
                }
            }
        }
    }
}

@Composable
private fun AlertCard(alert: AlertItem) {
    val (borderColor, iconTint, icon) = when (alert.severity) {
        AlertSeverity.HIGH   -> Triple(Color(0xFFFF5C5C), Color(0xFFFF5C5C), Icons.Default.Warning)
        AlertSeverity.MEDIUM -> Triple(Color(0xFFFFA726), Color(0xFFFFA726), Icons.Default.Notifications)
        AlertSeverity.LOW    -> Triple(AccentCyan, AccentCyan, Icons.Default.Info)
    }

    val cardAlpha = if (alert.isRead) 0.5f else 1f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark, RoundedCornerShape(12.dp))
            .border(1.dp, borderColor.copy(alpha = cardAlpha * 0.6f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint.copy(alpha = cardAlpha),
            modifier = Modifier.size(28.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                alert.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary.copy(alpha = cardAlpha)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                alert.description,
                fontSize = 12.sp,
                color = TextSecondary.copy(alpha = cardAlpha)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(alert.time, fontSize = 11.sp, color = TextSecondary.copy(alpha = cardAlpha))
    }
}
