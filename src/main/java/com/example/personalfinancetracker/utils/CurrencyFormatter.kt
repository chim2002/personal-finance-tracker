package com.example.personalfinancetracker.utils

import android.content.Context
import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    private var currencySymbol: String = "$"
    private var numberFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.getDefault())

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences("FinanceTrackerPrefs", Context.MODE_PRIVATE)
        currencySymbol = prefs.getString("currency", "$") ?: "$"
        updateNumberFormat()
    }

    fun setCurrencySymbol(symbol: String) {
        currencySymbol = symbol
        updateNumberFormat()
    }

    private fun updateNumberFormat() {
        numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())
        numberFormat.minimumFractionDigits = 2
        numberFormat.maximumFractionDigits = 2
    }

    fun format(amount: Double): String {
        return "$currencySymbol${numberFormat.format(amount)}"
    }
} 