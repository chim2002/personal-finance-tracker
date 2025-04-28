package com.example.personalfinancetracker.ui.transactions

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.personalfinancetracker.R
import com.example.personalfinancetracker.databinding.FragmentTransactionsBinding
import com.example.personalfinancetracker.model.Transaction
import com.example.personalfinancetracker.model.TransactionType
import com.example.personalfinancetracker.utils.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class TransactionsFragment : Fragment() {

    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var adapter: TransactionsAdapter
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferenceManager = PreferenceManager(requireContext())
        setupRecyclerView()
        setupFab()
    }

    private fun setupRecyclerView() {
        adapter = TransactionsAdapter(
            onTransactionClick = { showTransactionDialog(it) },
            onTransactionLongClick = { showDeleteDialog(it) }
        )
        binding.recyclerViewTransactions.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@TransactionsFragment.adapter
        }
        updateTransactionsList()
    }

    private fun setupFab() {
        binding.fabAddTransaction.setOnClickListener {
            showTransactionDialog()
        }
    }

    fun updateTransactionsList() {
        val transactions = preferenceManager.getTransactions()
            .sortedByDescending { it.date }
        adapter.submitList(transactions)
    }

    private fun showTransactionDialog(transaction: Transaction? = null) {
        val dialogBinding = layoutInflater.inflate(R.layout.dialog_transaction, null)
        val categories = resources.getStringArray(R.array.categories)

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        dialogBinding.findViewById<AutoCompleteTextView>(R.id.autoCompleteCategory).setAdapter(adapter)

        if (transaction != null) {
            dialogBinding.findViewById<TextInputEditText>(R.id.etTitle).setText(transaction.title)
            dialogBinding.findViewById<TextInputEditText>(R.id.etAmount).setText(transaction.amount.toString())
            dialogBinding.findViewById<AutoCompleteTextView>(R.id.autoCompleteCategory).setText(transaction.category)
            dialogBinding.findViewById<TextInputEditText>(R.id.etDate).setText(dateFormat.format(transaction.date))
            dialogBinding.findViewById<TextInputEditText>(R.id.etNote).setText(transaction.note)
            dialogBinding.findViewById<RadioGroup>(R.id.radioGroupType).check(
                if (transaction.type == TransactionType.INCOME) R.id.radioIncome else R.id.radioExpense
            )
        }

        dialogBinding.findViewById<TextInputEditText>(R.id.etDate).setOnClickListener {
            showDatePicker { date ->
                dialogBinding.findViewById<TextInputEditText>(R.id.etDate).setText(dateFormat.format(date))
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (transaction == null) "Add Transaction" else "Edit Transaction")
            .setView(dialogBinding)
            .setPositiveButton("Save") { _, _ ->
                val title = dialogBinding.findViewById<TextInputEditText>(R.id.etTitle).text.toString()
                val amount = dialogBinding.findViewById<TextInputEditText>(R.id.etAmount).text.toString().toDoubleOrNull()
                val category = dialogBinding.findViewById<AutoCompleteTextView>(R.id.autoCompleteCategory).text.toString()
                val date = dateFormat.parse(dialogBinding.findViewById<TextInputEditText>(R.id.etDate).text.toString())
                val note = dialogBinding.findViewById<TextInputEditText>(R.id.etNote).text.toString()
                val type = if (dialogBinding.findViewById<RadioButton>(R.id.radioIncome).isChecked) {
                    TransactionType.INCOME
                } else {
                    TransactionType.EXPENSE
                }

                if (title.isBlank() || amount == null || category.isBlank() || date == null) {
                    Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val newTransaction = Transaction(
                    id = transaction?.id ?: UUID.randomUUID().toString(),
                    title = title,
                    amount = amount,
                    category = category,
                    type = type,
                    date = date,
                    note = note
                )

                if (transaction == null) {
                    preferenceManager.saveTransaction(newTransaction)
                } else {
                    preferenceManager.updateTransaction(newTransaction)
                }

                updateTransactionsList()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteDialog(transaction: Transaction) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Delete") { _, _ ->
                preferenceManager.deleteTransaction(transaction.id)
                updateTransactionsList()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDatePicker(onDateSelected: (Date) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(year, month, day)
                onDateSelected(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 