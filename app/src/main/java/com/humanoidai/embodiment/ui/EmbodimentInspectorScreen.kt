package com.humanoidai.embodiment.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Hardware
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.humanoidai.embodiment.Capability
import com.humanoidai.ui.theme.BackgroundDark
import com.humanoidai.ui.theme.SurfaceDark
import com.humanoidai.ui.theme.SuccessGreen
import com.humanoidai.ui.theme.TextPrimary
import com.humanoidai.ui.theme.TextSecondary

@Composable
fun EmbodimentInspectorScreen(viewModel: EmbodimentInspectorViewModel) {
    val activeEmbodiment by viewModel.activeEmbodiment.collectAsState()
    val worldState by viewModel.worldState.collectAsState()
    val allCapabilities by viewModel.capabilities.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BackgroundDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Text("Embodiment Inspector", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Spacer(modifier = Modifier.height(16.dp))

            // Active Embodiment Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Active Embodiment", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    activeEmbodiment?.let {
                        Text("ID: ${it.profile.id}", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Type: ${it.profile.type}", color = TextSecondary)
                        Text("Model: ${it.profile.model} (${it.profile.manufacturer})", color = TextSecondary)
                    } ?: Text("No active embodiment", color = Color.Red)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Capabilities Grid
            Text("Hardware Capabilities", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth().height(200.dp)
            ) {
                items(Capability.entries) { cap ->
                    val hasCap = allCapabilities.contains(cap)
                    Row(
                        modifier = Modifier.padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (hasCap) Icons.Default.CheckCircle else Icons.Default.Hardware,
                            contentDescription = null,
                            tint = if (hasCap) SuccessGreen else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(cap.name, style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // World State Summary
            Text("World State", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(Modifier.padding(16.dp)) {
                    worldState?.let { ws ->
                        Text("Objects Detected: ${ws.environment.detectedObjectCount}", color = TextSecondary)
                        Text("User Present: ${if (ws.environment.activeUserPresent) "YES" else "NO"}", color = TextSecondary)
                        Text("Thermal Status: ${ws.environment.thermalPressure}", color = TextSecondary)
                    } ?: Text("World state unavailable", color = TextSecondary)
                }
            }
        }
    }
}
