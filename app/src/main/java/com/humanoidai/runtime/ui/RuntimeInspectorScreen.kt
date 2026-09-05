package com.humanoidai.runtime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.humanoidai.runtime.HealthState
import com.humanoidai.runtime.RuntimeProfile
import com.humanoidai.ui.theme.AccentCyan
import com.humanoidai.ui.theme.BackgroundDark
import com.humanoidai.ui.theme.SurfaceDark
import com.humanoidai.ui.theme.TextPrimary
import com.humanoidai.ui.theme.TextSecondary

@Composable
fun RuntimeInspectorScreen(viewModel: RuntimeInspectorViewModel) {
    val metrics by viewModel.metrics.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val health by viewModel.health.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BackgroundDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("AI Runtime Inspector", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Spacer(Modifier.height(16.dp))

            // System Health
            StatusCard(
                title = "System Health",
                value = health.name,
                color = when (health) {
                    HealthState.HEALTHY -> Color.Green
                    HealthState.BUSY -> Color.Yellow
                    HealthState.DEGRADED -> Color.Red
                    else -> Color.Gray
                }
            )

            Spacer(Modifier.height(8.dp))

            // Active Profile
            Text("Runtime Profile", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                RuntimeProfile.entries.forEach { p ->
                    FilterChip(
                        selected = profile == p,
                        onClick = { viewModel.setProfile(p) },
                        label = { Text(p.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentCyan.copy(alpha = 0.2f),
                            selectedLabelColor = AccentCyan
                        )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Resource Metrics
            MetricSection("CPU") {
                MetricRow("Utilization", "${metrics.cpu.utilizationPercent}%")
                MetricRow("Threads", "${metrics.cpu.activeThreads}")
            }

            MetricSection("Memory") {
                MetricRow("Heap Used", "${metrics.memory.heapUsedMb} MB")
                MetricRow("Heap Max", "${metrics.memory.heapMaxMb} MB")
                MetricRow("Native", "${metrics.memory.nativeUsedMb} MB")
            }

            MetricSection("Battery") {
                MetricRow("Level", "${metrics.battery.percentage}%")
                MetricRow("Status", if (metrics.battery.isCharging) "Charging" else "Discharging")
                MetricRow("Current", "${metrics.battery.currentNowMa} mA")
            }

            MetricSection("AI Scheduler") {
                MetricRow("Queued Tasks", "${metrics.ai.tasksQueued}")
                MetricRow("Avg Latency", "${metrics.ai.averageInferenceTimeMs} ms")
            }

            MetricSection("Thermal") {
                MetricRow("Status", "${metrics.thermal.status}")
                MetricRow("Throttling", if (metrics.thermal.isThrottling) "YES" else "NO")
            }
        }
    }
}

@Composable
fun StatusCard(title: String, value: String, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Text(value, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MetricSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = AccentCyan)
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
        Column(content = content)
    }
}

@Composable
fun MetricRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}
