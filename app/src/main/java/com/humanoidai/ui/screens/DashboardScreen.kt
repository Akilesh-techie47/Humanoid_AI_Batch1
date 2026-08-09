package com.humanoidai.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.humanoidai.ui.components.SidePanelDrawer
import com.humanoidai.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    SidePanelDrawer(
        navController = navController,
        drawerState = drawerState
    ) {
        Scaffold(
            containerColor = BackgroundDark,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text("DASHBOARD", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccentCyan)
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
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Welcome Back", fontSize = 24.sp,
                            fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("System Active", fontSize = 13.sp, color = AlertGreen)
                    }
                    Icon(Icons.Default.AccountCircle, contentDescription = null,
                        tint = AccentCyan, modifier = Modifier.size(40.dp))
                }

                Spacer(Modifier.height(20.dp))

                // Status Cards Row
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatusCard("Users", "2", Icons.Default.People, AlertGreen, Modifier.weight(1f))
                    StatusCard("Alerts", "3", Icons.Default.Warning, AlertOrange, Modifier.weight(1f))
                    StatusCard("AI", "ON", Icons.Default.SmartToy, PrimaryBlue, Modifier.weight(1f))
                }

                Spacer(Modifier.height(20.dp))

                // Quick Actions
                Text("Quick Actions", fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickActionCard("Camera", Icons.Default.Videocam,
                        Modifier.weight(1f)) { navController.navigate(NavRoutes.ENVIRONMENT) }
                    QuickActionCard("Recognize", Icons.Default.Face,
                        Modifier.weight(1f)) { navController.navigate(NavRoutes.RECOGNITION) }
                    QuickActionCard("Analytics", Icons.Default.Analytics,
                        Modifier.weight(1f)) { navController.navigate(NavRoutes.ANALYTICS) }
                }

                Spacer(Modifier.height(20.dp))

                // Recent Events
                Text("Recent Events", fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(10.dp))

                listOf<Triple<String, String, Color>>(
                    Triple("User Detected", "2 min ago", AlertGreen),
                    Triple("Meeting Reminder", "15 min ago", AlertOrange),
                    Triple("Motion Alert", "1 hr ago", AlertRed),
                ).forEach { event ->
                    val (title, time, color) = event
                    EventRow(title, time, color)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun StatusCard(label: String, value: String, icon: ImageVector,
               color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 11.sp, color = TextSecondary)
        }
    }
}

@Composable
fun QuickActionCard(label: String, icon: ImageVector,
                    modifier: Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, fontSize = 12.sp, color = TextPrimary)
        }
    }
}

@Composable
fun EventRow(title: String, time: String, dotColor: Color) {
    Card(shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark)) {
        Row(Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(dotColor, shape = RoundedCornerShape(5.dp)))
            Spacer(Modifier.width(12.dp))
            Text(title, color = TextPrimary, modifier = Modifier.weight(1f))
            Text(time, fontSize = 12.sp, color = TextSecondary)
        }
    }
}