package com.example.personalfinancetracker.ui.budget

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.personalfinancetracker.databinding.FragmentBudgetBinding
import com.example.personalfinancetracker.utils.CurrencyFormatter
import com.example.personalfinancetracker.utils.PreferenceManager

class BudgetFragment : Fragment() {

    private var _binding: FragmentBudgetBinding? = null
    private val binding get() = _binding!!
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferenceManager = PreferenceManager(requireContext())
        CurrencyFormatter.initialize(requireContext())
        setupUI()
    }

    private fun setupUI() {
        // Set current budget if exists
        val currentBudget = preferenceManager.getMonthlyBudget()
        if (currentBudget > 0) {
            binding.etBudget.setText(currentBudget.toString())
        }

        // Setup save button
        binding.btnSaveBudget.setOnClickListener {
            saveBudget()
        }

        updateBudgetProgress()
    }

    private fun saveBudget() {
        val budgetText = binding.etBudget.text.toString()
        if (budgetText.isBlank()) {
            Toast.makeText(context, "Please enter a budget amount", Toast.LENGTH_SHORT).show()
            return
        }

        val budget = budgetText.toDoubleOrNull()
        if (budget == null || budget <= 0) {
            Toast.makeText(context, "Please enter a valid budget amount", Toast.LENGTH_SHORT).show()
            return
        }

        preferenceManager.setMonthlyBudget(budget)
        Toast.makeText(context, "Budget saved successfully", Toast.LENGTH_SHORT).show()
        updateBudgetProgress()
    }

    private fun updateBudgetProgress() {
        val monthlyBudget = preferenceManager.getMonthlyBudget()
        val monthlyExpenses = preferenceManager.getMonthlyExpenses()

        if (monthlyBudget > 0) {
            val progress = (monthlyExpenses / monthlyBudget * 100).toInt()
            binding.progressBudget.progress = progress
            binding.tvBudgetProgress.text = "Spent ${CurrencyFormatter.format(monthlyExpenses)} of ${CurrencyFormatter.format(monthlyBudget)}"

            // Show warning if budget is close to or exceeded
            when {
                monthlyExpenses >= monthlyBudget -> {
                    binding.tvBudgetWarning.text = "Budget exceeded!"
                    binding.tvBudgetWarning.visibility = View.VISIBLE
                }
                monthlyExpenses >= monthlyBudget * 0.8 -> {
                    binding.tvBudgetWarning.text = "Budget almost exceeded!"
                    binding.tvBudgetWarning.visibility = View.VISIBLE
                }
                else -> {
                    binding.tvBudgetWarning.visibility = View.GONE
                }
            }
        } else {
            binding.progressBudget.progress = 0
            binding.tvBudgetProgress.text = "No budget set"
            binding.tvBudgetWarning.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 