package com.humanoidai.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.humanoidai.memory.LongTermMemory
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
    var selectedFilter by remember { mutableStateOf("All") }

    LaunchedEffect(Unit) {
        val interactions = memory.getRecentInteractions(60)
        val alerts = memory.getRecentAlerts(40)

        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
        val today = dateFormat.format(Date())

        val interactionEvents = interactions.map {
            HistoryEvent(
                id = it.interactionId.hashCode(),
                title = it.eventType.replace("_", " ").replaceFirstChar { c -> c.uppercase() },
                detail = it.aiResponse.take(80) + if (it.aiResponse.length > 80) "..." else "",
                timestamp = timeFormat.format(Date(it.timestamp)),
                date = if (dateFormat.format(Date(it.timestamp)) == today) "Today" else dateFormat.format(Date(it.timestamp)),
                type = EventType.DETECTION,
                rawTimestamp = it.timestamp
            )
        }

        val alertEvents = alerts.map {
            HistoryEvent(
                id = it.alertId.hashCode(),
                title = it.alertType.replace("_", " ").replaceFirstChar { c -> c.uppercase() },
                detail = it.contactName.ifBlank { "Unknown person detected" },
                timestamp = timeFormat.format(Date(it.triggeredAt)),
                date = if (dateFormat.format(Date(it.triggeredAt)) == today) "Today" else dateFormat.format(Date(it.triggeredAt)),
                type = EventType.ALERT,
                rawTimestamp = it.triggeredAt
            )
        }

        historyItems = (interactionEvents + alertEvents).sortedByDescending { it.rawTimestamp }
    }

    val filteredItems = remember(historyItems, selectedFilter) {
        when (selectedFilter) {
            "Detections" -> historyItems.filter { it.type == EventType.DETECTION }
            "Alerts" -> historyItems.filter { it.type == EventType.ALERT }
            "System" -> historyItems.filter { it.type == EventType.SYSTEM }
            else -> historyItems
        }
    }

    val grouped = filteredItems.groupBy { it.date }
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
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Activity History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch { drawerState.open() }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Filter chips row
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf("All", "Detections", "Alerts")
                    items(filters) { filter ->
                        AuraChip(
                            text = filter,
                            isSelected = selectedFilter == filter,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedFilter = filter
                            }
                        )
                    }
                }

                if (filteredItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "No activity recorded yet",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        grouped.forEach { (date, dayEvents) ->
                            item {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CalendarToday,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = date,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            items(dayEvents, key = { it.id }) { event ->
                                ModernHistoryEventRow(event)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernHistoryEventRow(event: HistoryEvent) {
    val (icon, iconColor) = when (event.type) {
        EventType.DETECTION -> Pair(Icons.Outlined.Person, AccentCyan)
        EventType.ALERT     -> Pair(Icons.Outlined.NotificationsActive, ErrorRed)
        EventType.SYSTEM    -> Pair(Icons.Outlined.Info, InfoBlue)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CornerRadiusMedium),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (event.detail.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = event.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = event.timestamp,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
