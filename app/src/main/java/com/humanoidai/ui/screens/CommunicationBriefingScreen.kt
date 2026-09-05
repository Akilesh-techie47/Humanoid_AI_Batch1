package com.humanoidai.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.humanoidai.ai.AIManager
import com.humanoidai.communication.*
import com.humanoidai.ui.theme.*
import com.humanoidai.voice.VoiceEngine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunicationBriefingScreen(navController: NavController, aiManager: AIManager, voiceEngine: VoiceEngine) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val engine = remember { CommunicationIntelligenceEngine(context).apply { setAIManager(aiManager) } }
    
    var briefingText by remember { mutableStateOf("GENERATING BRIEFING...") }
    var missedCalls by remember { mutableStateOf<List<CommunicationItem>>(emptyList()) }
    var notifications by remember { mutableStateOf<List<CommunicationItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            briefingText = engine.getWhatDidIMissSummary()
            missedCalls = MissedCallDetector(context).getMissedCalls()
            notifications = HumanoidNotificationListener.notifications.value
            isLoading = false
            voiceEngine.speak(briefingText)
        }
    }

    Scaffold(
        containerColor = BackgroundDark,
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
                        .background(SurfaceDark.copy(alpha = 0.4f))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                
                Spacer(Modifier.width(16.dp))
                
                Text("SYSTEM BRIEFING", fontSize = 15.sp, fontWeight = FontWeight.Black, color = AccentCyan, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            // ---- AI Briefing Section ----
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, null, tint = AccentCyan, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("AI BRIEFING", color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = AccentCyan, strokeWidth = 2.dp)
                    } else {
                        Text(
                            briefingText,
                            color = Color.White,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            Text("DETAILED LOG", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    if (missedCalls.isEmpty() && notifications.isEmpty() && !isLoading) {
                        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Text("No recent communications", color = TextSecondary, fontSize = 14.sp)
                        }
                    }
                }
                
                items(missedCalls) { call ->
                    CommItemRow(call)
                }
                
                items(notifications) { notif ->
                    CommItemRow(notif)
                }
            }
        }
    }
}

@Composable
private fun CommItemRow(item: CommunicationItem) {
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val timeStr = timeFormat.format(Date(item.timestamp))

    val icon = when (item.type) {
        CommunicationType.WHATSAPP -> Icons.Default.Chat
        CommunicationType.TELEGRAM -> Icons.AutoMirrored.Filled.Send
        CommunicationType.SMS -> Icons.Default.Sms
        CommunicationType.MISSED_CALL -> Icons.AutoMirrored.Filled.CallMissed
        CommunicationType.EMAIL -> Icons.Default.Email
        CommunicationType.GENERIC -> Icons.Default.Notifications
    }

    val iconColor = when (item.type) {
        CommunicationType.MISSED_CALL -> ErrorRed
        else -> AccentCyan
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = SurfaceDark.copy(alpha = 0.4f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(item.sender, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(timeStr, color = TextSecondary.copy(alpha = 0.6f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
                Text(
                    item.sourceApp.uppercase(), 
                    color = iconColor.copy(alpha = 0.7f), 
                    fontSize = 9.sp, 
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    item.contentPreview,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            if (item.isPriority) {
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.Star, null, tint = WarningOrange, modifier = Modifier.size(16.dp))
            }
        }
    }
}
