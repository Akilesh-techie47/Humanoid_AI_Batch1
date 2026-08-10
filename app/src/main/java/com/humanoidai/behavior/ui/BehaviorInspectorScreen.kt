package com.humanoidai.behavior.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.humanoidai.behavior.CommunicationLevel
import com.humanoidai.behavior.InteractionTurnState
import com.humanoidai.behavior.PersonalityProfile
import com.humanoidai.ui.theme.AccentCyan
import com.humanoidai.ui.theme.SuccessGreen

@Composable
fun BehaviorInspectorScreen(viewModel: BehaviorInspectorViewModel) {
    val level by viewModel.level.collectAsState()
    val personality by viewModel.personality.collectAsState()
    val turnState by viewModel.turnState.collectAsState()
    val history by viewModel.history.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text("AI Behavior Inspector", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // Interaction State
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Current Turn State", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = turnState.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = when (turnState) {
                        InteractionTurnState.RESPONDING -> SuccessGreen
                        InteractionTurnState.LISTENING -> AccentCyan
                        else -> Color.Gray
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Policies
        Text("Communication Level", style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            CommunicationLevel.entries.forEach { l ->
                FilterChip(
                    selected = level == l,
                    onClick = { viewModel.setLevel(l) },
                    label = { Text(l.name) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("Personality Profile", style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), horizontalArrangement = Arrangement.SpaceBetween) {
            PersonalityProfile.entries.forEach { p ->
                FilterChip(
                    selected = personality == p,
                    onClick = { viewModel.setPersonality(p) },
                    label = { Text(p.name) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // History
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Interaction History", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { viewModel.clearHistory() }) {
                Text("Clear")
            }
        }
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(history.reversed()) { turn ->
                Column(Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = turn.userText ?: "[SYSTEM/AUTONOMOUS]",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentCyan
                    )
                    Text(
                        text = turn.aiResponse,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    HorizontalDivider(modifier = Modifier.padding(top = 4.dp), thickness = 0.5.dp)
                }
            }
        }
    }
}
