package com.humanoidai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Notifications
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
enum class EventType { DETECTION, ALERT, SYSTEM }

data class HistoryEvent(
    val id: Int,
    val title: String,
    val detail: String,
    val timestamp: String,
    val date: String,
    val type: EventType
)

// -----------------------------------------------------------------
// HistoryScreen
// -----------------------------------------------------------------
@Composable
fun HistoryScreen(navController: NavController) {

    // Grouped by date — real events stored/retrieved from DB in Phase 3+
    val events = remember {
        listOf(
            HistoryEvent(1, "Person A detected", "Confidence: 97%", "09:14 AM", "Today", EventType.DETECTION),
            HistoryEvent(2, "Unknown person alert", "Alert triggered at zone 1", "09:02 AM", "Today", EventType.ALERT),
            HistoryEvent(3, "System started", "All sensors online", "08:55 AM", "Today", EventType.SYSTEM),
            HistoryEvent(4, "Person B detected", "Confidence: 89%", "06:30 PM", "Yesterday", EventType.DETECTION),
            HistoryEvent(5, "Camera obstructed", "ROI blocked for 3s", "04:15 PM", "Yesterday", EventType.ALERT),
            HistoryEvent(6, "Person C detected", "Confidence: 93%", "11:00 AM", "Yesterday", EventType.DETECTION),
        )
    }

    val grouped = events.groupBy { it.date }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text("History", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            grouped.forEach { (date, dayEvents) ->
                item {
                    DateHeader(date)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(dayEvents) { event ->
                    HistoryEventRow(event)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun DateHeader(date: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.DateRange, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(date, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AccentCyan)
    }
}

@Composable
private fun HistoryEventRow(event: HistoryEvent) {
    val (icon, iconColor) = when (event.type) {
        EventType.DETECTION -> Pair(Icons.Default.Person, AccentCyan)
        EventType.ALERT     -> Pair(Icons.Default.Notifications, Color(0xFFFF5C5C))
        EventType.SYSTEM    -> Pair(Icons.Default.DateRange, TextSecondary)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(event.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(event.detail, fontSize = 11.sp, color = TextSecondary)
        }
        Text(event.timestamp, fontSize = 11.sp, color = TextSecondary)
    }
}
