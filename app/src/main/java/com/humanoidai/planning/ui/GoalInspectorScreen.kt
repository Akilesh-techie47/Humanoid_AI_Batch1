package com.humanoidai.planning.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.humanoidai.planning.Goal
import com.humanoidai.planning.GoalStatus
import com.humanoidai.planning.StepStatus
import com.humanoidai.ui.theme.AccentCyan
import com.humanoidai.ui.theme.SuccessGreen

@Composable
fun GoalInspectorScreen(viewModel: GoalInspectorViewModel) {
    val goals by viewModel.activeGoals.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("AI Goal Inspector", style = MaterialTheme.typography.headlineMedium)
            Button(onClick = { viewModel.clearCompleted() }) {
                Text("Clear Done")
            }
        }
        
        Spacer(Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(goals.values.toList().sortedByDescending { it.creationTime }) { goal ->
                GoalCard(goal, onCancel = { viewModel.cancelGoal(goal.id) })
            }
        }
    }
}

@Composable
fun GoalCard(goal: Goal, onCancel: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(goal.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Type: ${goal.type} | Priority: ${goal.priority}", style = MaterialTheme.typography.labelSmall)
                }
                
                if (goal.status != GoalStatus.COMPLETED && goal.status != GoalStatus.CANCELLED) {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Cancel, contentDescription = "Cancel", tint = Color.Red)
                    }
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = { goal.progress },
                modifier = Modifier.fillMaxWidth(),
                color = if (goal.status == GoalStatus.FAILED) Color.Red else SuccessGreen
            )
            
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Status: ${goal.status}", style = MaterialTheme.typography.bodySmall)
                Text("${(goal.progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(8.dp))
            
            Text("Plan Steps:", style = MaterialTheme.typography.labelMedium)
            goal.steps.forEach { step ->
                StepRow(step)
            }
        }
    }
}

@Composable
fun StepRow(step: com.humanoidai.planning.PlanStep) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (step.status) {
                StepStatus.COMPLETED -> Icons.Default.CheckCircle
                StepStatus.RUNNING -> Icons.Default.HourglassEmpty
                else -> Icons.Default.HourglassEmpty
            },
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = when (step.status) {
                StepStatus.COMPLETED -> SuccessGreen
                StepStatus.RUNNING -> AccentCyan
                StepStatus.FAILED -> Color.Red
                else -> Color.Gray
            }
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(step.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            Text(step.description, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}
