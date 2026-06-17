package com.humanoidai.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.humanoidai.ui.theme.*
import kotlinx.coroutines.launch

data class ChatMessage(val text: String, val isUser: Boolean)

@Composable
fun AssistantScreen(navController: NavController) {
    val messages = remember {
        mutableStateListOf(
            ChatMessage("Good morning! I can see Ravi Kumar is the primary focus subject — confidence 94%. You have a meeting in 15 minutes. How can I help?", false)
        )
    }
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111115))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextSecondary)
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(AccentCyan.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                        .border(1.dp, AccentCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                }
                Column {
                    Text("Humanoid AI", fontSize = 14.sp, color = TextPrimary)
                    Text("Context-aware · Gemini", fontSize = 11.sp, color = AlertGreen)
                }
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .background(AccentCyan.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                        .border(1.dp, AccentCyan.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("Watching", fontSize = 10.sp, color = AccentCyan)
                }
            }
        }

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            items(messages) { msg ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 300.dp)
                            .background(
                                if (msg.isUser) AccentCyan else Color(0xFF18181E),
                                if (msg.isUser) RoundedCornerShape(12.dp, 3.dp, 12.dp, 12.dp)
                                else RoundedCornerShape(3.dp, 12.dp, 12.dp, 12.dp)
                            )
                            .then(
                                if (!msg.isUser) Modifier.border(
                                    1.dp, Color(0xFF2A2A35),
                                    RoundedCornerShape(3.dp, 12.dp, 12.dp, 12.dp)
                                ) else Modifier
                            )
                            .padding(10.dp, 9.dp)
                    ) {
                        Text(
                            msg.text,
                            fontSize = 13.sp,
                            color = if (msg.isUser) Color.Black else TextPrimary,
                            lineHeight = 19.sp
                        )
                    }
                }
            }
        }

        // Input bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111115))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Ask about what I see...", color = TextSecondary, fontSize = 13.sp) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentCyan.copy(alpha = 0.4f),
                    unfocusedBorderColor = Color(0xFF2A2A35),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentCyan,
                    focusedContainerColor = Color(0xFF18181E),
                    unfocusedContainerColor = Color(0xFF18181E)
                ),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
            )
            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        messages.add(ChatMessage(inputText, true))
                        inputText = ""
                        // TODO: call Gemini API here
                        messages.add(ChatMessage("Processing your request with current camera context...", false))
                        scope.launch { listState.animateScrollToItem(messages.size - 1) }
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .background(AccentCyan, RoundedCornerShape(10.dp))
            ) {
                Icon(Icons.Default.ArrowUpward, contentDescription = "Send", tint = Color.Black, modifier = Modifier.size(18.dp))
            }
        }
    }
}