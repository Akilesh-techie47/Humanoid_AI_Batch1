package com.humanoidai.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.humanoidai.navigation.NavRoutes

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

val bottomNavItems = listOf(
    BottomNavItem("Home",    Icons.Default.Home,         NavRoutes.DASHBOARD),
    BottomNavItem("Camera",  Icons.Default.Videocam,     NavRoutes.ENVIRONMENT),
    BottomNavItem("Alerts",  Icons.Default.Notifications,NavRoutes.ALERTS),
    BottomNavItem("AI",      Icons.Default.SmartToy,     NavRoutes.ASSISTANT),
    BottomNavItem("Settings",Icons.Default.Settings,     NavRoutes.SETTINGS),
)

@Composable
fun BottomNavBar(navController: NavController) {
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route

    NavigationBar(containerColor = androidx.compose.ui.graphics.Color(0xFF141824)) {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(NavRoutes.DASHBOARD) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}