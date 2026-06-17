package com.humanoidai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.google.firebase.auth.FirebaseAuth
import com.humanoidai.ui.theme.*

// -----------------------------------------------------------------
// SettingsScreen
// -----------------------------------------------------------------
@Composable
fun SettingsScreen(navController: NavController) {

    // Toggleable settings state
    var notificationsEnabled by remember { mutableStateOf(true) }
    var roiOverlayEnabled by remember { mutableStateOf(true) }
    var unknownAlertEnabled by remember { mutableStateOf(true) }
    var heatmapEnabled by remember { mutableStateOf(false) }
    var proactiveAssistEnabled by remember { mutableStateOf(true) }

    val currentUser = FirebaseAuth.getInstance().currentUser

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text("Settings", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

        // ---- Account ----
        Spacer(modifier = Modifier.height(20.dp))
        SectionHeader("Account")
        InfoRow(Icons.Default.Person, "Signed in as", currentUser?.email ?: currentUser?.phoneNumber ?: "Unknown")
        Spacer(modifier = Modifier.height(8.dp))
        ActionRow(Icons.Default.ExitToApp, "Sign Out", Color(0xFFFF5C5C)) {
            FirebaseAuth.getInstance().signOut()
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        }

        // ---- Detection ----
        Spacer(modifier = Modifier.height(20.dp))
        SectionHeader("Detection")
        ToggleRow(Icons.Default.Visibility, "ROI Overlay", roiOverlayEnabled) { roiOverlayEnabled = it }
        ToggleRow(Icons.Default.Warning, "Unknown Person Alerts", unknownAlertEnabled) { unknownAlertEnabled = it }
        ToggleRow(Icons.Default.Thermostat, "Heatmap Overlay", heatmapEnabled) { heatmapEnabled = it }

        // ---- Assistant ----
        Spacer(modifier = Modifier.height(20.dp))
        SectionHeader("Assistant")
        ToggleRow(Icons.Default.Notifications, "Notifications", notificationsEnabled) { notificationsEnabled = it }
        ToggleRow(Icons.Default.AutoAwesome, "Proactive Assistance", proactiveAssistEnabled) { proactiveAssistEnabled = it }

        // ---- About ----
        Spacer(modifier = Modifier.height(20.dp))
        SectionHeader("About")
        InfoRow(Icons.Default.Info, "App Version", "1.0.0 (Phase 1)")
        InfoRow(Icons.Default.Build, "Build", "Debug")

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// -----------------------------------------------------------------
// Section header
// -----------------------------------------------------------------
@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = AccentCyan,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

// -----------------------------------------------------------------
// Toggle row
// -----------------------------------------------------------------
@Composable
private fun ToggleRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .padding(bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = AccentCyan,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = SurfaceDark
            )
        )
    }
    Spacer(modifier = Modifier.height(6.dp))
}

// -----------------------------------------------------------------
// Info row (read-only)
// -----------------------------------------------------------------
@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, color = TextSecondary)
    }
    Spacer(modifier = Modifier.height(6.dp))
}

// -----------------------------------------------------------------
// Action row (tappable)
// -----------------------------------------------------------------
@Composable
private fun ActionRow(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, fontSize = 14.sp, color = color, fontWeight = FontWeight.SemiBold)
    }
}
