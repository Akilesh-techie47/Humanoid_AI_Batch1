package com.humanoidai.security.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.humanoidai.security.AiPermission
import com.humanoidai.security.SecurityPolicyProfile
import com.humanoidai.ui.theme.AccentCyan
import com.humanoidai.ui.theme.BackgroundDark
import com.humanoidai.ui.theme.SuccessGreen
import com.humanoidai.ui.theme.SurfaceDark
import com.humanoidai.ui.theme.TextPrimary
import com.humanoidai.ui.theme.TextSecondary

@Composable
fun PrivacyDashboardScreen(viewModel: PrivacyDashboardViewModel) {
    val session by viewModel.currentSession.collectAsState()
    val permissions by viewModel.permissions.collectAsState()
    val policy by viewModel.policy.collectAsState()

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
            Text("Privacy Dashboard", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Spacer(Modifier.height(16.dp))

            // Session Info
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("AI Session Status", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (session != null) Icons.Default.Shield else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (session != null) SuccessGreen else Color.Red
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = session?.status?.name ?: "NO ACTIVE SESSION",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    session?.let {
                        Text("ID: ${it.id}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Privacy Policy
            Text("Security Policy", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SecurityPolicyProfile.entries.forEach { p ->
                    FilterChip(
                        selected = policy == p,
                        onClick = { viewModel.setPolicy(p) },
                        label = { Text(p.name.replace("_", " ")) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentCyan.copy(alpha = 0.2f),
                            selectedLabelColor = AccentCyan
                        )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Permissions
            Text("Active Permissions", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            permissions.forEach { (perm, granted) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(perm.name, color = TextPrimary)
                    Text(
                        text = if (granted) "GRANTED" else "DENIED",
                        color = if (granted) SuccessGreen else Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Button(
                onClick = { viewModel.refreshPermissions() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
            ) {
                Text("Refresh Permissions", color = Color.Black)
            }
        }
    }
}

