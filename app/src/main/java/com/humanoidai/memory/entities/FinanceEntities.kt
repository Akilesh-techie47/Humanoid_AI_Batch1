package com.humanoidai.memory.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "finance_transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val amount: Long,
    val type: String, // INCOME / EXPENSE
    val category: String,
    val description: String,
    val timestamp: Long,
    val merchant: String?,
    val source: String,
    val currency: String,
    val notes: String?
)

@Entity(tableName = "finance_budgets")
data class BudgetEntity(
    @PrimaryKey val id: String,
    val category: String,
    val targetAmount: Long,
    val period: String,
    val startDate: Long,
    val endDate: Long
)

@Entity(tableName = "finance_savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val targetAmount: Long,
    val currentAmount: Long,
    val targetDate: Long,
    val createdAt: Long
)
