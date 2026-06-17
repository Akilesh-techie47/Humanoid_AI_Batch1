package com.humanoidai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.humanoidai.alerts.AlertEngine
import com.humanoidai.alerts.AlertItem
import com.humanoidai.alerts.AlertPriority
import com.humanoidai.ui.components.SidePanelDrawer
import com.humanoidai.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    navController: NavController,
    alertEngine: AlertEngine
) {
    val alerts by alertEngine.alerts.collectAsState()
    val unreadCount by alertEngine.unreadCount.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var selectedFilter by remember { mutableStateOf<AlertPriority?>(null) }

    val filteredAlerts = if (selectedFilter != null) {
        alerts.filter { it.priority == selectedFilter }
    } else {
        alerts
    }

    SidePanelDrawer(
        navController = navController,
        drawerState = drawerState,
        unreadCount = unreadCount
    ) {
        Scaffold(
            containerColor = BackgroundDark,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text("ALERTS CENTER", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccentCyan)
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
            ) {
                // ---- Summary Header ----
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        if (unreadCount > 0) {
                            Text(
                                "$unreadCount unread notifications",
                                fontSize = 12.sp,
                                color = Color(0xFFFF5C5C)
                            )
                        } else {
                            Text("All alerts reviewed", fontSize = 12.sp, color = AlertGreen)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (unreadCount > 0) {
                            TextButton(onClick = { alertEngine.markAllRead() }) {
                                Text("Mark all read", color = AccentCyan, fontSize = 12.sp)
                            }
                        }
                        IconButton(onClick = { alertEngine.clearAll() }) {
                            Icon(Icons.Default.DeleteSweep, null, tint = TextSecondary)
                        }
                    }
                }

                // ---- Priority Filter Chips ----
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        label = { Text("All (${alerts.size})", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentCyan.copy(alpha = 0.2f),
                            selectedLabelColor = AccentCyan
                        )
                    )
                    FilterChip(
                        selected = selectedFilter == AlertPriority.CRITICAL,
                        onClick = { selectedFilter = if (selectedFilter == AlertPriority.CRITICAL) null else AlertPriority.CRITICAL },
                        label = { Text("Critical", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFF3B30).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFFFF3B30)
                        )
                    )
                    FilterChip(
                        selected = selectedFilter == AlertPriority.HIGH,
                        onClick = { selectedFilter = if (selectedFilter == AlertPriority.HIGH) null else AlertPriority.HIGH },
                        label = { Text("High", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFF5C5C).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFFFF5C5C)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ---- Alert List ----
                if (filteredAlerts.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.NotificationsNone,
                                null,
                                tint = TextSecondary.copy(alpha = 0.4f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                if (selectedFilter != null) "No ${selectedFilter?.name?.lowercase()} alerts"
                                else "No alerts yet",
                                fontSize = 15.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Alerts appear when unknown persons\nare detected by the camera",
                                fontSize = 13.sp,
                                color = TextSecondary.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        items(filteredAlerts, key = { it.id }) { alert ->
                            AlertCard(
                                alert = alert,
                                formattedTime = alertEngine.getFormattedTime(alert.timestamp),
                                onTap = { alertEngine.markRead(alert.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertCard(
    alert: AlertItem,
    formattedTime: String,
    onTap: () -> Unit
) {
    val (borderColor, iconColor, bgColor, icon) = when (alert.priority) {
        AlertPriority.CRITICAL -> Quad(
            Color(0xFFFF3B30), Color(0xFFFF3B30),
            Color(0xFF1A0505), Icons.Default.Warning
        )
        AlertPriority.HIGH -> Quad(
            Color(0xFFFF5C5C), Color(0xFFFF5C5C),
            Color(0xFF140808), Icons.Default.PersonOff
        )
        AlertPriority.MEDIUM -> Quad(
            AccentCyan, AccentCyan,
            Color(0xFF050F0F), Icons.Default.Person
        )
        AlertPriority.LOW -> Quad(
            TextSecondary, TextSecondary,
            SurfaceDark, Icons.Default.Info
        )
    }

    val alpha = if (alert.isRead) 0.5f else 1f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (!alert.isRead) bgColor else SurfaceDark,
                RoundedCornerShape(12.dp)
            )
            .border(
                width = if (!alert.isRead) 1.dp else 0.5.dp,
                color = borderColor.copy(alpha = if (!alert.isRead) 0.6f else 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onTap)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            if (alert.faceBitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = alert.faceBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                Icon(
                    icon,
                    null,
                    tint = iconColor.copy(alpha = alpha),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    alert.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary.copy(alpha = alpha)
                )
                Text(
                    formattedTime,
                    fontSize = 10.sp,
                    color = TextSecondary.copy(alpha = alpha)
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                alert.description,
                fontSize = 12.sp,
                color = TextSecondary.copy(alpha = alpha),
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            PriorityBadge(alert.priority, alpha)
        }

        if (!alert.isRead) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(borderColor, CircleShape)
            )
        }
    }
}

@Composable
private fun PriorityBadge(priority: AlertPriority, alpha: Float = 1f) {
    val (color, label) = when (priority) {
        AlertPriority.CRITICAL -> Pair(Color(0xFFFF3B30), "CRITICAL")
        AlertPriority.HIGH     -> Pair(Color(0xFFFF5C5C), "HIGH")
        AlertPriority.MEDIUM   -> Pair(AccentCyan, "MEDIUM")
        AlertPriority.LOW      -> Pair(TextSecondary, "LOW")
    }
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f * alpha), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, fontSize = 9.sp, color = color.copy(alpha = alpha), fontWeight = FontWeight.Bold)
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
