package com.example.personalfinancetracker.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.personalfinancetracker.databinding.FragmentDashboardBinding
import com.example.personalfinancetracker.model.TransactionType
import com.example.personalfinancetracker.utils.CurrencyFormatter
import com.example.personalfinancetracker.utils.PreferenceManager
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import java.text.NumberFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class
DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferenceManager = PreferenceManager(requireContext())
        CurrencyFormatter.initialize(requireContext())
        setupUI()
    }

    private fun setupUI() {
        updateMonthlySummary()
        updateBudgetProgress()
        setupPieChart()
    }

    fun updateMonthlySummary() {
        val transactions = preferenceManager.getTransactions()
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        val monthlyIncome = transactions
            .filter { 
                it.type == TransactionType.INCOME && 
                isSameMonthAndYear(it.date, currentMonth, currentYear)
            }
            .sumOf { it.amount }

        val monthlyExpenses = transactions
            .filter { 
                it.type == TransactionType.EXPENSE && 
                isSameMonthAndYear(it.date, currentMonth, currentYear)
            }
            .sumOf { it.amount }

        val monthlyBalance = monthlyIncome - monthlyExpenses

        binding.tvMonthlyIncome.text = CurrencyFormatter.format(monthlyIncome)
        binding.tvMonthlyExpenses.text = CurrencyFormatter.format(monthlyExpenses)
        binding.tvMonthlyBalance.text = CurrencyFormatter.format(monthlyBalance)
    }

    private fun isSameMonthAndYear(date: Date, month: Int, year: Int): Boolean {
        val calendar = Calendar.getInstance()
        calendar.time = date
        return calendar.get(Calendar.MONTH) == month && 
               calendar.get(Calendar.YEAR) == year
    }

    fun updateBudgetProgress() {
        val monthlyBudget = preferenceManager.getMonthlyBudget()
        val monthlyExpenses = preferenceManager.getMonthlyExpenses()

        if (monthlyBudget > 0) {
            val progress = (monthlyExpenses / monthlyBudget * 100).toInt()
            binding.progressBudget.progress = progress
            binding.tvBudgetProgress.text = "Spent ${CurrencyFormatter.format(monthlyExpenses)} of ${CurrencyFormatter.format(monthlyBudget)}"
        } else {
            binding.progressBudget.progress = 0
            binding.tvBudgetProgress.text = "No budget set"
        }
    }

    fun setupPieChart() {
        val categoryExpenses = preferenceManager.getCategoryExpenses()
        val entries = categoryExpenses.map { PieEntry(it.value.toFloat(), it.key) }

        val dataSet = PieDataSet(entries, "Spending by Category")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextSize = 12f

        val data = PieData(dataSet)
        binding.pieChart.data = data
        binding.pieChart.description.isEnabled = false
        binding.pieChart.legend.isEnabled = true
        binding.pieChart.setEntryLabelTextSize(12f)
        binding.pieChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 