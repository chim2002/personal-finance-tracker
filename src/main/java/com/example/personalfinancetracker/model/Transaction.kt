package com.example.personalfinancetracker.model

import java.util.Date
import java.util.UUID

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val category: String,
    val type: TransactionType,
    val date: Date = Date(),
    val note: String = ""
) {
    companion object {
        const val serialVersionUID = 1L
    }
}

enum class TransactionType {
    INCOME, EXPENSE
} 