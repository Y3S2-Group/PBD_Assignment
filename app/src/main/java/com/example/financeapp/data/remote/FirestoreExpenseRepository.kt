package com.example.financeapp.data.remote

import com.example.financeapp.domain.model.Expense
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirestoreExpenseRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    private fun expensesRef(uid: String) =
        firestore.collection("users").document(uid).collection("expenses")

    suspend fun saveExpense(uid: String, expense: Expense): Unit =
        suspendCancellableCoroutine { cont ->
            expensesRef(uid).document(expense.id)
                .set(expense.toFirestoreMap())
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    suspend fun getAllExpenses(uid: String): List<Expense> =
        suspendCancellableCoroutine { cont ->
            expensesRef(uid).get()
                .addOnSuccessListener { snapshot ->
                    cont.resume(snapshot.documents.mapNotNull { it.toExpense() })
                }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
}

private fun Expense.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "amountLkr" to amountLkr,
    "category" to category,
    "spendingType" to spendingType,
    "paymentMethod" to paymentMethod,
    "timestamp" to timestamp,
)

private fun DocumentSnapshot.toExpense(): Expense? {
    val category = getString("category") ?: return null
    val spendingType = getString("spendingType") ?: return null
    return try {
        Expense(
            id = id,
            amountLkr = getDouble("amountLkr") ?: 0.0,
            category = category,
            spendingType = spendingType,
            paymentMethod = getString("paymentMethod") ?: "",
            timestamp = getLong("timestamp") ?: 0L,
        )
    } catch (e: Exception) {
        null
    }
}
