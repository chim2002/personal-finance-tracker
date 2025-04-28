package com.example.personalfinancetracker.ui.transactions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.personalfinancetracker.databinding.ItemTransactionBinding
import com.example.personalfinancetracker.model.Transaction
import com.example.personalfinancetracker.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.personalfinancetracker.utils.CurrencyFormatter

class TransactionsAdapter(
    private val onTransactionClick: (Transaction) -> Unit,
    private val onTransactionLongClick: (Transaction) -> Unit
) : ListAdapter<Transaction, TransactionsAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TransactionViewHolder(
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: Transaction) {
            binding.apply {
                tvTransactionTitle.text = transaction.title
                tvTransactionCategory.text = transaction.category
                tvTransactionDate.text = dateFormat.format(transaction.date)
                tvTransactionAmount.text = CurrencyFormatter.format(transaction.amount)
                tvTransactionAmount.setTextColor(
                    root.context.getColor(
                        if (transaction.type == TransactionType.INCOME) {
                            android.R.color.holo_green_dark
                        } else {
                            android.R.color.holo_red_dark
                        }
                    )
                )

                root.setOnClickListener { onTransactionClick(transaction) }
                root.setOnLongClickListener {
                    onTransactionLongClick(transaction)
                    true
                }
            }
        }
    }

    private class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
} 