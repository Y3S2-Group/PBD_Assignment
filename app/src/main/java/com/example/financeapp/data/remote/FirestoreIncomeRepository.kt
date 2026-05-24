package com.example.financeapp.data.remote

import com.example.financeapp.domain.model.Income
import com.example.financeapp.domain.model.RecurringIncome
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirestoreIncomeRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    private fun incomesRef(uid: String) =
        firestore.collection("users").document(uid).collection("incomes")

    private fun recurringRef(uid: String) =
        firestore.collection("users").document(uid).collection("recurring_incomes")

    suspend fun saveIncome(uid: String, income: Income): Unit =
        suspendCancellableCoroutine { cont ->
            incomesRef(uid).document(income.id)
                .set(income.toFirestoreMap())
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    suspend fun deleteIncome(uid: String, id: String): Unit =
        suspendCancellableCoroutine { cont ->
            incomesRef(uid).document(id)
                .delete()
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    suspend fun getAllIncomes(uid: String): List<Income> =
        suspendCancellableCoroutine { cont ->
            incomesRef(uid).get()
                .addOnSuccessListener { snapshot ->
                    cont.resume(snapshot.documents.mapNotNull { it.toIncome() })
                }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    suspend fun saveRecurringIncome(uid: String, recurring: RecurringIncome): Unit =
        suspendCancellableCoroutine { cont ->
            recurringRef(uid).document(recurring.id)
                .set(recurring.toFirestoreMap())
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    suspend fun deactivateRecurringIncome(uid: String, id: String): Unit =
        suspendCancellableCoroutine { cont ->
            recurringRef(uid).document(id)
                .update("isActive", false)
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    suspend fun deleteRecurringIncome(uid: String, id: String): Unit =
        suspendCancellableCoroutine { cont ->
            recurringRef(uid).document(id)
                .delete()
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    suspend fun getAllRecurringIncomes(uid: String): List<RecurringIncome> =
        suspendCancellableCoroutine { cont ->
            recurringRef(uid).get()
                .addOnSuccessListener { snapshot ->
                    cont.resume(snapshot.documents.mapNotNull { it.toRecurringIncome() })
                }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
}

private fun Income.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "amount" to amount,
    "currency" to currency,
    "amountLKR" to amountLKR,
    "sourceType" to sourceType,
    "sourceLabel" to sourceLabel,
    "date" to date,
    "notes" to notes,
    "projectRef" to projectRef,
    "exchangeRate" to exchangeRate,
    "isRecurring" to isRecurring,
    "invoicePaid" to invoicePaid,
)

private fun DocumentSnapshot.toIncome(): Income? {
    val sourceType = getString("sourceType") ?: return null
    return try {
        Income(
            id = id,
            amount = getDouble("amount") ?: 0.0,
            currency = getString("currency") ?: "LKR",
            amountLKR = getDouble("amountLKR") ?: 0.0,
            sourceType = sourceType,
            sourceLabel = getString("sourceLabel"),
            date = getLong("date") ?: 0L,
            notes = getString("notes"),
            projectRef = getString("projectRef"),
            exchangeRate = getDouble("exchangeRate") ?: 1.0,
            isRecurring = getBoolean("isRecurring") ?: false,
            invoicePaid = getBoolean("invoicePaid") ?: true,
        )
    } catch (e: Exception) {
        null
    }
}

private fun RecurringIncome.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "sourceType" to sourceType,
    "sourceLabel" to sourceLabel,
    "currency" to currency,
    "defaultAmount" to defaultAmount,
    "dayOfMonth" to dayOfMonth,
    "isActive" to isActive,
    "createdAt" to createdAt,
)

private fun DocumentSnapshot.toRecurringIncome(): RecurringIncome? {
    val sourceType = getString("sourceType") ?: return null
    return try {
        RecurringIncome(
            id = id,
            sourceType = sourceType,
            sourceLabel = getString("sourceLabel"),
            currency = getString("currency") ?: "LKR",
            defaultAmount = getDouble("defaultAmount") ?: 0.0,
            dayOfMonth = getLong("dayOfMonth")?.toInt() ?: 1,
            isActive = getBoolean("isActive") ?: true,
            createdAt = getLong("createdAt") ?: 0L,
        )
    } catch (e: Exception) {
        null
    }
}
