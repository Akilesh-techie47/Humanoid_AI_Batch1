package com.humanoidai.ui.apps.finance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.humanoidai.platform.model.finance.SavingsGoal
import com.humanoidai.ui.theme.AuraSpacing
import com.humanoidai.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailsScreen(
    navController: NavController,
    goalId: String,
    viewModel: FinanceViewModel
) {
    var goal by remember { mutableStateOf<SavingsGoal?>(null) }
    var contributionAmount by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(goalId) {
        goal = viewModel.getGoalById(goalId)
    }

    if (goal == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val g = goal!!

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("GOAL DETAILS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, "Delete", tint = Color.Red)
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
                .verticalScroll(rememberScrollState())
                .padding(AuraSpacing.large)
        ) {
            Text(g.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text("Target date: ${formatDate(g.targetDate)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Spacer(Modifier.height(32.dp))

            SavingsGoalCard(goal = g, onClick = {})

            Spacer(Modifier.height(48.dp))

            Text("Add Contribution", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            
            OutlinedTextField(
                value = contributionAmount,
                onValueChange = { if (it.all { char -> char.isDigit() }) contributionAmount = it },
                label = { Text("Amount (Rupees)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                prefix = { Text("₹") }
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    val amount = contributionAmount.toLongOrNull() ?: 0L
                    if (amount > 0) {
                        viewModel.updateGoalAmount(g, g.currentAmount + (amount * 100))
                        contributionAmount = ""
                        // Refresh
                        navController.popBackStack() 
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = contributionAmount.isNotEmpty()
            ) {
                Text("Confirm Contribution", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Goal") },
            text = { Text("Are you sure you want to delete this savings goal?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteGoal(g)
                        navController.popBackStack()
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
