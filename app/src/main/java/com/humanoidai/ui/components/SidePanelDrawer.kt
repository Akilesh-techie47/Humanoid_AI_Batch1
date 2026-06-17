package com.humanoidai.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
                drawerContainerColor = Color(0xFF0F0F1A),
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
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "HUMANOID AI",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(bottom = 16.dp))

                    // Menu Items
                    NavItem(Icons.Default.CameraAlt, "Camera Home", NavRoutes.ENVIRONMENT, navController, drawerState)
                    NavItem(Icons.Default.Notifications, "Alerts", NavRoutes.ALERTS, navController, drawerState, badge = unreadCount)
                    NavItem(Icons.Default.AutoAwesome, "AI Assistant", NavRoutes.ASSISTANT, navController, drawerState)
                    NavItem(Icons.Default.Face, "Face Recognition", NavRoutes.RECOGNITION, navController, drawerState)
                    NavItem(Icons.Default.PersonAdd, "Face Enrollment", NavRoutes.ENROLLMENT, navController, drawerState)
                    
                    Spacer(Modifier.height(16.dp))
                    Text("HISTORY & ANALYTICS", fontSize = 11.sp, color = TextSecondary, modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
                    
                    NavItem(Icons.Default.History, "Activity History", NavRoutes.HISTORY, navController, drawerState)
                    NavItem(Icons.Default.BarChart, "Security Analytics", NavRoutes.ANALYTICS, navController, drawerState)
                    
                    Spacer(Modifier.weight(1f))
                    
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 16.dp))
                    
                    NavItem(Icons.Default.Settings, "Settings", NavRoutes.SETTINGS, navController, drawerState)
                }
            }
        }
    ) {
        content()
    }
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
    
    NavigationDrawerItem(
        label = { Text(label, fontSize = 14.sp) },
        selected = isSelected,
        onClick = {
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
            selectedContainerColor = AccentCyan.copy(alpha = 0.15f),
            unselectedIconColor = TextSecondary,
            selectedIconColor = AccentCyan,
            unselectedTextColor = TextSecondary,
            selectedTextColor = Color.White
        ),
        modifier = Modifier.padding(vertical = 2.dp)
    )
}
