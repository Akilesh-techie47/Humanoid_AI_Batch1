package com.humanoidai.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.humanoidai.ai.AIManager
import com.humanoidai.ai.ChatMessage
import com.humanoidai.companion.CompanionEngine
import com.humanoidai.companion.CompanionState
import com.humanoidai.ui.components.SidePanelDrawer
import com.humanoidai.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(
    navController: NavController,
    aiManager: AIManager,
    companionEngine: CompanionEngine,
    ownerName: String
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val messages by aiManager.getMessages().collectAsState()
    val companionState by companionEngine.state.collectAsState()
    
    var inputText by remember { mutableStateOf("") }

    SidePanelDrawer(navController, drawerState) {
        Scaffold(
            containerColor = BackgroundDark,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text("AI ASSISTANT", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccentCyan)
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
                // Chat Area
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    reverseLayout = true,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(messages.reversed()) { msg ->
                        AssistantBubble(msg)
                    }
                }

                // Status Indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(SurfaceDark.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (companionState == CompanionState.THINKING) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), color = AccentCyan, strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                        }
                        Text(
                            companionState.label.uppercase(),
                            color = when(companionState) {
                                CompanionState.THINKING -> WarningOrange
                                CompanionState.SPEAKING -> SuccessGreen
                                else -> AccentCyan
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Input Area
                Surface(
                    color = SurfaceDark,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .imePadding(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Ask Humanoid anything...", color = TextSecondary) },
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentCyan,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        
                        Spacer(Modifier.width(12.dp))
                        
                        FloatingActionButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    val q = inputText
                                    inputText = ""
                                    companionEngine.ask(q, ownerName, "Assistant")
                                }
                            },
                            containerColor = AccentCyan,
                            contentColor = Color.Black,
                            shape = CircleShape,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(Icons.Default.Send, "Send")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AssistantBubble(msg: ChatMessage) {
    val alignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (msg.isUser) Color(0xFF1E1E2E) else SurfaceDark
    val textColor = if (msg.isUser) Color.White else Color.White.copy(alpha = 0.9f)
    
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(bgColor, RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (msg.isUser) 16.dp else 4.dp,
                    bottomEnd = if (msg.isUser) 4.dp else 16.dp
                ))
                .padding(12.dp)
        ) {
            Text(
                msg.text,
                color = textColor,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}
