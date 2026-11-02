package com.humanoidai.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.humanoidai.navigation.NavRoutes
import com.humanoidai.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SidePanelDrawer(
    navController: NavController,
    drawerState: DrawerState = rememberDrawerState(initialValue = DrawerValue.Closed),
    isOpen: Boolean = false,
    onDrawerClose: () -> Unit = {},
    unreadCount: Int = 0,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(isOpen) {
        if (isOpen) {
            drawerState.open()
        } else {
            drawerState.close()
        }
    }

    LaunchedEffect(drawerState.isClosed) {
        if (drawerState.isClosed && isOpen) {
            onDrawerClose()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                modifier = Modifier
                    .width(310.dp)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Modern App Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = "Aura Logo",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Aura 360",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Intelligent Companion",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // "+ New Chat" Action Button (ChatGPT / Claude style)
                    Surface(
                        shape = RoundedCornerShape(CornerRadiusMedium),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(CornerRadiusMedium))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch { drawerState.close() }
                                navController.navigate(NavRoutes.ASSISTANT) {
                                    popUpTo(NavRoutes.DASHBOARD) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AddComment,
                                contentDescription = "New Chat",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "New Chat",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(Modifier.height(8.dp))

                    // Scrollable navigation destinations
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Section: AI & Assistant
                        DrawerSectionHeader("AI ASSISTANT")
                        ModernNavItem(Icons.Outlined.AutoAwesome, "AI Assistant", NavRoutes.ASSISTANT, navController, drawerState)
                        ModernNavItem(Icons.Outlined.QuestionAnswer, "What Did I Miss?", NavRoutes.COMMUNICATION_BRIEFING, navController, drawerState)
                        ModernNavItem(Icons.Outlined.Forum, "Communication Intel", NavRoutes.COMMUNICATION_ACCESS, navController, drawerState)

                        Spacer(Modifier.height(12.dp))

                        // Section: Vision & Perception
                        DrawerSectionHeader("VISION & SENSORS")
                        ModernNavItem(Icons.Outlined.CameraAlt, "Camera Vision Home", NavRoutes.ENVIRONMENT, navController, drawerState)
                        ModernNavItem(Icons.Outlined.Face, "Face Recognition", NavRoutes.RECOGNITION, navController, drawerState)
                        ModernNavItem(Icons.Outlined.PersonAdd, "Face Enrollment", NavRoutes.ENROLLMENT, navController, drawerState)

                        Spacer(Modifier.height(12.dp))

                        // Section: System & Ecosystem
                        DrawerSectionHeader("ECOSYSTEM & OVERVIEW")
                        ModernNavItem(Icons.Outlined.Dashboard, "Dashboard", NavRoutes.DASHBOARD, navController, drawerState)
                        ModernNavItem(Icons.Outlined.Apps, "Aura Apps Hub", NavRoutes.APPS_HUB, navController, drawerState)
                        ModernNavItem(Icons.Outlined.Notifications, "Alerts", NavRoutes.ALERTS, navController, drawerState, badge = unreadCount)
                        ModernNavItem(Icons.Outlined.History, "Activity History", NavRoutes.HISTORY, navController, drawerState)
                        ModernNavItem(Icons.Outlined.Analytics, "Security Analytics", NavRoutes.ANALYTICS, navController, drawerState)

                        Spacer(Modifier.height(12.dp))

                        // Section: Diagnostics
                        DrawerSectionHeader("DIAGNOSTICS")
                        ModernNavItem(Icons.Outlined.Troubleshoot, "Runtime Inspector", NavRoutes.RUNTIME_INSPECTOR, navController, drawerState)
                        ModernNavItem(Icons.Outlined.PrivacyTip, "Privacy Gateway", NavRoutes.PRIVACY_DASHBOARD, navController, drawerState)
                        ModernNavItem(Icons.Outlined.TrackChanges, "AI Goal Plan", NavRoutes.GOAL_INSPECTOR, navController, drawerState)

                        Spacer(Modifier.height(16.dp))
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(Modifier.height(8.dp))

                    // Bottom Settings destination
                    ModernNavItem(
                        icon = Icons.Outlined.Settings,
                        label = "Settings",
                        route = NavRoutes.SETTINGS,
                        navController = navController,
                        drawerState = drawerState
                    )
                }
            }
        }
    ) {
        content()
    }
}

@Composable
private fun DrawerSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun ModernNavItem(
    icon: ImageVector,
    label: String,
    route: String,
    navController: NavController,
    drawerState: DrawerState,
    badge: Int = 0
) {
    val isSelected = navController.currentDestination?.route == route
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    NavigationDrawerItem(
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        },
        selected = isSelected,
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            scope.launch { drawerState.close() }
            if (!isSelected) {
                navController.navigate(route) {
                    popUpTo(NavRoutes.DASHBOARD) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        },
        badge = {
            if (badge > 0) {
                Badge(
                    containerColor = ErrorRed,
                    contentColor = Color.White
                ) {
                    Text(
                        text = badge.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = Color.Transparent,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
            unselectedTextColor = MaterialTheme.colorScheme.onSurface,
            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        shape = RoundedCornerShape(CornerRadiusMedium),
        modifier = Modifier.padding(vertical = 2.dp)
    )
}
