package com.humanoidai.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.humanoidai.permission.PermissionManager
import com.humanoidai.ui.theme.SuccessGreen
import androidx.compose.foundation.shape.CircleShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunicationAccessScreen(navController: NavController) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var hasNotifAccess by remember { mutableStateOf(PermissionManager.isNotificationServiceEnabled(context)) }
    var hasContactsAccess by remember { mutableStateOf(PermissionManager.hasReadContactsPermission(context)) }
    var hasCallLogAccess by remember { mutableStateOf(PermissionManager.hasReadCallLogPermission(context)) }
    var hasSmsAccess by remember { mutableStateOf(PermissionManager.hasReadSmsPermission(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { map ->
        hasContactsAccess = map[PermissionManager.READ_CONTACTS_PERMISSION] ?: hasContactsAccess
        hasCallLogAccess = map[PermissionManager.READ_CALL_LOG_PERMISSION] ?: hasCallLogAccess
        hasSmsAccess = map[PermissionManager.READ_SMS_PERMISSION] ?: hasSmsAccess
    }

    // Refresh notif access when returning to screen
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotifAccess = PermissionManager.isNotificationServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
                        navController.popBackStack() 
                    },
                    modifier = Modifier
                        .size((44).dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f), CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                }
                
                Spacer(Modifier.width(16.dp))
                
                Text("SYSTEM ACCESS", fontSize = 15.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Enable access to unlock Communication Intelligence. Aura 360° will be able to brief you on missed calls and messages.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            AccessRow(
                title = "Notifications",
                subtitle = "Understand messages and important alerts",
                enabled = hasNotifAccess,
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
            )

            AccessRow(
                title = "Contacts",
                subtitle = "Identify people and priority contacts",
                enabled = hasContactsAccess,
                onClick = {
                    permissionLauncher.launch(arrayOf(PermissionManager.READ_CONTACTS_PERMISSION))
                }
            )

            AccessRow(
                title = "Call History",
                subtitle = "Detect missed calls",
                enabled = hasCallLogAccess,
                onClick = {
                    permissionLauncher.launch(arrayOf(PermissionManager.READ_CALL_LOG_PERMISSION))
                }
            )

            AccessRow(
                title = "Messages",
                subtitle = "Read and summarize SMS",
                enabled = hasSmsAccess,
                onClick = {
                    permissionLauncher.launch(arrayOf(PermissionManager.READ_SMS_PERMISSION))
                }
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Done", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AccessRow(title: String, subtitle: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (enabled) SuccessGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary,
                    contentColor = if (enabled) SuccessGreen else MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    if (enabled) "ENABLED" else "ENABLE", 
                    fontSize = 10.sp, 
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
