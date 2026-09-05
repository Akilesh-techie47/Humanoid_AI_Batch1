package com.humanoidai.distributed.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.humanoidai.distributed.AIInstance
import com.humanoidai.distributed.SyncPolicy
import com.humanoidai.embodiment.EmbodimentType
import com.humanoidai.ui.theme.AccentCyan
import com.humanoidai.ui.theme.BackgroundDark
import com.humanoidai.ui.theme.SurfaceDark
import com.humanoidai.ui.theme.SuccessGreen
import com.humanoidai.ui.theme.TextPrimary
import com.humanoidai.ui.theme.TextSecondary

@Composable
fun CoordinationInspectorScreen(viewModel: CoordinationInspectorViewModel) {
    val instances by viewModel.instances.collectAsState()
    val syncPolicy by viewModel.syncPolicy.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BackgroundDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Coordination Inspector", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                IconButton(
                    onClick = {
                        viewModel.addMockInstance(AIInstance(type = EmbodimentType.ROBOT, capabilities = emptySet()))
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Mock Robot", tint = AccentCyan)
                }
            }
            
            Spacer(Modifier.height(16.dp))

            // Sync Policy
            Text("Synchronization Policy", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SyncPolicy.entries.forEach { p ->
                    FilterChip(
                        selected = syncPolicy == p,
                        onClick = { viewModel.setSyncPolicy(p) },
                        label = { Text(p.name.replace("_", " ")) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentCyan.copy(alpha = 0.2f),
                            selectedLabelColor = AccentCyan
                        )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Instances
            Text("Connected Instances (${instances.size})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                items(instances.values.toList()) { instance ->
                    InstanceCard(
                        instance = instance,
                        isLocal = instance.id == viewModel.localInstanceId,
                    ) { viewModel.removeInstance(instance.id) }
                }
            }
        }
    }
}

@Composable
fun InstanceCard(instance: AIInstance, isLocal: Boolean, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLocal) AccentCyan.copy(alpha = 0.1f) else SurfaceDark
        ),
        border = if (isLocal) BorderStroke(1.dp, AccentCyan) else null
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Hub, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (isLocal) AccentCyan else TextSecondary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isLocal) "LOCAL: ${instance.type}" else "REMOTE: ${instance.type}",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                if (!isLocal) {
                    TextButton(onClick = onRemove) {
                        Text("Disconnect", color = Color.Red)
                    }
                }
            }
            Text("ID: ${instance.id}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            
            Spacer(Modifier.height(8.dp))
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Workload: ${instance.workloadPercent}%", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                Text("Battery: ${instance.batteryLevel}%", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            }
        }
    }
}
