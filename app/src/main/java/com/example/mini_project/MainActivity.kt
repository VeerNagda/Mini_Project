package com.example.mini_project

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var transactions: List<Transaction>
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var linerLayoutManager: LinearLayoutManager
    private lateinit var db: AppDatabase
    private lateinit var deleteTransaction: Transaction
    private lateinit var oldTransaction: List<Transaction>
    private lateinit var balance: TextView
    private lateinit var budget: TextView
    private lateinit var expense: TextView
    private lateinit var addBtn: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        balance = findViewById(R.id.balance)
        budget = findViewById(R.id.budget)
        expense = findViewById(R.id.expense)
        addBtn = findViewById(R.id.addBtn)
        val recyclerView: RecyclerView = findViewById(R.id.recyclerview)

        transactions = arrayListOf()

        transactionAdapter = TransactionAdapter(transactions)
        linerLayoutManager = LinearLayoutManager(this)

        db = Room.databaseBuilder(this, AppDatabase::class.java, "transactions").build()



        recyclerView.apply {
            adapter = transactionAdapter
            layoutManager = linerLayoutManager
        }

        val itemTouchHelper = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                deleteTransaction(transactions[viewHolder.adapterPosition])
            }
        }

        val swipeHelper = ItemTouchHelper(itemTouchHelper)
        swipeHelper.attachToRecyclerView(recyclerView)

        addBtn.setOnClickListener {
            val intent = Intent(this, AddTransactionActivity::class.java)
            startActivity(intent)
        }


    }

    override fun onResume() {
        super.onResume()
        fetchAll()
    }

    private fun fetchAll() {
        GlobalScope.launch {
            transactions = db.transactionDao().getAll()

            runOnUiThread {
                updateDashboard()
                transactionAdapter.setData(transactions)
            }
        }
    }

    private fun undoDelete() {
        GlobalScope.launch {
            db.transactionDao().insertAll(deleteTransaction)
            transactions = oldTransaction

            runOnUiThread {
                transactionAdapter.setData(transactions)
                updateDashboard()
            }
        }
    }

    private fun showSnackbar() {
        val view = findViewById<View>(R.id.coordinator)
        val snackbar = Snackbar.make(view, "Transaction deleted", Snackbar.LENGTH_LONG)
        snackbar.setAction("Undo") {
            undoDelete()
        }.setActionTextColor(ContextCompat.getColor(this, R.color.red))
            .setTextColor(ContextCompat.getColor(this, R.color.white))
            .show()
    }

    private fun deleteTransaction(transaction: Transaction) {
        deleteTransaction = transaction
        oldTransaction = transactions

        GlobalScope.launch {
            db.transactionDao().delete(transaction)
            transactions = transactions.filter { it.id != transaction.id }
            runOnUiThread {
                updateDashboard()
                transactionAdapter.setData(transactions)
                showSnackbar()
            }
        }
    }


    private fun updateDashboard() {
        val totalAmount: Double = transactions.sumOf { it.amount }
        val budgetAmount: Double = transactions.filter { it.amount > 0 }.sumOf { it.amount }
        val expenseAmount: Double = totalAmount - budgetAmount

        balance.text = String.format("%.2f", totalAmount)
        budget.text = String.format("%.2f", budgetAmount)
        expense.text = String.format("%.2f", expenseAmount)
    }

}