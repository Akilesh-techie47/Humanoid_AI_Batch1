package com.humanoidai.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    drawerState: DrawerState,
    unreadCount: Int = 0,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(280.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 24.dp, top = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "Humanoid Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "HUMANOID AI",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 1.sp
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), modifier = Modifier.padding(bottom = 16.dp))

                    // PRIMARY
                    DrawerSectionHeader("PRIMARY")
                    NavItem(Icons.Default.Dashboard, "Dashboard", NavRoutes.DASHBOARD, navController, drawerState)
                    NavItem(Icons.Default.CameraAlt, "Camera Home", NavRoutes.ENVIRONMENT, navController, drawerState)
                    NavItem(Icons.Default.Notifications, "Alerts", NavRoutes.ALERTS, navController, drawerState, badge = unreadCount)
                    NavItem(Icons.Default.AutoAwesome, "AI Assistant", NavRoutes.ASSISTANT, navController, drawerState)
                    NavItem(Icons.Default.Forum, "Communication Intel", NavRoutes.COMMUNICATION_ACCESS, navController, drawerState)
                    NavItem(Icons.Default.QuestionAnswer, "What Did I Miss?", NavRoutes.COMMUNICATION_BRIEFING, navController, drawerState)
                    NavItem(Icons.Default.Face, "Face Recognition", NavRoutes.RECOGNITION, navController, drawerState)
                    NavItem(Icons.Default.PersonAdd, "Face Enrollment", NavRoutes.ENROLLMENT, navController, drawerState)
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // MONITORING
                    DrawerSectionHeader("MONITORING")
                    NavItem(Icons.Default.History, "Activity History", NavRoutes.HISTORY, navController, drawerState)
                    NavItem(Icons.Default.Analytics, "Security Analytics", NavRoutes.ANALYTICS, navController, drawerState)

                    Spacer(Modifier.height(16.dp))

                    // DIAGNOSTICS
                    DrawerSectionHeader("DIAGNOSTICS")
                    NavItem(Icons.Default.Troubleshoot, "Runtime Inspector", NavRoutes.RUNTIME_INSPECTOR, navController, drawerState)
                    NavItem(Icons.Default.PrivacyTip, "Privacy Gateway", NavRoutes.PRIVACY_DASHBOARD, navController, drawerState)
                    NavItem(Icons.Default.TrackChanges, "AI Goal Plan", NavRoutes.GOAL_INSPECTOR, navController, drawerState)

                    Spacer(Modifier.height(16.dp))

                    // SETTINGS
                    DrawerSectionHeader("SYSTEM")
                    NavItem(Icons.Default.Settings, "Settings", NavRoutes.SETTINGS, navController, drawerState)

                    Spacer(Modifier.height(32.dp))
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
        title, 
        fontSize = 10.sp, 
        fontWeight = FontWeight.Bold,
        color = TextMuted, 
        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
        letterSpacing = 1.sp
    )
}

@Composable
private fun NavItem(
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
        label = { Text(label, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
        selected = isSelected,
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            scope.launch { drawerState.close() }
            if (!isSelected) {
                navController.navigate(route) {
                    popUpTo(NavRoutes.ENVIRONMENT) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        },
        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
        badge = {
            if (badge > 0) {
                Badge(containerColor = Color.Red) {
                    Text(badge.toString(), color = Color.White)
                }
            }
        },
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = Color.Transparent,
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            selectedIconColor = MaterialTheme.colorScheme.primary,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            selectedTextColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier.padding(vertical = 2.dp)
    )
}
