package com.humanoidai.ui.apps.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.humanoidai.platform.model.finance.FinancialAnalysisEngine
import com.humanoidai.ui.theme.AuraSpacing
import com.humanoidai.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialAnalysisScreen(
    navController: NavController,
    viewModel: FinanceViewModel
) {
    val transactions by viewModel.currentMonthTransactions.collectAsState()
    val period by viewModel.selectedPeriod.collectAsState()
    
    val totalIncome = remember(transactions) { FinancialAnalysisEngine.calculateTotalIncome(transactions) }
    val totalExpenses = remember(transactions) { FinancialAnalysisEngine.calculateTotalExpenses(transactions) }
    val netCashFlow = totalIncome - totalExpenses
    val savingsRate = remember(totalIncome, totalExpenses) { FinancialAnalysisEngine.calculateSavingsRate(totalIncome, totalExpenses) }
    val categories = remember(transactions) { FinancialAnalysisEngine.calculateSpendingByCategory(transactions) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("FINANCIAL ANALYSIS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
            Text(
                text = "Report for $period",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
            
            Spacer(Modifier.height(24.dp))

            AnalysisSummaryCard(
                income = totalIncome,
                expenses = totalExpenses,
                net = netCashFlow,
                savingsRate = savingsRate
            )

            Spacer(Modifier.height(32.dp))

            Text("Spending by Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            if (categories.isEmpty()) {
                Text("No data for this period", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                categories.entries.sortedByDescending { it.value }.forEach { entry ->
                    CategorySpendingItem(name = entry.key, amount = entry.value, total = totalExpenses)
                }
            }

            Spacer(Modifier.height(32.dp))
            
            AssistantQuickAction(onClick = { navController.navigate("finance_ai") })
            
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
fun AnalysisSummaryCard(income: Long, expenses: Long, net: Long, savingsRate: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryStat("Income", formatCurrency(income), SuccessGreen)
                SummaryStat("Expenses", formatCurrency(expenses), Color.Red)
            }
            Spacer(Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryStat("Net Cash Flow", formatCurrency(net), if (net >= 0) SuccessGreen else Color.Red)
                SummaryStat("Savings Rate", "$savingsRate%", SuccessGreen)
            }
        }
    }
}

@Composable
fun SummaryStat(label: String, value: String, color: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun CategorySpendingItem(name: String, amount: Long, total: Long) {
    val percentage = if (total > 0) (amount.toDouble() / total).toFloat() else 0f
    
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, style = MaterialTheme.typography.bodyLarge)
            Text(formatCurrency(amount), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { percentage },
            modifier = Modifier.fillMaxWidth().height(6.dp).background(Color.Transparent, RoundedCornerShape(3.dp)),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
