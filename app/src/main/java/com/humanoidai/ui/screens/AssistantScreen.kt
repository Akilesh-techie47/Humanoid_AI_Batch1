package com.humanoidai.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

import com.humanoidai.ui.components.ArmsunFooter

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
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "AI ASSISTANT", 
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, 
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, "Menu", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Chat Area
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        reverseLayout = true,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp, start = 16.dp, end = 16.dp)
                    ) {
                        items(messages.reversed()) { msg ->
                            AssistantBubble(msg)
                        }
                    }

                    // Bottom Panel for Status and Input
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    ) {
                        // AI State Pill
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                border = BorderStroke(1.dp, when(companionState) {
                                    CompanionState.THINKING -> WarningOrange.copy(alpha = 0.3f)
                                    CompanionState.SPEAKING -> SuccessGreen.copy(alpha = 0.3f)
                                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                }),
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (companionState == CompanionState.THINKING) {
                                        CircularProgressIndicator(modifier = Modifier.size(10.dp), color = WarningOrange, strokeWidth = 1.5.dp)
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Text(
                                        companionState.label.uppercase(),
                                        color = when(companionState) {
                                            CompanionState.THINKING -> WarningOrange
                                            CompanionState.SPEAKING -> SuccessGreen
                                            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }

                        // Input Bar
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                                .imePadding(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AuraTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                label = "Message Humanoid...",
                                modifier = Modifier.weight(1f)
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
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                shape = CircleShape,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.Send, "Send", modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
                
                ArmsunFooter(modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

@Composable
fun AssistantBubble(msg: ChatMessage) {
    val alignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (msg.isUser) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.background
    val borderColor = if (msg.isUser) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    val textColor = if (msg.isUser) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
    
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp) // P0: Prevent full-width stretching
                .clip(RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (msg.isUser) 16.dp else 4.dp,
                    bottomEnd = if (msg.isUser) 4.dp else 16.dp
                ))
                .background(bgColor.copy(alpha = 0.8f))
                .border(1.dp, borderColor, RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (msg.isUser) 16.dp else 4.dp,
                    bottomEnd = if (msg.isUser) 4.dp else 16.dp
                ))
                .padding(12.dp)
        ) {
            Text(
                text = if (msg.isUser) "YOU" else "HUMANOID",
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                msg.text,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp
            )
        }
    }
}
