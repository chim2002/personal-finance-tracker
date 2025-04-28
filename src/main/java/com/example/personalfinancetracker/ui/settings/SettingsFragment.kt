package com.example.personalfinancetracker.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.personalfinancetracker.R
import com.example.personalfinancetracker.databinding.FragmentSettingsBinding
import com.example.personalfinancetracker.model.Transaction
import com.example.personalfinancetracker.ui.dashboard.DashboardFragment
import com.example.personalfinancetracker.ui.transactions.TransactionsFragment
import com.example.personalfinancetracker.utils.CurrencyFormatter
import com.example.personalfinancetracker.utils.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var preferenceManager: PreferenceManager
    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    private val IMPORT_REQUEST_CODE = 1001

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferenceManager = PreferenceManager(requireContext())
        CurrencyFormatter.initialize(requireContext())
        setupUI()
    }

    private fun setupUI() {
        // Setup currency dropdown
        val currencies = resources.getStringArray(R.array.currencies)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, currencies)
        binding.etCurrency.apply {
            setAdapter(adapter)
            setOnClickListener {
                showDropDown()
            }
            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    showDropDown()
                }
            }
        }

        // Set current currency
        val currentCurrency = preferenceManager.getCurrency()
        val currentCurrencyIndex = currencies.indexOfFirst { it.startsWith(currentCurrency) }
        if (currentCurrencyIndex != -1) {
            binding.etCurrency.setText(currencies[currentCurrencyIndex], false)
        }

        // Setup currency save button
        binding.btnSaveCurrency.setOnClickListener {
            val selectedCurrency = binding.etCurrency.text.toString()
            if (selectedCurrency.isBlank()) {
                Toast.makeText(context, "Please select a currency", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Extract just the symbol from the selected currency (e.g., "$" from "$ - USD")
            val currencySymbol = selectedCurrency.split(" - ")[0].trim()
            preferenceManager.setCurrency(currencySymbol)
            CurrencyFormatter.setCurrencySymbol(currencySymbol)
            Toast.makeText(context, "Currency saved successfully", Toast.LENGTH_SHORT).show()
        }

        // Setup export button
        binding.btnExportData.setOnClickListener {
            exportData()
        }

        // Setup import button
        binding.btnImportData.setOnClickListener {
            importData()
        }
    }

    private fun exportData() {
        try {
            val data = mapOf(
                "transactions" to preferenceManager.getTransactions(),
                "budget" to preferenceManager.getMonthlyBudget(),
                "currency" to preferenceManager.getCurrency()
            )

            val json = gson.toJson(data)
            val fileName = "finance_data_${dateFormat.format(Date())}.json"
            val file = File(requireContext().getExternalFilesDir(null), fileName)

            FileOutputStream(file).use { output ->
                output.write(json.toByteArray())
            }

            Toast.makeText(context, "Data exported to: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to export data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun importData() {
        try {
            // Get the most recent backup file
            val backupDir = requireContext().getExternalFilesDir(null)
            val backupFiles = backupDir?.listFiles { file -> 
                file.name.startsWith("finance_data_") && file.name.endsWith(".json")
            }?.sortedByDescending { it.lastModified() }

            if (backupFiles.isNullOrEmpty()) {
                Toast.makeText(context, "No backup files found", Toast.LENGTH_SHORT).show()
                return
            }

            val latestBackup = backupFiles.first()
            
            // Show confirmation dialog
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Import Data")
                .setMessage("This will replace all existing data with the backup from ${latestBackup.name}. Are you sure?")
                .setPositiveButton("Import") { _, _ ->
                    try {
                        val json = latestBackup.readText()
                        val type = object : TypeToken<Map<String, Any>>() {}.type
                        val importedData = gson.fromJson<Map<String, Any>>(json, type)

                        // Validate imported data
                        if (!importedData.containsKey("transactions") || 
                            !importedData.containsKey("budget") || 
                            !importedData.containsKey("currency")) {
                            throw IllegalArgumentException("Invalid backup file format")
                        }

                        val transactionsType = object : TypeToken<List<Transaction>>() {}.type
                        val transactions = gson.fromJson<List<Transaction>>(
                            gson.toJson(importedData["transactions"]),
                            transactionsType
                        )

                        val budget = (importedData["budget"] as Number).toDouble()
                        val currency = importedData["currency"] as String

                        // Validate transactions
                        if (transactions.any { it.id.isBlank() || it.title.isBlank() || it.amount <= 0 }) {
                            throw IllegalArgumentException("Invalid transaction data")
                        }

                        // Clear existing data
                        preferenceManager.clearAllData()

                        // Import new data
                        transactions.forEach { preferenceManager.saveTransaction(it) }
                        preferenceManager.setMonthlyBudget(budget)
                        preferenceManager.setCurrency(currency)
                        CurrencyFormatter.setCurrencySymbol(currency)

                        // Update UI
                        requireActivity().runOnUiThread {
                            // Update currency dropdown
                            val currencies = resources.getStringArray(R.array.currencies)
                            val currentCurrencyIndex = currencies.indexOfFirst { it.startsWith(currency) }
                            if (currentCurrencyIndex != -1) {
                                binding.etCurrency.setText(currencies[currentCurrencyIndex], false)
                            }

                            // Show success message
                            Toast.makeText(context, "Data imported successfully", Toast.LENGTH_SHORT).show()

                            // Update other fragments
                            val dashboardFragment = parentFragmentManager.findFragmentByTag("dashboard") as? DashboardFragment
                            dashboardFragment?.let {
                                it.updateMonthlySummary()
                                it.updateBudgetProgress()
                                it.setupPieChart()
                            }

                            val transactionsFragment = parentFragmentManager.findFragmentByTag("transactions") as? TransactionsFragment
                            transactionsFragment?.updateTransactionsList()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "Failed to import data: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to import data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 