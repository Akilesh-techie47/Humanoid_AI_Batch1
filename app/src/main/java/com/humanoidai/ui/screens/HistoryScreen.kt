package com.humanoidai.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import com.humanoidai.memory.LongTermMemory
import com.humanoidai.ui.components.ArmsunFooter
import com.humanoidai.ui.components.SidePanelDrawer
import com.humanoidai.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class EventType { DETECTION, ALERT, SYSTEM }

data class HistoryEvent(
    val id: Int,
    val title: String,
    val detail: String,
    val timestamp: String,
    val date: String,
    val type: EventType,
    val rawTimestamp: Long = 0L
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavController) {
    val context = LocalContext.current
    val memory = remember { LongTermMemory.getInstance(context) }
    var historyItems by remember { mutableStateOf<List<HistoryEvent>>(emptyList()) }

    LaunchedEffect(Unit) {
        val interactions = memory.getRecentInteractions(60)
        val alerts = memory.getRecentAlerts(40)
        
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
        val today = dateFormat.format(Date())
        
        val interactionEvents = interactions.map {
            HistoryEvent(
                id = it.interactionId.hashCode(),
                title = it.eventType.replace("_", " "),
                detail = it.aiResponse.take(60) + if(it.aiResponse.length > 60) "..." else "",
                timestamp = timeFormat.format(Date(it.timestamp)),
                date = if(dateFormat.format(Date(it.timestamp)) == today) "Today" else dateFormat.format(Date(it.timestamp)),
                type = EventType.DETECTION,
                rawTimestamp = it.timestamp
            )
        }
        
        val alertEvents = alerts.map {
            HistoryEvent(
                id = it.alertId.hashCode(),
                title = it.alertType.replace("_", " "),
                detail = it.contactName.ifBlank { "Unknown person" },
                timestamp = timeFormat.format(Date(it.triggeredAt)),
                date = if(dateFormat.format(Date(it.triggeredAt)) == today) "Today" else dateFormat.format(Date(it.triggeredAt)),
                type = EventType.ALERT,
                rawTimestamp = it.triggeredAt
            )
        }
        
        historyItems = (interactionEvents + alertEvents).sortedByDescending { it.rawTimestamp }
    }

    val grouped = historyItems.groupBy { it.date }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    SidePanelDrawer(
        navController = navController,
        drawerState = drawerState
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
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
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Menu, "Menu", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                }
                
                Spacer(Modifier.width(16.dp))
                
                Text(
                    "ACTIVITY HISTORY", 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, 
                    color = MaterialTheme.colorScheme.primary, 
                    letterSpacing = 1.sp
                )
            }
        }
    ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    grouped.forEach { (date, dayEvents) ->
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            DateHeader(date)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        items(dayEvents) { event ->
                            HistoryEventRow(event)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    item { Spacer(modifier = Modifier.height(64.dp)) }
                }
                
                ArmsunFooter(modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

@Composable
private fun DateHeader(date: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.DateRange, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(date, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun HistoryEventRow(event: HistoryEvent) {
    val (icon, iconColor) = when (event.type) {
        EventType.DETECTION -> Pair(Icons.Default.Person, MaterialTheme.colorScheme.primary)
        EventType.ALERT     -> Pair(Icons.Default.Notifications, ErrorRed)
        EventType.SYSTEM    -> Pair(Icons.Default.Settings, SuccessGreen)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(event.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(2.dp))
                Text(event.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            
            Text(event.timestamp, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        }
    }
}
